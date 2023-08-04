<?php

/*
  	This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
  	It demonstrates how to implement an interface DataStore in PHP + OCI8 (Oracle 12c+).
  	More about DataStore: https://sqldalmaker.sourceforge.net/preconfiguring.html#ds
  	Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store.go

  	Copy-paste this code to your project and change it for your needs.
  	Improvements are welcome: sqldalmaker@gmail.com

 */

// include_once 'DataStore.php';

/**
 * The class to access both OUT and INOUT parameters
 */
class OutParam {

    public $type;
    public $value;
    public $size;

    function __construct($type = SQLT_CHR, $value = null, $size = -1) {
        $this->type = $type;
        $this->value = $value;
        $this->size = $size;
    }

}

// class OciDataStore implements DataStore
class DataStore { // no inheritance is also OK

    private $conn = null;
    private $commit_mode;
    private $NO_RC_IDS = array(null);

    function __destruct() {
        self::close();
    }

    public function open() {
        if (!is_null($this->conn)) {
            throw new Exception("Already open");
        }
        $this->conn = oci_connect('ORDERS', 'sa', 'localhost:1521/orcl');
        if (!$this->conn) { // FALSE on error
            self::_trigger_oci_error();
        }
        $this->commit_mode = OCI_NO_AUTO_COMMIT;
    }

    // https://www.php.net/manual/en/function.oci-commit.php
    // A transaction begins when the first SQL statement that changes data is executed with oci_execute() using the OCI_NO_AUTO_COMMIT flag
    public function beginTransaction() {
        $this->commit_mode = OCI_NO_AUTO_COMMIT;
    }

    public function commit() {
        if (!is_null($this->conn)) {
            oci_commit($this->conn);
        }
    }

    public function rollback() {
        if (!is_null($this->conn)) {
            oci_rollback($this->conn);
        }
    }

    public function close() {
        if (!is_null($this->conn)) {
            oci_close($this->conn);
            $this->conn = null;
        }
    }

    public function insert($_sql, array $params, array &$ai_values) {
        $sql = self::_format_sql($_sql);
        $gen_key = null;
        if (count($ai_values) > 0) {
            if (count($ai_values) > 1) {
                throw new Exception("Multiple generated keys are not supported");
            }
            $gen_key = array_keys($ai_values)[0];
            $sql = $sql . ' returning ' . $gen_key . ' into :' . $gen_key;
        }
        $stid = oci_parse($this->conn, $sql);
        try {
            $bind_names = SqlBindNames::getSqlBindNames($sql);
            self::bind_params($stid, $this->NO_RC_IDS, $bind_names, $params);
            $gen_value = 0;
            if (count($ai_values) > 0) {
                oci_bind_by_name($stid, ':' . $gen_key, $gen_value, -1, SQLT_INT);
            }
            $r = oci_execute($stid, $this->commit_mode);
            if (!$r) {
                self::_trigger_oci_error();
            }
            if (count($ai_values) > 0) {
                $ai_values[$gen_key] = $gen_value;
            }
            return $r;
        } finally {
            oci_free_statement($stid);
        }
    }

    public function execDML($_sql, array $params) {
        $sql = self::_format_sql($_sql);
        $stid = oci_parse($this->conn, $sql);
        try {
            $bind_names = SqlBindNames::getSqlBindNames($sql);
            $rc_ids = array();
            $out_cursors = self::bind_params($stid, $rc_ids, $bind_names, $params);
            $r = oci_execute($stid, $this->commit_mode);
            if (!$r) {
                self::_trigger_oci_error();
            }
            if ($out_cursors) {
                $cb_index = 0;
                for ($i = 0; $i < count($params); $i++) {
                    $p = $params[$i];
                    if (is_callable($p)) {
                        $rcid = $rc_ids[$cb_index];
                        try {
                            $cb_index++;
                            // https://www.php.net/manual/en/function.oci-new-cursor.php
                            oci_execute($rcid, OCI_DEFAULT);  // Execute the REF CURSOR like a normal statement id
                            while (($row = oci_fetch_array($rcid, OCI_ASSOC + OCI_RETURN_NULLS))) {
                                $p($row);
                            }
                        } finally {
                            oci_free_statement($rcid);
                        }
                    }
                }
            } else {
                for ($i = 0; $i < count($params); $i++) {
                    $p = $params[$i];
                    if (is_array($p)) {
                        // (exec-dml) + (SP call) + (list-param containing callback(s)) means 'implicit cursor'
                        $cb = $p[0];
                        if (is_callable($cb)) {
                            // https://blogs.oracle.com/opal/using-php-and-oracle-database-12c-implicit-result-sets
                            $cb_index = 0;
                            while (($imprcid = oci_get_implicit_resultset($stid))) {
                                try {
                                    while ($row = oci_fetch_array($imprcid, OCI_ASSOC + OCI_RETURN_NULLS)) {
                                        $cb = $p[$cb_index];
                                        $cb($row);
                                    }
                                } finally {
                                    oci_free_statement($imprcid); // missing in blog
                                }
                                $cb_index++;
                            }
                        }
                    }
                }
            }
            return $r;
        } finally {
            oci_free_statement($stid);
        }
    }

