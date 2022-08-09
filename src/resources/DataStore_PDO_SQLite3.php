<?php

// include_once 'DataStore.php'; // uncomment if you need inheritance

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net

  This is an example of how to implement DataStore in PHP + PDO + SQLite3.
  Copy-paste this code to your project and change it for your needs.

  Improvements are welcome: sdm@gmail.com

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
        $db_name = '../todolist.sqlite';
        $this->db = new PDO("sqlite:$db_name");
        // http://stackoverflow.com/questions/15058129/php-pdo-inserting-data
        // By default, PDO does not throw exceptions. To make it throw exceptions on error, call
        $this->db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $this->db->setAttribute(PDO::ATTR_PERSISTENT, true);
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
        // ------------ do nothing because of PDO::ATTR_PERSISTENT, true
//        if (is_null($this->db)) {
//            throw new Exception("Already closed");
//        }
//        /*         * * close the database connection ** */
//        $this->db = null;
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
            foreach (array_keys($ai_values) as $key) {
                $id = $this->db->lastInsertId($key);
                $ai_values[$key] = $id;
            }
            return $res;
        } finally {
            $stmt->closeCursor();
        }
    }

    public function execDML($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        try {
            $res = $stmt->execute($params);
            return $res;
        } finally {
            $stmt->closeCursor();
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
            $res = array();
            while ($val = $stmt->fetchColumn()) {
                array_push($res, $val);
            }
            return $res;
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
