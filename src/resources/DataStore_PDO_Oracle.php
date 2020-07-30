<?php

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
  Contact: sqldalmaker@gmail.com

  This is an example of how to implement DataStore in PHP + PDO + ORACLE.
  Copy-paste this code to your project and change it for your needs.

  Known issues:

  - Obtaining generated Keys after INSERT
  - UDF returning SYS_REFCURSOR
  - SYS_REFCURSOR-s as OUT params
  - Implicit SYS_REFCURSOR-s

 */

// include_once 'DataStore.php';

/**
 * The class to work with both OUT and INOUT parameters
 */
class OutParam {

    public $type;
    public $value;

    function __construct($type, $value = null) {
        $this->type = $type;
        $this->value = $value;
    }

}

// class PDODataStore implements DataStore 
class DataStore { // no inheritance is also OK

    private $db;

    function __destruct() {
        $this->db = null;
    }

    public function open() {
        if (!is_null($this->db)) {
            throw new Exception("Already open");
        }
        $opt = [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ];
        $this->db = new PDO("oci:dbname=//localhost:1521/orcl", "ORDERS", "sa", $opt);
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
        $stmt = $this->db->prepare($sql);
        try {
            // http://stackoverflow.com/questions/10699543/pdo-prepared-statement-in-php-using-associative-arrays-yields-wrong-results
            // use the optional parameter of execute instead of explicitly binding the parameters:
            $res = $stmt->execute($params);
            // http://www.php.net/manual/en/pdo.lastinsertid.php
            // Returns the ID of the last inserted row, or the last value from a sequence object,
            // depending on the underlying driver. For example, PDO_PGSQL requires you to specify the name
            // of a sequence object for the name parameter.
            // This method may not return a meaningful or consistent result across different PDO drivers,
            // because the underlying database may not even support the notion of auto-increment fields or sequences.
            foreach ($ai_values as $key) {
                $id = $this->db->lastInsertId($key);
                $ai_values[$key] = $id;
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
                // $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT);
                // => OCIStmtFetch: ORA-24374: define not done before fetch or execute and fetch
                // $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type);
                // => OCIStmtFetch: ORA-24374: define not done before fetch or execute and fetch
                // This one is OK for both OUT and INOUT:
                $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT, 256);
                array_push($out_params, $params[$i]);
            } else {
                $stmt->bindParam($i + 1, $params[$i]);
            }
        }
    }

    public function execDML($sql, array $params) {
        $sp_name = $this->get_sp_name($sql);
        if ($sp_name != null) {
            $stmt = $this->db->prepare($sql);
            try {
                $out_params = array();
                $this->bind_params($stmt, $params, $out_params);
                $res = $stmt->execute();
                // PG logic bindColumn --> fetch(PDO::FETCH_BOUND) didn't work with ORACLE:
                //$this->fetch_out_params($stmt, $out_params);
                return $res;
            } finally {
                $stmt->closeCursor();
            }
        } else {
            $stmt = $this->db->prepare($sql);
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

    public function queryDto($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        try {
            $stmt->execute($params);
            $res = $stmt->fetch(PDO::FETCH_ASSOC);
            return $res;
        } finally {
            $stmt->closeCursor();
        }
    }

    public function queryDtoList($sql, array $params, $callback) {
        $stmt = $this->db->prepare($sql);
        try {
            $res = $stmt->execute($params);
            if ($res) {
                while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                    $callback($row);
                }
            }
            return $res;
        } finally {
            $stmt->closeCursor();
        }
    }

}
