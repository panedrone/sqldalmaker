<?php

// include_once 'DataStore.php'; // uncomment if you need inheritance

class OutParam {

    public $value;

}

class InOutParam {

    public $value;

    function __construct($value) {
        $this->value = $value;
    }

}

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
  Contact: sqldalmaker@gmail.com

  This is an example of how to implement DataStore in PHP + PDO + MySQL.
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
        $this->db = new PDO('mysql:host=localhost;dbname=sakila', 'root', 'root');
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
        foreach (array_keys($ai_values) as $key) {
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

    private function fetch_out_params($call_params) {
        foreach ($call_params as $key => $value) {
            if (strpos($key, '@') === 0) {
                $row = $this->db->query("select $key")->fetch(PDO::FETCH_ASSOC);
                if ($row) {
                    $value->value = $row !== false ? $row[$key] : null;
                }
            }
        }
    }

    private function get_call_info($sp_name, $params, &$in_params, &$call_params) {
        for ($i = 0; $i < count($params); $i++) {
            if ($params[$i] instanceof OutParam) {
                $name = "@out_param_$i";
                $call_params[$name] = $params[$i];
            } else if ($params[$i] instanceof InOutParam) {
                $name = "@inout_param_$i";
                $call_params[$name] = $params[$i];
                $in_params[$name] = $params[$i]->value;
            } else {
                $name = ":in_param_$i";
                $call_params[$name] = null;
                $in_params[$name] = $params[$i];
            }
        }
        $params_str = join(', ', array_keys($call_params));
        $sql = "call $sp_name($params_str)";
        return $sql;
    }

    private function init_call_params($stmt, $in_params) {
        foreach ($in_params as $key => $value) {
            if (strpos($key, '@') === 0) {
                $this->db->query("set $key = $value");
            } else {
                $stmt->bindParam($key, $value); // PDO::PARAM_INT | PDO::PARAM_INPUT_OUTPUT
            }
        }
    }

    public function execDML($sql, array $params) {
        $sp_name = $this->get_sp_name($sql);
        if ($sp_name != null) {
            $in_params = array();
            $call_params = array();
            $sql = $this->get_call_info($sp_name, $params, $in_params, $call_params);
            $stmt = $this->db->prepare($sql);
            $this->init_call_params($stmt, $in_params);
            $res = $stmt->execute();
            $stmt->closeCursor();
            $this->fetch_out_params($call_params);
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
        $value = $stmt->fetchColumn();
        $stmt->closeCursor();
        return $value;
    }

    public function queryList($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        $res = array();
        while ($val = $stmt->fetchColumn()) {
            array_push($res, $val);
        }
        $stmt->closeCursor();
        return $res;
    }

    public function queryDto($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        $res = $stmt->fetch(PDO::FETCH_ASSOC);
        $stmt->closeCursor();
        return $res;
    }

    public function queryDtoList($sql, array $params, $callback) {
        $stmt = $this->db->prepare($sql);
        $res = $stmt->execute($params);
        if ($res) {
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                // http://php.net/manual/en/functions.anonymous.php
                $callback($row);
            }
        }
        $stmt->closeCursor();
        return $res;
    }

}
