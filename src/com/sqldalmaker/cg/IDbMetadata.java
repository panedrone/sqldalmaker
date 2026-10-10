/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.Collections;
import java.util.List;

/*
 * 10.10.2026 1.333 Claude
 *
 * What the database says about SQL statements and tables, as it is: no settings, no type-map,
 * no FieldInfo. Turning it into FieldInfo-s is the job of ColumnFields, TableFields,
 * ParamFields, DtoClassInfo and DaoClassInfo, and they need nothing but this interface.
 *
 * A value that the driver failed to report is null.
 */
interface IDbMetadata {

    // columns of the result set of 'jdbc_sql'
    List<Column> read_sql_columns(String jdbc_sql) throws Exception;

    // 'select * from table_ref' + what DatabaseMetaData says about its columns
    Table read_table(String table_ref) throws Exception;

    // names of PK columns as DatabaseMetaData.getPrimaryKeys reports them
    List<String> read_pk_col_names(String table_ref) throws Exception;

    // parameters of 'jdbc_sql'; types are read for the indexes 0..type_count-1
    Params read_sql_params(String jdbc_sql, int type_count) throws Exception;

    // a column of a result set: ResultSetMetaData
    final class Column {

        final String name;
        final String java_type; // Object if not detected
        final boolean is_ai;
        final int scale; // 0 if not detected
        final int precision; // 0 if not detected
        final int display_size; // 0 if not detected

        Column(String name, String java_type, boolean is_ai, int scale, int precision, int display_size) {
            this.name = name;
            this.java_type = java_type;
            this.is_ai = is_ai;
            this.scale = scale;
            this.precision = precision;
            this.display_size = display_size;
        }
    }

    // a row of DatabaseMetaData.getColumns
    final class TableColumn {

        final String name;
        final Integer data_type; // java.sql.Types
        final Integer column_size;
        final Integer decimal_digits;
        final Boolean is_ai; // IS_AUTOINCREMENT
        final Boolean is_nullable;
        final String fk; // "pk_table.pk_column" or null
        final boolean is_indexed;
        final boolean is_unique;

        TableColumn(String name, Integer data_type, Integer column_size, Integer decimal_digits,
                    Boolean is_ai, Boolean is_nullable, String fk, boolean is_indexed, boolean is_unique) {
            this.name = name;
            this.data_type = data_type;
            this.column_size = column_size;
            this.decimal_digits = decimal_digits;
            this.is_ai = is_ai;
            this.is_nullable = is_nullable;
            this.fk = fk;
            this.is_indexed = is_indexed;
            this.is_unique = is_unique;
        }
    }

    final class Table {

        final List<Column> columns; // 'select * from table'
        // in the order of getColumns; the same column may come more than once (e.g. the same
        // table name in several schemas), and then each row refines it once more
        final List<TableColumn> table_columns;

        Table(List<Column> columns, List<TableColumn> table_columns) {
            this.columns = Collections.unmodifiableList(columns);
            this.table_columns = Collections.unmodifiableList(table_columns);
        }
    }

    final class Params {

        final boolean count_unknown; // ParameterMetaData exists, but getParameterCount fails
        final int count;
        final List<String> java_types; // Object where not detected

        Params(boolean count_unknown, int count, List<String> java_types) {
            this.count_unknown = count_unknown;
            this.count = count;
            this.java_types = Collections.unmodifiableList(java_types);
        }
    }
}
