<?php

// include_once 'DataStore.php'; // uncomment if you need inheritance

/*
  This is an example of how to implement and use DataStore for PDO.
  Web-site: http://sqldalmaker.sourceforge.net
  Contact: sqldalmaker@gmail.com
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

        $db_name = 'thesaurus.sqlite';

        $this->db = new PDO("sqlite:dal/$db_name");

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

        foreach ($ai_values as $key => $value) {
            $id = $this->db->lastInsertId($key);
            $ai_values[$key] = $id;
        }

        return $res;
    }

    public function execDML($sql, array $params) {

        $stmt = $this->db->prepare($sql);

        return $stmt->execute($params);
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
            // http://php.net/manual/en/functions.anonymous.php
            $callback($row);
        }

        return TRUE;
    }

}
