#include "DataStore.h"

#include <QtSql/QSqlQuery>
#include <QtSql/QSqlError>
#include <QVariant>

bool CDataStore::open() {
    errors.clear();
    QSqlDatabase::addDatabase("QSQLITE");
    QSqlDatabase db = QSqlDatabase::database();
    db.setDatabaseName("./thesaurus.sqlite");
    if (!db.open()) {
        errors = db.lastError().text();
        return false;
    }
    return true;
}

bool CDataStore::close() {
    errors.clear();
    QSqlDatabase db = QSqlDatabase::database();
    db.close();
    return true;
}

bool CDataStore::prepare(
        const QString &sql,
        QSqlQuery &query,
        int params_count, CValue *params[]) {

    errors.clear();
    if (!query.prepare(sql)) {
        errors = query.lastError().text();
        return false;
    }
    for (int i = 0; i < params_count; i++) {
        QVariant val; // it is IsNull() == true now
        if (params[i]->IsNull) {
            // it is IsNull() == true now
        } else {
            ValueType vt = params[i]->getType();
            switch (vt) {
                case VALUE_TYPE_LONG:
                    val.setValue(static_cast<CLong*> (params[i])->Value);
                    break;
                case VALUE_TYPE_DOUBLE:
                    val.setValue(static_cast<CDouble*> (params[i])->Value);
                    break;
                case VALUE_TYPE_TEXT:
                    val.setValue(static_cast<CText*> (params[i])->Value);
                    break;
                case VALUE_TYPE_BLOB:
                    val.setValue(static_cast<CBlob*> (params[i])->Value); // TODO: not tested yet
                    break;
                default:
                    errors = "Unexpected parameter type";
                    return false;
                    // TODO: other types
            }
            query.addBindValue(val);
        }
    }
    return true;
}

bool CDataStore::readRowData(
        QSqlQuery &query,
        int ret_values_count, CValue *ret_values[],
        DL_STR col_names[]) {

    for (int i = 0; i < ret_values_count; i++) {
        QVariant value = query.value(col_names[i]);
        if (value.isNull()) {
            ret_values[i]->IsNull = true;
        } else {
            ret_values[i]->IsNull = false;
            ValueType vt = ret_values[i]->getType();
            switch (vt) {
                case VALUE_TYPE_LONG:
                    static_cast<CLong *> (ret_values[i])->Value = value.toInt();
                    break;
                case VALUE_TYPE_DOUBLE:
                    static_cast<CDouble *> (ret_values[i])->Value = value.toDouble();
                    break;
                case VALUE_TYPE_TEXT:
                    static_cast<CText *> (ret_values[i])->Value = value.toString();
                    break;
                case VALUE_TYPE_BLOB:
                    static_cast<CBlob *> (ret_values[i])->Value = value.toBitArray(); // TODO: not tested yet
                    break;
                default:
                    errors = "Unexpected value type";
                    return false;
                    // TODO: other types
            }
        }
    }
    return true;
}

int CDataStore::insert(
        DL_STR sql,
        int params_count, CValue *params[],
        int ai_values_count, CValue *ai_values[]) {

    QSqlQuery query;
    if (!prepare(sql, query, params_count, params)) {
        return -1;
    }
    query.exec();
    if (ai_values_count == 1) {
        QVariant id = query.lastInsertId(); // TODO: not tested yet
        ((CLong *) ai_values[0])->Value = id.toInt();
        ((CLong *) ai_values[0])->IsNull = false;
    }
    int rows_inserted = query.numRowsAffected(); // TODO: not tested yet
    return rows_inserted;
}

int CDataStore::execDML(DL_STR sql, int params_count, CValue *params[]) {
    QSqlQuery query;
    if (!prepare(sql, query, params_count, params)) {
        return -1;
    }
    query.exec();
    int rows_affected = query.numRowsAffected(); // TODO: not tested yet
    return rows_affected;
}

int CDataStore::queryRow(
        DL_STR sql,
        int params_count, CValue *params[],
        int ret_values_count, CValue *ret_values[],
        DL_STR col_names[]) {

    QSqlQuery query;
    if (!prepare(sql, query, params_count, params)) {
        return -1;
    }
    query.setForwardOnly(true);
    query.exec();
    int num_rows = 0;
    while (query.next()) {
        if (!readRowData(query, ret_values_count, ret_values, col_names)) {
            return -1;
        }
        num_rows++;
    }
    if (num_rows > 1) {
        errors = "The query returned more than 1 record";
    }
    return num_rows;
}

int CDataStore::queryRowList(
        DL_STR sql,
        int params_count, CValue *params[],
        int ret_values_count, CValue *ret_values[],
        DL_STR col_names[], CRowProcessor *rowProcessor) {

    QSqlQuery query;
    if (!prepare(sql, query, params_count, params)) {
        return -1;
    }
    query.setForwardOnly(true);
    query.exec();
    int num_rows = 0;
    while (query.next()) {
        if (!readRowData(query, ret_values_count, ret_values, col_names)) {
            return -1;
        }
        num_rows++;
        rowProcessor->processRow();
    }
    return num_rows;
}