    public function query($sql, array $params) {
        $res_arr = queryList($sql, $params);
        if (count($res_arr) == 0) {
            throw new Exception('No rows');
        }
        if (count($res_arr) > 1) {
            throw new Exception('More than 1 row exists');
        }
        return $res_arr[0];
    }

    public function queryList($_sql, array $params) {
        $sql = self::_format_sql($_sql);
        $stid = oci_parse($this->conn, $sql);
        try {
            $bind_names = SqlBindNames::getSqlBindNames($sql);
            self::bind_params($stid, $this->NO_RC_IDS, $bind_names, $params);
            $r = oci_execute($stid, OCI_DEFAULT); // just reading, nothing to commit
            if (!$r) {
                self::_trigger_oci_error();
            }
            $res_arr = array();
            while ($row = oci_fetch_array($stid, OCI_NUM + OCI_RETURN_NULLS)) {
                array_push($res_arr, $row[0]);
            }
            return $res_arr;
        } finally {
            oci_free_statement($stid);
        }
    }

    public function queryRow($sql, array $params) {
        $res_arr = array();
        $callback = function($row) use(&$res_arr) {
            array_push($res_arr, $row);
        };
        queryRowList($sql, $params, $callback);
        if (count($res_arr) == 0) {
            throw new Exception('No rows');
        }
        if (count($res_arr) > 1) {
            throw new Exception('More than 1 row exists');
        }
        return $res_arr[0];
    }

    public function queryRowList($_sql, array $params, $callback) {
        $sql = self::_format_sql($_sql);
        $stid = oci_parse($this->conn, $sql);
        try {
            $bind_names = SqlBindNames::getSqlBindNames($sql);
            $sp_name = self::_get_sp_name($sql);
            if (is_null($sp_name)) {
                self::bind_params($stid, $this->NO_RC_IDS, $bind_names, $params);
                $r = oci_execute($stid, OCI_DEFAULT); // just reading, nothing to commit
                if (!$r) {
                    self::_trigger_oci_error();
                }
                while ($row = oci_fetch_array($stid, OCI_ASSOC + OCI_RETURN_NULLS)) {
                    $callback($row);
                }
                return $r;
            } else {
                throw new Exception("SP are not allowed in 'query...', use 'exec-dml' instead");
            }
        } finally {
            oci_free_statement($stid);
        }
    }

    private function _trigger_oci_error() {
        $e = oci_error();
        trigger_error(htmlentities($e['message'], ENT_QUOTES), E_USER_ERROR);
    }

    private function bind_params($stid, &$rc_ids, &$bind_names, &$params) {
        $ref_cursors = false;
        for ($i = 0; $i < count($params); $i++) {
            $p = $params[$i];
            if ($p instanceof OutParam) {
                $type = $params[$i]->type;
                if ($type == SQLT_RSET || $type == OCI_B_CURSOR) {
                    throw new Exception("SQLT_RSET and OCI_B_CURSOR are not allowed, use RefCursor instead.");
                } else {
                    $size = $params[$i]->size;
                    $param_name = ':' . $bind_names[$i];
                    oci_bind_by_name($stid, $param_name, $params[$i]->value, $size, $type);
                }
            } else if (is_callable($p)) {
                if (count($rc_ids) > 0) {
                    throw new Exception("RefCursor is not allowed in this method.");
                }
                $rcid = oci_new_cursor($this->conn);
                $param_name = ':' . $bind_names[$i];
                oci_bind_by_name($stid, $param_name, $rcid, -1, OCI_B_CURSOR);
                array_push($rc_ids, $rcid);
                $ref_cursors = true;
            } else if (is_array($p)) {
                // implicit cursor. do nothing
            } else {
                oci_bind_by_name($stid, ':' . $bind_names[$i], $params[$i]);
            }
        }
        return $ref_cursors;
    }

