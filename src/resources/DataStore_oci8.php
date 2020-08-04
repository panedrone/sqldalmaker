<?php

/*
  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net

  This is an example of how to implement DataStore in PHP + OCI8 (ORACLE).
  Copy-paste this code to your project and change it for your needs.

  Improvements are welcome: sqldalmaker@gmail.com

 *  */

// include_once 'DataStore.php';

/**
 * The class to work with both OUT and INOUT parameters
 */
class OutParam {

    public $type;
    public $size;
    public $value;

    function __construct($type = SQLT_CHR, $value = null, $size = -1) {
        $this->type = $type;
        $this->size = $size;
        $this->value = $value;
    }

}

// class PDODataStore implements DataStore 
class DataStore { // no inheritance is also OK

    private $conn;
    private $commit_mode;

    function __destruct() {
        self::close();
    }

    public function open() {
        if (!is_null($this->conn)) {
            throw new Exception("Already open");
        }
        $this->conn = oci_connect('ORDERS', 'sa', 'localhost:1521/orcl');
        if (!$this->conn) {
            $this->trigger_oci_error();
        }
        $this->commit_mode = OCI_NO_AUTO_COMMIT;
    }

    private function trigger_oci_error() {
        $e = oci_error();
        trigger_error(htmlentities($e['message'], ENT_QUOTES), E_USER_ERROR);
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
        $sql = self::format_sql($_sql);
        if (count($ai_values) > 0) {
            if (count($ai_values) > 1) {
                throw new Exception("Multiple generated keys are not supported");
            }
            $gen_key = array_keys($ai_values)[0];
            $sql = $sql . ' returning ' . $gen_key . ' into :' . $gen_key;
            $stid = oci_parse($this->conn, $sql);
            try {
                $bind_names = SqlBindNames::getSqlBindNames($sql);
                $this->bind_params($stid, true, $bind_names, $params);
                $id = 0;
                oci_bind_by_name($stid, ':' . $gen_key, $id, -1, SQLT_INT);
                $r = oci_execute($stid, $this->commit_mode);
                if (!$r) {
                    self::trigger_oci_error();
                }
                $ai_values[$gen_key] = $id;
                return $r;
            } finally {
                oci_free_statement($stid);
            }
        } else {
            $stid = oci_parse($this->conn, $sql);
            try {
                $bind_names = SqlBindNames::getSqlBindNames($sql);
                $this->bind_params($stid, true, $bind_names, $params);
                $r = oci_execute($stid, $this->commit_mode);
                if (!$r) {
                    self::trigger_oci_error();
                }
                return $r;
            } finally {
                oci_free_statement($stid);
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
        if (strcmp(strtolower($parts[0]), 'begin') === 0) {
            return $parts[1];
        }
        return null;
    }

    private function bind_params($stid, $throw_on_ref_cursors, $bind_names, &$params) {
        $ref_cursors = false;
        for ($i = 0; $i < count($params); $i++) {
            if ($params[$i] instanceof OutParam) {
                $param_name = ':' . $bind_names[$i];
                $type = $params[$i]->type;
                if ($type == SQLT_RSET || $type == OCI_B_CURSOR) {
                    if ($throw_on_ref_cursors) {
                        throw new Exception("SYS_REFCURSOR-s are allowed only in 'queryDto' and 'queryDtoList'");
                    }
                    $rcid = oci_new_cursor($this->conn);
                    oci_bind_by_name($stid, $param_name, $rcid, -1, OCI_B_CURSOR);
                    $params[$i]->value = $rcid;
                    $ref_cursors = true;
                } else {
                    $size = $params[$i]->size;
                    oci_bind_by_name($stid, $param_name, $params[$i]->value, $size, $type);
                }
            } else {
                oci_bind_by_name($stid, ':' . $bind_names[$i], $params[$i]);
            }
        }
        return $ref_cursors;
    }

    public function execDML($_sql, array $params) {
        $sql = self::format_sql($_sql);
        $stid = oci_parse($this->conn, $sql);
        try {
            $bind_names = SqlBindNames::getSqlBindNames($sql);
            $this->bind_params($stid, true, $bind_names, $params);
            $r = oci_execute($stid, $this->commit_mode);
            if (!$r) {
                self::trigger_oci_error();
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
        $sql = self::format_sql($_sql);
        $stid = oci_parse($this->conn, $sql);
        try {
            $bind_names = SqlBindNames::getSqlBindNames($sql);
            $this->bind_params($stid, true, $bind_names, $params);
            $r = oci_execute($stid, OCI_DEFAULT); // just reading, nothing to commit
            if (!$r) {
                self::trigger_oci_error();
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

    public function queryDto($sql, array $params) {
        $res_arr = array();
        $callback = function($row) use(&$res_arr) {
            array_push($res_arr, $row);
        };
        queryDtoList($sql, $params, $callback);
        if (count($res_arr) == 0) {
            throw new Exception('No rows');
        }
        if (count($res_arr) > 1) {
            throw new Exception('More than 1 row exists');
        }
        return $res_arr[0];
    }

    public function queryDtoList($_sql, array $params, $callback) {
        $sql = self::format_sql($_sql);
        $stid = oci_parse($this->conn, $sql);
        try {
            $bind_names = SqlBindNames::getSqlBindNames($sql);
            $sp_name = self::get_sp_name($sql);
            if (is_null($sp_name)) {
                $this->bind_params($stid, true, $bind_names, $params);
                $r = oci_execute($stid, OCI_DEFAULT); // just reading, nothing to commit
                if (!$r) {
                    self::trigger_oci_error();
                }
                while ($row = oci_fetch_array($stid, OCI_ASSOC + OCI_RETURN_NULLS)) {
                    $callback($row);
                }
                return $r;
            } else {
                $ref_cursors = $this->bind_params($stid, false, $bind_names, $params);
                $r = oci_execute($stid, OCI_DEFAULT); // just reading, nothing to commit
                if (!$r) {
                    self::trigger_oci_error();
                }
                if ($ref_cursors) {
                    for ($i = 0; $i < count($params); $i++) {
                        if ($params[$i] instanceof OutParam) {
                            $type = $params[$i]->type;
                            if ($type == SQLT_RSET || $type == OCI_B_CURSOR) {
                                $rcid = $params[$i]->value;
                                // https://www.php.net/manual/en/function.oci-new-cursor.php
                                oci_execute($rcid, OCI_DEFAULT);  // Execute the REF CURSOR like a normal statement id
                                while (($row = oci_fetch_array($rcid, OCI_ASSOC + OCI_RETURN_NULLS))) {
                                    $callback($row);
                                }
                                oci_free_statement($rcid);
                            }
                        }
                    }
                } else { // implicit cursors if no out ref cursors
                    // https://blogs.oracle.com/opal/using-php-and-oracle-database-12c-implicit-result-sets
                    while (($imprcid = oci_get_implicit_resultset($stid))) {
                        while ($row = oci_fetch_array($imprcid, OCI_ASSOC + OCI_RETURN_NULLS)) {
                            $callback($row);
                        }
                        oci_free_statement($imprcid); // missing in blog
                    }
                }
            }
        } finally {
            oci_free_statement($stid);
        }
    }

    private function format_sql($sql) {
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
