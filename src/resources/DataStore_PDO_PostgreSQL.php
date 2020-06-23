<?php

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
  Contact: sqldalmaker@gmail.com

  This is an example of how to implement DataStore in PHP + PDO
  considering some features of PostgreSQL (e.g. Returning Unanamed REFCURSOR).

  Some features described in here
  http://www.sqlines.com/postgresql/how-to/return_result_set_from_stored_procedure
  are not supported so far.

  OUT and INOUT parameters are not supported so far too.

  Copy-paste this code to your project and change it for your needs.

 */

class DataStore {

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

    private static function startsWith($string, $startString) {
        $len = strlen($startString);
        return (substr($string, 0, $len) === $startString);
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

        return $res;
    }

    public function execDML($sql, array $params) {

        $stmt = $this->db->prepare($sql);

        return $stmt->execute($params);
    }

    /*
     * While using stored procedure that 'RETURNS REFCURSOR',
     * ensure execution of this method inside transaction.
     */

    public function query($sql, array $params) {

        $res_array = $this->queryList($sql, $params);

        if (count($res_array) == 0) {
            throw new Exception('No rows');
        }

        if (count($res_array) > 1) {
            throw new Exception('More than 1 row exists');
        }

        return $res_array[0];
    }

    /*
     * While using stored procedure that 'RETURNS REFCURSOR',
     * ensure execution of this method inside transaction.
     */

    public function queryList($sql, array $params) {

        $stmt = $this->db->prepare($sql);
        try {
            if (!$stmt->execute($params)) {
                return FALSE;
            }
            $res_array = array();
            $row = $stmt->fetch(PDO::FETCH_ASSOC); // ---- (1)
            $cursor_name = array_values($row)[0];
            if ($this->startsWith($cursor_name, '<unnamed portal ')) {
                $stmt_cursor = $this->db->query("FETCH ALL FROM \"$cursor_name\"");
                try {
                    while ($val = $stmt_cursor->fetchColumn()) {
                        array_push($res_array, $val);
                    }
                } finally {
                    $this->db->query("CLOSE \"$cursor_name\"");
                    $stmt_cursor->closeCursor();
                }
            } else {
                array_push($res_array, array_values($row)[0]); // ---- (1)
                while ($val = $stmt->fetchColumn()) {
                    array_push($res_array, $val);
                }
            }
            return $res_array;
        } finally {
            $stmt->closeCursor();
        }
    }

    /*
     * While using stored procedure that 'RETURNS REFCURSOR',
     * ensure execution of this method inside transaction.
     */

    public function queryDto($sql, array $params) {

        $res_array = array();

        $callback = function($row) use(&$res_array) {
            array_push($res_array, $row);
        };

        $this->queryDtoList($sql, $params, $callback);

        if (count($res_array) == 0) {
            throw new Exception('No rows');
        }

        if (count($res_array) > 1) {
            throw new Exception('More than 1 row exists');
        }

        return $res_array[0];
    }

    /*
     * While using stored procedure that 'RETURNS REFCURSOR',
     * ensure execution of this method inside transaction.
     */

    public function queryDtoList($sql, array $params, $callback) {

        $stmt = $this->db->prepare($sql);
        try {
            if (!$stmt->execute($params)) {
                return FALSE;
            }
            $row = $stmt->fetch(PDO::FETCH_ASSOC); // ---- (1)
            $cursor_name = array_values($row)[0];
            if ($this->startsWith($cursor_name, '<unnamed portal ')) {
                $stmt_cursor = $this->db->query("FETCH ALL FROM \"$cursor_name\"");
                try {
                    while ($row = $stmt_cursor->fetch(PDO::FETCH_ASSOC)) {
                        $callback($row);
                    }
                } finally {
                    $this->db->query("CLOSE \"$cursor_name\"");
                    $stmt_cursor->closeCursor();
                }
            } else {
                $callback($row); // ---- (1)
                while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                    $callback($row);
                }
            }
            return TRUE;
        } finally {
            $stmt->closeCursor();
        }
    }

}