    private function _get_sp_name($sql_src) {
        $sql = trim($sql_src);
        $sql_parts = explode('(', $sql);
        if (count($sql_parts) < 2) {
            return null;
        }
        $parts = preg_split('/\s+/', $sql_parts[0]);
        if (count($parts) < 2) {
            return null;
        }
        if (strcmp(strtolower($parts[0]), 'begin') === 0) {
            return $parts[1];
        }
        return null;
    }

    private function _format_sql($sql) {
        $i = 1;
        while (true) {
            $pos = strpos($sql, '?');
            if (!$pos) {
                break;
            }
            $str1 = substr($sql, 0, $pos);
            $str2 = substr($sql, $pos + 1);
            $sql = $str1 . ':p__' . strval($i) . $str2;
            $i += 1;
        }
        return $sql;
    }

}

//
// How to get a list of SQL query parameters for PHP OCI?
// https://stackoverflow.com/questions/59731852/how-to-get-a-list-of-sql-query-paramerters-for-php-oci
//
class SqlBindNames {

    private static function isLineBreak($ch) {
        return (($ch === "\r") || ($ch === "\n"));
    }

    private static function isIdentChar($ch) {
        return (($ch >= 'a') && ($ch <= 'z')) ||
                (($ch >= 'A') && ($ch <= 'Z')) ||
                (($ch >= '0') && ($ch <= '9')) ||
                ($ch === '_');
    }

    private const QUOTE_SINGLE_CHR = '\'';
    private const QUOTE_DOUBLE_CHR = '"';
    private const COMMENT_LINE_STR = "--";
    private const COMMENT_BEGIN_STR = "/*";
    private const COMMENT_END_STR = "*/";
    private const BIND_START_CHR = ':';
    private const MODE_NORMAL = 0;
    private const MODE_QUOTE_SINGLE = 1;
    private const MODE_QUOTE_DOUBLE = 2;
    private const MODE_COMMENT_LINE = 3;
    private const MODE_COMMENT_MULTI = 4;
    private const MODE_BIND_VARNAME = 5;

    public static function getSqlBindNames(string $sql, bool $unique = true) {
        $mode = self::MODE_NORMAL;
        $names = array();
        $namesIndex = array();
        $len = strlen($sql);
        $i = 0;

        while ($i < $len) {
            $curr = $sql[$i];
            if ($i < $len - 1) {
                $next = $sql[$i + 1];
            } else {
                $next = "\0";
            }
            $nextMode = $mode;

            if ($mode === self::MODE_NORMAL) {
                if ($curr === self::QUOTE_SINGLE_CHR) {
                    $nextMode = self::MODE_QUOTE_SINGLE;
                } else if ($curr === self::QUOTE_DOUBLE_CHR) {
                    $nextMode = self::MODE_QUOTE_DOUBLE;
                } else if (($curr === self::COMMENT_LINE_STR[0]) && ($next === self::COMMENT_LINE_STR[1])) {
                    $i += 1;
                    $nextMode = self::MODE_COMMENT_LINE;
                } else if (($curr === self::COMMENT_BEGIN_STR[0]) && ($next === self::COMMENT_BEGIN_STR[1])) {
                    $i += 1;
                    $nextMode = self::MODE_COMMENT_MULTI;
                } else if (($curr === self::BIND_START_CHR) && self::isIdentChar($next)) {
                    $bindName = "";
                    $nextMode = self::MODE_BIND_VARNAME;
                }
            } else if (($mode === self::MODE_QUOTE_SINGLE) && ($curr === self::QUOTE_SINGLE_CHR)) {
                $nextMode = self::MODE_NORMAL;
            } else if (($mode === self::MODE_QUOTE_DOUBLE) && ($curr === self::QUOTE_DOUBLE_CHR)) {
                $nextMode = self::MODE_NORMAL;
            } else if (($mode === self::MODE_COMMENT_LINE) && self::isLineBreak($curr)) {
                $nextMode = self::MODE_NORMAL;
            } else if (($mode === self::MODE_COMMENT_MULTI) && ($curr === self::COMMENT_END_STR[0]) && ($next === self::COMMENT_END_STR[1])) {
                $i += 1;
                $nextMode = self::MODE_NORMAL;
            } else if ($mode === self::MODE_BIND_VARNAME) {
                if (self::isIdentChar($curr)) {
                    $bindName = $bindName . $curr;
                }
                if (!self::isIdentChar($next)) {
                    /* found new bind param */
                    if (!$unique || !in_array(strtolower($bindName), $namesIndex)) {
                        array_push($namesIndex, strtolower($bindName));
                        array_push($names, $bindName);
                    }
                    $nextMode = self::MODE_NORMAL;
                }
            }

            $i += 1;
            $mode = $nextMode;
        }

        return $names;
    }

}
