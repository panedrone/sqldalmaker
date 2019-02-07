#include "DataStore.h"

#include <locale>
#include <codecvt>

#include "sqlite3.h"

#define mpDB ((sqlite3*)db)

bool CDataStore::open() {

    errors.empty();

    // TODO: replace with required value:

    sqlite3 *_db;
    const char *szFile = "/Users/panedrone/Desktop/console_test/northwindEF.db";
    int nRet = sqlite3_open(szFile, &_db);

    db = _db;

    bool res = nRet == SQLITE_OK;

    if (!res) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        return res;
    }

    sqlite3_busy_timeout(mpDB, 60000); // 60 seconds

    return res;
}

bool CDataStore::close() {

    errors.empty();

    int nRet = sqlite3_close(mpDB);

    bool res = nRet == SQLITE_OK;

    if (!res) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
    }

    return res;
}

static int bindParameters(sqlite3_stmt *pStmt, int params_count,
        CValue *params[]) {

    int nRes;

    for (int i = 0; i < params_count; i++) {
        // http://www.sqlite.org/c3ref/bind_blob.html
        // The leftmost SQL parameter has an index of 1

        int paramIndex = i + 1;

        if (params[i]->IsNull) {
            sqlite3_bind_null(pStmt, i);
        } else {
            ValueType vt = params[i]->getType();

            switch (vt) {
                case VALUE_TYPE_LONG:
                    nRes = sqlite3_bind_int(pStmt, paramIndex,
                            ((CLong *) params[i])->Value);
                    if (nRes != SQLITE_OK) {
                        return nRes;
                    }
                    break;
                case VALUE_TYPE_DOUBLE:
                    nRes = sqlite3_bind_double(pStmt, paramIndex,
                            ((CDouble *) params[i])->Value);
                    if (nRes != SQLITE_OK) {
                        return nRes;
                    }
                    break;
                case VALUE_TYPE_TEXT: {
                    const char *szValue = ((CText *) params[i])->Value.c_str();
                    nRes = sqlite3_bind_text(pStmt, paramIndex, szValue, -1,
                            SQLITE_TRANSIENT);

                    if (nRes != SQLITE_OK) {
                        return nRes;
                    }
                    break;
                }
                case VALUE_TYPE_BLOB: {
                    CBlob *blob = (CBlob *) params[i];
                    nRes = sqlite3_bind_blob(pStmt, paramIndex,
                            blob->Value.data(), (int) blob->Value.size(),
                            NULL);
                    if (nRes != SQLITE_OK) {
                        return nRes;
                    }
                    break;
                }
                    // TODO: other types
            }
        }
    }

    return SQLITE_OK;
}

std::string toUtf8(const wchar_t *str) {

    // http://en.cppreference.com/w/cpp/locale/string_convert/from_bytes
    // converts between UTF-8 and UCS-4 (given sizeof(wchar_t)==4)
    std::wstring_convert<std::codecvt_utf8<wchar_t>, wchar_t> convert;
    return convert.to_bytes(str);
}

static int prepare(sqlite3 *pDb, DL_STR szSQL, sqlite3_stmt **ppStmt) {

    std::string sql = toUtf8(szSQL);

    return sqlite3_prepare_v2(pDb, sql.c_str(), -1, ppStmt, NULL);
}

int CDataStore::insert(DL_STR sql, int params_count, CValue *params[],
        int AI_values_count, CValue *AI_values[]) {

    errors.empty();

    sqlite3_stmt *pStmt;

    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        return -1;
    }

    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        sqlite3_finalize(pStmt);
        return -1;
    }

    nRet = sqlite3_step(pStmt);
    if (nRet != SQLITE_DONE) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        sqlite3_finalize(pStmt);
        return -1;
    }

    int records_inserted = sqlite3_changes(mpDB);

    sqlite3_int64 primaryKey = sqlite3_last_insert_rowid(mpDB);
    ((CLong *) AI_values[0])->Value = (int) primaryKey;

    //Reset the add statement.
    // sqlite3_reset(pStmt);

    sqlite3_finalize(pStmt);

    return records_inserted;
}

int CDataStore::execDML(DL_STR sql, int params_count,
        CValue *params[]) {

    errors.empty();

    sqlite3_stmt *pStmt;

    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        return false;
    }

    int records_updated = -1;

    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        goto finalize;
    }

    nRet = sqlite3_step(pStmt);
    if (nRet != SQLITE_DONE) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        goto finalize;
    }

    records_updated = sqlite3_changes(mpDB);

    //Reset the statement.
    // sqlite3_reset(pStmt);

    finalize:

    sqlite3_finalize(pStmt);

    return records_updated;
}

static void getFieldNames(sqlite3_stmt *pStmt, std::vector<std::string> &res) {

    int mnCols = sqlite3_column_count(pStmt);

    for (int i = 0; i < mnCols; i++) {

        const char *szTemp = sqlite3_column_name(pStmt, i);

        res.push_back(szTemp);
    }
}

