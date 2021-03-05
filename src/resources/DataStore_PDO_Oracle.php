<?php

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
  This is an example of how to implement DataStore in PHP + PDO + Oracle.
  Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/DataStore_PDO_Oracle.php
  Copy-paste this code to your project and change it for your needs.
  Improvements are welcome: sqldalmaker@gmail.com

  Known Issues:

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
        $this->db = null;
    }

    public function insert($sql, array $params, array &$ai_values) {
        if (count($ai_values) > 0) {
            if (count($ai_values) > 1) {
                throw new Exception("Multiple generated keys are not supported");
            }
            $gen_key = array_keys($ai_values)[0];
            $sql = $sql . ' RETURN ' . $gen_key . ' INTO ?';
            $stmt = $this->db->prepare($sql);
            try {
                $i = 1;
                for (; $i <= count($params); $i++) {
                    $stmt->bindParam($i, $params[$i - 1]);
                }
                // PDO::PARAM_INT does not work
                $id = ''; // initializing with integer instead of '' kills process
                $stmt->bindParam($i, $id, PDO::PARAM_STR | PDO::PARAM_INPUT_OUTPUT, 256);
                // ^^ specifying the length (256) means OUT parameter
                $res = $stmt->execute();
                if ($res) {
                    $ai_values[$gen_key] = $id;
                }
                return $res;
            } finally {
                $stmt->closeCursor();
            }
        } else {
            $stmt = $this->db->prepare($sql);
            try {
                $res = $stmt->execute($params);
                return $res;
            } finally {
                $stmt->closeCursor();
            }
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
                // ^^ specifying the length (256) means OUT parameter
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
