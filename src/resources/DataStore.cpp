#include "StdAfx.h"
#include "DataStore.h"

#include "sqlite3.h"

#define mpDB ((sqlite3*)db)

bool CDataStore::open() {
    errors.Empty();
    // TODO: replace with required value:
    LPCTSTR szFile = _T("d:/northwindEF.db");
    // LPCTSTR szFile = _T("d:/meddict-en-2.db");
    sqlite3 *_db;
#if defined(_UNICODE) || defined(UNICODE)
    int nRet = sqlite3_open16(szFile, &_db);
#else
    int nRet = sqlite3_open(szFile, &_db);
#endif
    db = _db;
    bool res = nRet == SQLITE_OK;
    if (!res) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        return res;
    }
    sqlite3_busy_timeout(mpDB, 60000); // 60 seconds
    return res;
}

bool CDataStore::close() {
    errors.Empty();
    int nRet = sqlite3_close(mpDB);
    bool res = nRet == SQLITE_OK;
    if (!res) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
    }
    return res;
}

static int bindParameters(sqlite3_stmt *pStmt, int params_count, CValue* params[]) {
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
                case VALUE_TYPE_TEXT:
                {
#if defined(_UNICODE) || defined(UNICODE)
                    LPCTSTR szValue = ((CText *) params[i])->Value.GetBuffer();
                    nRes = sqlite3_bind_text16(pStmt, paramIndex, szValue, -1,
                            SQLITE_TRANSIENT);
#else
                    const char *szValue = ((CText *) params[i])->Value.GetBuffer();
                    nRes = sqlite3_bind_text(pStmt, paramIndex, szValue, -1,
                            SQLITE_TRANSIENT);
#endif
                    if (nRes != SQLITE_OK) {
                        return nRes;
                    }
                    break;
                }
                case VALUE_TYPE_BLOB:
                {
                    CBlob *blob = (CBlob *) params[i];
                    nRes = sqlite3_bind_blob(pStmt, paramIndex,
                            blob->Value.GetData(), (int) blob->Value.GetCount(),
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

static int prepare(sqlite3 *pDb, DL_STR szSQL, sqlite3_stmt **ppStmt) {
    // http://www.firstobject.com/wchar_t-string-on-linux-osx-windows.htm
    // Unlike Windows UTF-16 2-byte wide chars, wchar_t on Linux and OS X is 4 bytes UTF-32
    int nRet = sqlite3_prepare16_v2(pDb, szSQL, -1, ppStmt, NULL);
    return nRet;
}

int CDataStore::insert(DL_STR sql, int params_count, CValue* params[],
        int AI_values_count, CValue* AI_values[]) {

    errors.Empty();
    sqlite3_stmt *pStmt;
    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        return -1;
    }
    int records_inserted = -1;
    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        goto finalize;
    }
    nRet = sqlite3_step(pStmt);
    if (nRet != SQLITE_DONE) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        goto finalize;
    }
    records_inserted = sqlite3_changes(mpDB);
    sqlite3_int64 primaryKey = sqlite3_last_insert_rowid(mpDB);
    ((CLong *) AI_values[0])->Value = (int) primaryKey;
    //Reset the add statement.
    // sqlite3_reset(pStmt);
finalize:
    sqlite3_finalize(pStmt);
    return records_inserted;
}

int CDataStore::execDML(DL_STR sql, int params_count, CValue* params[]) {
    errors.Empty();
    sqlite3_stmt *pStmt;
    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        return false;
    }
    int records_updated = -1;
    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        goto finalize;
    }
    nRet = sqlite3_step(pStmt);
    if (nRet != SQLITE_DONE) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        goto finalize;
    }
    records_updated = sqlite3_changes(mpDB);
    //Reset the statement.
    // sqlite3_reset(pStmt);
finalize:
    sqlite3_finalize(pStmt);
    return records_updated;
}

static void getFieldNames(sqlite3_stmt *pStmt, CSimpleArray<CStringW> &res) {
    int mnCols = sqlite3_column_count(pStmt);
    for (int i = 0; i < mnCols; i++) {
        DL_STR szTemp = (DL_STR) sqlite3_column_name16(pStmt, i);
        res.Add(szTemp);
    }
}

static int getFieldIndex2(CSimpleArray<CStringW> &field_names, DL_STR name) {
    for (int i = 0; i < field_names.GetSize(); i++) {
        CStringW &fn = field_names[i];
        if (fn.Compare(name) == 0) {
            return i;
        }
    }
    return -1;
}