static int getFieldIndex2(std::vector<std::string> &field_names,
        DL_STR name) {

    std::string n = toUtf8(name);

    for (size_t i = 0; i < field_names.size(); i++) {

        std::string &fn = field_names[i];

        if (fn.compare(n) == 0) {
            return (int)i;
        }
    }

    return -1;
}

static bool getFieldIndexes(std::vector<std::string> &field_names,
        int col_names_size, DL_STR col_names[],
        std::vector<size_t> &res) {

    for (size_t i = 0; i < col_names_size; i++) {

        int index = getFieldIndex2(field_names, col_names[i]);

        if (index == -1) {
            return false;
        }

        res.push_back((size_t)index);
    }

    return col_names_size > 0;
}

static void readRowData(sqlite3_stmt *pStmt, CValue *ret_values[], std::vector<size_t> &field_indexes) {

    for (size_t col = 0; col < field_indexes.size(); col++) {

        int i = (int) field_indexes[col];

        // http://www.sqlite.org/c3ref/column_name.html
        // The leftmost column is number 0.

        int r = sqlite3_column_type(pStmt, i);
        if (r == SQLITE_NULL) {
            ret_values[i]->IsNull = true;
        } else {
            ret_values[i]->IsNull = false;

            ValueType vt = ret_values[i]->getType();

            switch (vt) {
                case VALUE_TYPE_LONG:
                    ((CLong *) ret_values[i])->Value = sqlite3_column_int(pStmt, i);
                    break;
                case VALUE_TYPE_DOUBLE:
                    ((CDouble *) ret_values[i])->Value = sqlite3_column_double(
                            pStmt, i);
                    break;
                case VALUE_TYPE_TEXT: {
                    const unsigned char *res = sqlite3_column_text(pStmt, i);
                    std::string s ((const char *) res);
                    ((CText *) ret_values[i])->Value.assign(s);
                    break;
                }
                case VALUE_TYPE_BLOB: {
                    int len = sqlite3_column_bytes(pStmt, 0);
                    unsigned char *buf = (unsigned char *) sqlite3_column_blob(pStmt, i);
                    CBlob *blob = (CBlob *) ret_values[i];
                    blob->Value.resize((size_t)len);
                    // Set the pointer to the first element
                    unsigned char *pData = blob->Value.data();
                    // Set array values directly
                    for (size_t j = 0; j < len; j++, pData++) {
                        *pData = buf[j];
                    }
                    break;
                }
                    // TODO: other types
            }
        }
    }
}

int CDataStore::queryRow(DL_STR sql, int params_count,
        CValue *params[], int ret_values_count, CValue *ret_values[],
        DL_STR col_names[]) {

    errors.empty();

    sqlite3_stmt *pStmt;

    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        return -1;
    }

    int num_rows;

    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        num_rows = -1;
    } else {
        num_rows = 0;

        nRet = sqlite3_step(pStmt);

        if (nRet == SQLITE_DONE) {
            // no rows
            // DO NOTHING
        } else if (nRet == SQLITE_ROW) {

            std::vector<std::string> field_names;
            getFieldNames(pStmt, field_names);

            std::vector<size_t> field_indexes;
            getFieldIndexes(field_names, ret_values_count, col_names, field_indexes);

            readRowData(pStmt, ret_values, field_indexes);
            num_rows = 1;
        }
    }

    sqlite3_finalize(pStmt);

    return num_rows;
}

int CDataStore::queryRowList(DL_STR sql, int params_count,
        CValue *params[], int ret_values_count, CValue *ret_values[],
        DL_STR col_names[], CRowProcessor *rowProcessor) {

    errors.empty();

    sqlite3_stmt *pStmt;

    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        return -1;
    }

    int num_rows;

    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        std::string s(sqlite3_errmsg(mpDB));
        errors.append(s);
        num_rows = -1;
    } else {

        std::vector<std::string> field_names;
        getFieldNames(pStmt, field_names);

        std::vector<size_t> field_indexes;
        getFieldIndexes(field_names, ret_values_count, col_names, field_indexes);

        num_rows = 0;

        while (1) {

            nRet = sqlite3_step(pStmt);

            // use if-s instead of switch to break the loop
            if (nRet == SQLITE_DONE) {
                // no rows
                break;// goto finalize;

            } else if (nRet == SQLITE_ROW) {
                // fetch the row data

                readRowData(pStmt, ret_values,
                        field_indexes);

                num_rows++;

                rowProcessor->processRow();

            } else {
                //case SQLITE_BUSY:
                // default:
                num_rows = -1; // ERROR
                break; // goto finalize;
            }
        }
    }

    sqlite3_finalize(pStmt);

    return num_rows;
}
