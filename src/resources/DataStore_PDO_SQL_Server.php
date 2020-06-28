<?php

// include_once 'DataStore.php'; // uncomment if you need inheritance

// If you declare a parameter as OUTPUT, it acts as Both Input and OUTPUT.
// https://stackoverflow.com/questions/49129536/how-to-declare-input-output-parameters-in-sql-server-stored-procedure-function
//
class InOutParam {

    public $type;
    public $value;

    function __construct($type, $value = null) {
        $this->type = $type;
        $this->value = $value;
    }

}

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
  Contact: sqldalmaker@gmail.com

  This is an example of how to implement DataStore in PHP + PDO + SQL Server.
  Copy-paste this code to your project and change it for your needs.
 */

// class PDODataStore implements DataStore
class DataStore { // no inheritance is also OK

    private $db;

    function __destruct() {
        // close connections when the object is destroyed
        $this->db = null;
    }

    public function open() {
        if (!is_null($this->db)) {
            throw new Exception("Already open");
        }
        $serverName = "(local)\sqlexpress";
        $this->db = new PDO("sqlsrv:server=$serverName ; Database=AdventureWorks2014", "sa", "root");
        // http://stackoverflow.com/questions/15058129/php-pdo-inserting-data
        // By default, PDO does not throw exceptions. To make it throw exceptions on error, call
        $this->db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        // $this->db->setAttribute(PDO::ATTR_PERSISTENT , true);
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
        /*         * * close the database connection ** */
        $this->db = null;
    }

    public function insert($sql, array $params, array &$ai_values) {
        $stmt = $this->db->prepare($sql);
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
            if ($params[$i] instanceof InOutParam) {
                // Uncaught PDOException: SQLSTATE[IMSSP]: Invalid direction specified for parameter 1. Input/output parameters must have a length
                // $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT);
                // Uncaught PDOException: SQLSTATE[42000]: [Microsoft][ODBC Driver 11 for SQL Server][SQL Server]Incorrect syntax near 'OUTPUT'
                // $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type | PDO::PARAM_INPUT_OUTPUT, 4000);
                $stmt->bindParam($i + 1, $params[$i]->value, $params[$i]->type); // this one is OK
                array_push($out_params, $params[$i]);
            } else {
                $stmt->bindParam($i + 1, $params[$i]);
            }
        }
    }

    private function fetch_out_params($stmt, &$params) {
        $fetch_bound = false;
        for ($i = 0; $i < count($params); $i++) {
            if ($params[$i] instanceof InOutParam) {
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
        // TODO:
        // It allows also ODBC syntax like
        // DECLARE @in_out int = ?;
        // EXEC [dbo].[sp_test_inout_params] @in_out OUT;
        // SELECT @in_out AS result;
        $sp_name = $this->get_sp_name($sql);
        if ($sp_name != null) {
            $stmt = $this->db->prepare($sql);
            $out_params = array();
            $this->bind_params($stmt, $params, $out_params);
            $res = $stmt->execute();
            $this->fetch_out_params($stmt, $out_params);
            return $res;
        } else {
            $stmt = $this->db->prepare($sql);
            $res = $stmt->execute($params);
            $stmt->closeCursor();
            return $res;
        }
    }

    public function query($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchColumn();
    }

    public function queryList($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        $res = array();
        while ($val = $stmt->fetchColumn()) {
            array_push($res, $val);
        }
        return $res;
    }

    public function queryDto($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    public function queryDtoList($sql, array $params, $callback) {
        $stmt = $this->db->prepare($sql);
        $res = $stmt->execute($params);
        if (!$res) {
            return FALSE;
        }
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $callback($row);
        }
        return TRUE;
    }

}
