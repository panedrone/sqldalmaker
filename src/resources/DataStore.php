<?php

/*
    Interface of Data Access Controller used by PHP code generated by SQL DAL Maker.

    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Contact: sqldalmaker@gmail.com
*/
interface DataStore {

    /**
     * @param string $sql
     * @param array $params
     * @param array $ai_values
     * @return TRUE on success or FALSE on failure.
     */
    public function insert($sql, array $params, array &$ai_values);

    /**
     * @param string $sql
     * @param array $params
     * @return TRUE on success or FALSE on failure.
     */
    public function execDML($sql, array $params);

    /**
     * @param string $sql
     * @param array $params
     * @return mixed: single scalar value or FALSE on failure.
     */
    public function query($sql, array $params);

    /**
     * @param string $sql
     * @param array $params
     * @return array: 0..n scalar values.
     */
    public function queryList($sql, array $params);

    /**
     * @param string $sql
     * @param array $params
     * @return: mixed: array indexed by column name as returned in your result set (single row) or FALSE on failure.
     */
    public function queryRow($sql, array $params);

    /**
     * @param string $sql
     * @param array $params
     * @param callable $callback
     * @return TRUE on success or FALSE on failure.
     */
    public function queryRowList($sql, array $params, $callback);
}