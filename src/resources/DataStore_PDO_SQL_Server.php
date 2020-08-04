<?php

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net

  This is an example of how to implement DataStore in PHP + PDO + SQL Server.
  Copy-paste this code to your project and change it for your needs.

  Improvements are welcome: sqldalmaker@gmail.com
 
 */

// include_once 'DataStore.php';

/**
 * The class to work with both OUT and INOUT parameters
 */
class OutParam {

    // If you declare a parameter as OUTPUT, it acts as Both Input and OUTPUT.
    // https://stackoverflow.com/questions/49129536/how-to-declare-input-output-parameters-in-sql-server-stored-procedure-function

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
        $serverName = "(local)\sqlexpress";
        $this->db = new PDO("sqlsrv:server=$serverName ; Database=AdventureWorks2014", "sa", "root");
        $this->db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
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
        $stmt = $this->db->prepare($sql);
        $res = $stmt->execute($params);
        if (count($ai_values) > 0) {
            if (count($ai_values) > 1) {
                throw new Exception("Multiple AI PK are not allowed");
            }
            $key = array_keys($ai_values)[0];
            // lastInsertId($key) returns '(string)'
            $id = $this->db->lastInsertId(null);
            $ai_values[$key] = $id;
        }
        return $res;
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

    private function bind_call_params($stmt, &$params, &$out_params) {
        for ($i = 0; $i < count($params); $i++) {
            if ($params[$i] instanceof OutParam) {
                // Errors wile using ODBC syntax:
                // Uncaught PDOException: SQLSTATE[IMSSP]: Invalid direction specified for parameter 1. Input/output parameters must have a length
                // $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT);
                // Uncaught PDOException: SQLSTATE[42000]: [Microsoft][ODBC Driver 11 for SQL Server][SQL Server]Incorrect syntax near 'OUTPUT'
                // $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT, 256);
                // This one is OK wile using ODBC syntax:
                // $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type);
                // This one is OK wile using something like {CALL [dbo].[sp_test_inout_params](?)}:
                $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT, 256);
                // ^^ seems like allocating memery requires using $Param1 = new OutParam(PDO::PARAM_STR, "10");
                // ^^ PDO::PARAM_INPUT_OUTPUT is mandatory.
                array_push($out_params, $params[$i]);
            } else {
                $stmt->bindParam($i + 1, $params[$i]);
            }
        }
    }

// It works while using ODBC syntax:
//
//    private function fetch_out_params($stmt, &$params) {
//        $fetch_bound = false;
//        for ($i = 0; $i < count($params); $i++) {
//            if ($params[$i] instanceof OutParam) {
//                $stmt->bindColumn($i + 1, $params[$i]->value, $params[$i]->type);
//                $fetch_bound = true;
//            }
//        }
//        if ($fetch_bound) {
//            // https://stackoverflow.com/questions/13382922/calling-stored-procedure-with-out-parameter-using-pdo
//            $stmt->fetch(PDO::FETCH_BOUND);
//        }
//    }

    public function execDML($sql, array $params) {
        // TODO:
        // It allows also ODBC syntax like
        // DECLARE @res = ?;
        // EXEC [dbo].[sp_test_inout_params] @res;
        // SELECT @res AS res;
        $sp_name = $this->get_sp_name($sql);
        if ($sp_name != null) {
            $stmt = $this->db->prepare($sql);
            try {
                $out_params = array();
                $this->bind_call_params($stmt, $params, $out_params);
                $res = $stmt->execute();
                // $this->fetch_out_params($stmt, $out_params);
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
