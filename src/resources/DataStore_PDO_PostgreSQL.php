<?php

/*
  	This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
  	It demonstrates how to implement an interface DataStore in PHP + PDO/PostgreSQL.
  	More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
  	Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/DataStore_PDO_PostgreSQL.php

  	Copy-paste this code to your project and change it for your needs.
  	Improvements are welcome: sqldalmaker@gmail.com

 */

// include_once 'DataStore.php';

/**
 * The class to work with both OUT and INOUT parameters
 */
class OutParam {

    // pgAdmin4:  
    // ERROR: procedures cannot have OUT arguments
    // HINT: INOUT arguments are permitted.
    //
    public $type;
    public $value;

    function __construct($type, $value = null) {
        $this->type = $type;
        $this->value = $value;
    }

}

// class PDODataStore implements DataStore 
class DataStore { // no inheritance is also OK

    /**
     * @var PDO
     */
    private $db;

    function __destruct() {
        $this->db = null;
    }

    public function open() {
        if (!is_null($this->db)) {
            throw new Exception("Already open");
        }
        $conStr = sprintf("pgsql:host=%s;port=%d;dbname=%s;user=%s;password=%s",
                'localhost',
                5432,
                'my-tests',
                'postgres',
                'sa');
        $this->db = new PDO($conStr);
        // http://stackoverflow.com/questions/15058129/php-pdo-inserting-data
        // By default, PDO does not throw exceptions. To make it throw exceptions on error, call
        $this->db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        // $this->db->setAttribute(PDO::ATTR_PERSISTENT , true);
    }

    public function pg_fetch($cursor) {
        return "FETCH ALL FROM \"$cursor\"";
    }

    public function beginTransaction() {
        $this->db->beginTransaction();
    }

    public function commit() {
        $this->db->commit();
    }

    public function rollback() {
        $this->db->rollback();
    }

    public function close() {
        if (is_null($this->db)) {
            throw new Exception("Already closed");
        }
        $this->db = null;
    }

    public function insert($sql, array $params, array &$ai_values) {
        if (count($ai_values) > 0) {
            $sql = $sql . ' RETURNING ' . array_keys($ai_values)[0];
        }
        $stmt = $this->db->prepare($sql);
        try {
            $res = $stmt->execute($params);
            if (count($ai_values) == 0) {
                return $res;
            }
            $ai = $stmt->fetch(PDO::FETCH_ASSOC);
            foreach ($ai as $key => $value) {
                $ai_values[$key] = $value;
            }
            return $res;
        } finally {
            $stmt->closeCursor();
        }
    }

    private function get_sp_name($sql_src) {
        $sql = trim($sql_src);
        $pos = strpos($sql, '{');
        if ($pos === 0) {
            if (strpos($sql, '}') === strlen($sql) - 1) {
                $sql = substr($sql, 1, strlen($sql) - 1);
            }
        }
        $sql_parts = explode('(', $sql);
        if (count($sql_parts) < 2) {
            return null;
        }
        $parts = preg_split('/\s+/', $sql_parts[0]);
        if (count($parts) < 2) {
            return null;
        }
        if (strcmp(strtolower($parts[0]), 'call') === 0) {
            return $parts[1];
        }
        return null;
    }

    private function bind_params($stmt, &$params, &$out_params) {
        for ($i = 0; $i < count($params); $i++) {
            if ($params[$i] instanceof OutParam) {
                $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT);
                array_push($out_params, $params[$i]);
            } else {
                $stmt->bindParam($i + 1, $params[$i]);
            }
        }
    }

    private function fetch_out_params($stmt, &$params) {
        $fetch_bound = false;
        for ($i = 0; $i < count($params); $i++) {
            if ($params[$i] instanceof OutParam) {
                $stmt->bindColumn($i + 1, $params[$i]->value, $params[$i]->type);
                $fetch_bound = true;
            }
        }
        if ($fetch_bound) {
            // https://stackoverflow.com/questions/13382922/calling-stored-procedure-with-out-parameter-using-pdo
            $stmt->fetch(PDO::FETCH_BOUND);
        }
    }

    public function execDML($sql, array $params) {
        $sp_name = $this->get_sp_name($sql);
        $stmt = $this->db->prepare($sql);
        if ($sp_name != null) {
            try {
                $out_params = array();
                $this->bind_params($stmt, $params, $out_params);
                $res = $stmt->execute();
                $this->fetch_out_params($stmt, $out_params);
            } finally {
                $stmt->closeCursor();
            }
            return $res;
        } else {
            try {
                $res = $stmt->execute($params);
            } finally {
                $stmt->closeCursor();
            }
            return $res;
        }
    }

    public function query($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        try {
            $stmt->execute($params);
            $res = $stmt->fetchColumn();
            return $res;
        } finally {
            $stmt->closeCursor();
        }
    }

    public function queryList($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        try {
            $stmt->execute($params);
            $res_arr = array();
            while ($val = $stmt->fetchColumn()) {
                array_push($res_arr, $val);
            }
            return $res_arr;
        } finally {
            $stmt->closeCursor();
        }
    }

    public function queryRow($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        try {
            $stmt->execute($params);
            $res = $stmt->fetch(PDO::FETCH_ASSOC);
            return $res;
        } finally {
            $stmt->closeCursor();
        }
    }

    public function queryRowList($sql, array $params, $callback) {
        $stmt = $this->db->prepare($sql);
        try {
            $res = $stmt->execute($params);
            if (!$res) {
                return $res;
            }
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                $callback($row);
            }
            // TODO: PDOStatement::nextRowset() ?
            return $res;
        } finally {
            $stmt->closeCursor();
        }
    }

}