static bool getFieldIndexes(CSimpleArray<CStringW> &field_names,
        int col_names_size, DL_STR col_names[], CSimpleArray<int> &res) {

    for (int i = 0; i < col_names_size; i++) {
        int index = getFieldIndex2(field_names, col_names[i]);
        if (index == -1) {
            return false;
        }
        res.Add(index);
    }
    return col_names_size > 0;
}

static int readRowData(sqlite3_stmt *pStmt, int ret_values_count,
        CValue* ret_values[], CSimpleArray<int> &field_indexes) {

    for (int col = 0; col < field_indexes.GetSize(); col++) {
        int i = field_indexes[col];
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
                    ((CLong *) ret_values[i])->Value = sqlite3_column_int64(pStmt, i);
                    break;
                case VALUE_TYPE_DOUBLE:
                    ((CDouble *) ret_values[i])->Value = sqlite3_column_double(pStmt, i);
                    break;
                case VALUE_TYPE_TEXT:
                {
                    // winnt.h:
                    // typedef wchar_t WCHAR;    // wc,   16-bit UNICODE character
#if defined(_UNICODE) || defined(UNICODE)
                    LPCTSTR res = (LPCTSTR) sqlite3_column_text16(pStmt, i);
#else
                    const unsigned char *res = sqlite3_column_text(pStmt, i);
#endif
                    ((CText *) ret_values[i])->Value = res;
                    break;
                }
                case VALUE_TYPE_BLOB:
                {
                    int len = sqlite3_column_bytes(pStmt, 0);
                    BYTE *buf = (BYTE *) sqlite3_column_blob(pStmt, i);
                    CBlob *blob = (CBlob *) ret_values[i];
                    // http://msdn.microsoft.com/en-us/library/es8w0f60%28v=vs.80%29.aspx
                    blob->Value.SetCount(len);
                    // Set the pointer to the first element
                    BYTE *pData = blob->Value.GetData();
                    // Set array values directly
                    for (int j = 0; j < len; j++, pData++) {
                        *pData = buf[j];
                    }
                    break;
                }
                    // TODO: other types
            }
        }
    }
    return 1;
}

int CDataStore::queryRow(DL_STR sql, int params_count,
        CValue* params[], int ret_values_count, CValue* ret_values[],
        DL_STR col_names[]) {

    errors.Empty();
    sqlite3_stmt *pStmt;
    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        return -1;
    }
    int num_rows;
    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        num_rows = -1;
    } else {
        num_rows = 0;
        nRet = sqlite3_step(pStmt);
        if (nRet == SQLITE_DONE) {
            // no rows
            // DO NOTHING
        } else if (nRet == SQLITE_ROW) {
            CSimpleArray<CStringW> field_names;
            getFieldNames(pStmt, field_names);
            CSimpleArray<int> field_indexes;
            getFieldIndexes(field_names, ret_values_count, col_names, field_indexes);
            if (readRowData(pStmt, ret_values_count, ret_values, field_indexes)) {
                num_rows = 1;
            }
        }
    }
    sqlite3_finalize(pStmt);
    return num_rows;
}

int CDataStore::queryRowList(DL_STR sql, int params_count,
        CValue* params[], int ret_values_count, CValue* ret_values[],
        DL_STR col_names[], CRowProcessor *rowProcessor) {

    errors.Empty();
    sqlite3_stmt *pStmt;
    int nRet = prepare(mpDB, sql, &pStmt);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        return -1;
    }
    int num_rows;
    nRet = bindParameters(pStmt, params_count, params);
    if (nRet != SQLITE_OK) {
        CString s(sqlite3_errmsg(mpDB));
        errors.Append(s);
        num_rows = -1;
    } else {
        CSimpleArray<CStringW> field_names;
        getFieldNames(pStmt, field_names);
        CSimpleArray<int> field_indexes;
        getFieldIndexes(field_names, ret_values_count, col_names, field_indexes);
        num_rows = 0;
        while (1) {
            nRet = sqlite3_step(pStmt);
            // use if-s instead of switch to break the loop
            if (nRet == SQLITE_DONE) {
                // no rows
                break; // goto finalize;
            } else if (nRet == SQLITE_ROW) {
                if (!readRowData(pStmt, ret_values_count, ret_values, field_indexes)) {
                    num_rows = -1; // ERROR
                    break; // goto finalize;
                }
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
