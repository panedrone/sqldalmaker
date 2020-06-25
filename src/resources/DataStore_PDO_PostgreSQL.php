<?php

// include_once 'DataStore.php'; // uncomment if you need inheritance

/*
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Contact: sqldalmaker@gmail.com

    This is an example of how to implement DataStore in PHP + PDO + PostgreSQL.
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
        $conStr = sprintf("pgsql:host=%s;port=%d;dbname=%s;user=%s;password=%s",
                'localhost',
                5432,
                'test',
                'postgres',
                'sa');
        $this->db = new \PDO($conStr);
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
        /* close the database connection (?) */
        $this->db = null;
    }

    public function insert($sql, array $params, array &$ai_values) {
        if (count($ai_values) > 0) {
            $sql = $sql . ' RETURNING ' . array_keys($ai_values)[0];
        }
        $stmt = $this->db->prepare($sql);
        // http://stackoverflow.com/questions/10699543/pdo-prepared-statement-in-php-using-associative-arrays-yields-wrong-results
        // use the optional parameter of execute instead of explicitly binding the parameters:
        $res = $stmt->execute($params);
        if (count($ai_values) > 0) {
            $ai = $stmt->fetch(PDO::FETCH_ASSOC);
            foreach ($ai as $key => $value) {
                $ai_values[$key] = $value;
            }
        }
        $stmt->closeCursor();
        return $res;
    }

    public function execDML($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        $res = $stmt->execute($params);
        $stmt->closeCursor();
        return $res;
    }

    public function query($sql, array $params) {
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        $res = $stmt->fetchColumn();
        $stmt->closeCursor();
        return $res;
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
