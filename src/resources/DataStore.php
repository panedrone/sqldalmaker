<?php

/*
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    About DataStore: http://sqldalmaker.sourceforge.net/how-to-start.html
    Contact: sqldalmaker@gmail.com

    Interface DataStore defines Data Access Controller used in PHP code generated by SQL DAL Maker.
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
    public function queryDto($sql, array $params);

    /**
     * @param string $sql
     * @param array $params
     * @param callable $callback
     * @return TRUE on success or FALSE on failure.
     */
    public function queryDtoList($sql, array $params, $callback);
}