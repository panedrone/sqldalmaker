package com.sqldalmaker;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/*
 * This class is a part of SQL DAL Maker project (http://sqldalmaker.sourceforge.net).
 * More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
 * Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/DataStore_JDBC.java_
 * Improvements are welcome: sqldalmaker@gmail.com
 */
public abstract class DataStore {

    public interface RowHandler {
        void handleRow(RowData rd) throws Exception;
    }

    public interface RecordHandler<T> {
        void handle(T t) throws Exception;
    }

    public abstract <T> T castGeneratedValue(Class<T> type, Object obj);

    public abstract int insert(String sql, String[] genColNames, Object[] genValues, Object... params) throws Exception;

    public abstract int execDML(String sql, Object... params) throws Exception;

    public abstract <T> T query(Class<T> type, String sql, Object... params) throws Exception;

    public abstract <T> List<T> queryList(Class<T> type, String sql, Object... params) throws Exception;

    public interface RowData {

        <T> T getValue(Class<T> type, String columnLabel) throws Exception;

        Short getShort(String columnLabel) throws Exception;

        Integer getInteger(String columnLabel) throws Exception;

        Long getLong(String columnLabel) throws Exception;

        Float getFloat(String columnLabel) throws Exception;

        Double getDouble(String columnLabel) throws Exception;

        BigDecimal getBigDecimal(String columnLabel) throws Exception;

        String getString(String columnLabel) throws Exception;

        Date getDate(String columnLabel) throws Exception;

        byte[] getBytes(String columnLabel) throws Exception;

        Boolean getBoolean(String columnLabel) throws Exception;
    }

    public abstract RowData queryRow(String sql, Object... params) throws Exception;

    public abstract void queryAllRows(String sql, RowHandler rowHandler, Object... params) throws Exception;
}
