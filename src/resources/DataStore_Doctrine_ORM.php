<?php

/*
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    This is an example of how to implement DataStore in PHP + Doctrine\ORM.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/DataStore_Doctrine_ORM.php
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
 */

class DataStore
{
    // https://stackoverflow.com/questions/3325012/execute-raw-sql-using-doctrine-2 // -->
    // https://www.doctrine-project.org/projects/doctrine-dbal/en/latest/reference/data-retrieval-and-manipulation.html#executequery

    private $db;

    function __construct($db)
    {
        $this->db = $db;
    }

    // https://www.doctrine-project.org/projects/doctrine-dbal/en/latest/reference/transactions.html#transactions
    public function beginTransaction()
    {
        $this->db->beginTransaction();
    }

    public function commit()
    {
        $this->db->commit();
    }

    public function rollback()
    {
        $this->db->rollback();
    }

    public function insert($sql, array $params, array &$ai_values)
    {
        // use Doctrine\ORM instead :)
    }

    /*
     * Executes a prepared statement with the given SQL and parameters and returns the affected rows count
     */
    public function execDML($sql, array $params): int
    {
        return $this->db->executeStatement($sql, $params);
    }

    /*
     * Retrieve only the value of the first column of the first result row.
     */
    public function query($sql, array $params)
    {
        return $this->db->fetchOne($sql, $params);
    }

    /*
     * Retrieve the values of the first column of all result rows.
     */
    public function queryList($sql, array $params): array
    {
        $resultSet = $this->db->executeQuery($sql, $params);
        $res = array();
        if ($resultSet) {
            $rows = $resultSet->fetchAllNumeric();
            foreach ($rows as $val) {
                array_push($res, $val[0]);
            }
        }
        return $res;
    }

    /*
     * Retrieve associative array of the first result row.
     */
    public function queryRow($sql, array $params)
    {
        return $this->db->fetchAssociative($sql, $params);
    }

    /*
     * Retrieve associative arrays of all result rows.
     */
    public function queryRowList($sql, array $params, $callback)
    {
        $resultSet = $this->db->executeQuery($sql, $params);
        if ($resultSet) {
            $rows = $resultSet->fetchAllAssociative();
            foreach ($rows as $row) {
                $callback($row);
            }
        }
    }
}