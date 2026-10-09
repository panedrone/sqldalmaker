/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/*
 * 30.08.2026 14:00 1.331 Claude refactor
 * 27.09.2023 17:56
 * 09.04.2023 20:31 1.282
 * 09.04.2023 14:52 1.282
 * 06.04.2023 02:56 1.281
 * 30.12.2022 23:48 1.273
 * 16.11.2022 08:02 1.269
 * 02.11.2022 06:51 1.267
 * 25.07.2022 14:53
 * 19.05.2022 09:45 1.241
 * 21.04.2022 11:34 1.225
 * 10.05.2021 21:46
 * 07.03.2021 22:16
 * 06.03.2021 19:52
 * 02.01.2020 07:21
 * 07.02.2019 19:50 initial commit
 */
public class FieldInfo {

    private String rendered_name;

    private String lower_camel_case;

    private String rendered_type;
    private final String original_type;
    private final String original_scalar_type;
    private String scalar_type;

    private String model;

    private final String column_name; // original name in database without conversions to lower/camel case etc.

    private int column_size = 0;
    private int decimal_digits = 0;

    private boolean is_auto_increment = false;
    private boolean is_pk = false;

    private boolean is_nullable = false;

    private String foreign_key = null;

    private boolean is_indexed = false;
    private boolean is_unique = false;

    private String comment;

    private String name_prefix = "";

    private String assign_func = "";

    private int precision = 0;
    private int scale = 0;

    private int display_size = 0;

    // 'comment' of a FieldInfo that stands for a method parameter and not for a column.
    // Column names are normalized, parameter names are not.
    public static final String PARAMETER = "parameter";

    // The DAO renderers of implicit cursors put a whole expression where a column name
    // is expected: an array of row handlers in Java, a slice of callbacks in Go. Such a
    // "name" has to reach the template untouched.
    // TODO: those callers should pass the expression as such, not disguised as a name.
    private static boolean _is_rendered_expression(String name) {
        return name.contains("func(") || name.contains("new RowHandler[]");
    }

    public static FieldInfo parameter(
            FieldNamesMode field_names_mode,
            String type_name,
            String param_name) throws Exception {

        return new FieldInfo(field_names_mode, type_name, param_name, PARAMETER);
    }

    public FieldInfo(
            FieldNamesMode field_names_mode,
            String original_field_type,
            String jdbc_db_col_name,
            String comment) throws Exception {

        if (original_field_type == null) {
            throw new Exception("<field type=null. Ensure that XSD and XML files of meta-program are valid.");
        }
        this.original_type = original_field_type;
        if ("-".equals(original_field_type.trim())) {
            this.original_scalar_type = original_field_type; // excluded field
        } else {
            String[] parts = original_field_type.split("-");
            if (parts.length == 2) {
                this.model = parts[0];
                this.original_scalar_type = parts[1];
            } else if (parts.length == 1) {
                this.original_scalar_type = original_field_type;
            } else {
                throw new Exception("Invalid field type: " + original_field_type);
            }
        }
        if (jdbc_db_col_name == null) {
            jdbc_db_col_name = "";
        }
        this.scalar_type = this.original_scalar_type;
        this.column_name = jdbc_db_col_name;
        this.rendered_type = original_field_type;
        this.rendered_name = jdbc_db_col_name;

        if (!this.rendered_name.isEmpty() && !_is_rendered_expression(this.rendered_name)) {

            this.rendered_name = this.rendered_name.replace("-", "_"); // for MySQL: `api-key`
            this.rendered_name = this.rendered_name.replace(".", "_"); // [OrderDetails].OrderID
            this.rendered_name = this.rendered_name.replace(":", "_"); // CustomerID:1 -- xerial SQLite3
            if (!PARAMETER.equals(comment)) {
                // apply only for fields
                this.rendered_name = this.rendered_name.replace(" ", "_"); // for MySQL!
                this.rendered_name = this.rendered_name.replace("[", "");  // [OrderDetails].OrderID
                this.rendered_name = this.rendered_name.replace("]", "");  // [OrderDetails].OrderID
            }

            this.lower_camel_case = Names.lower_camel_case(rendered_name);

            // don't apply to callback-params
            if (FieldNamesMode.LOWER_CAMEL_CASE.equals(field_names_mode)) {
                this.rendered_name = Names.lower_camel_case(this.rendered_name);
            } else if (FieldNamesMode.TITLE_CASE.equals(field_names_mode)) {
                this.rendered_name = Names.title_case(this.rendered_name);
            } else if (FieldNamesMode.SNAKE_CASE.equals(field_names_mode)) {
                this.rendered_name = Names.camel_case_to_lower_snake_case(this.rendered_name);
                this.name_prefix = "_";
            }
        }

        this.comment = comment;
    }

    private FieldInfo(FieldInfo src) {
        this.rendered_name = src.rendered_name;
        this.lower_camel_case = src.lower_camel_case;
        this.rendered_type = src.rendered_type;
        this.original_type = src.original_type;
        this.original_scalar_type = src.original_scalar_type;
        this.scalar_type = src.scalar_type;
        this.model = src.model;
        this.column_name = src.column_name;
        this.column_size = src.column_size;
        this.decimal_digits = src.decimal_digits;
        this.is_auto_increment = src.is_auto_increment;
        this.is_pk = src.is_pk;
        this.is_nullable = src.is_nullable;
        this.foreign_key = src.foreign_key;
        this.is_indexed = src.is_indexed;
        this.is_unique = src.is_unique;
        this.comment = src.comment;
        this.name_prefix = src.name_prefix;
        this.assign_func = src.assign_func;
        this.precision = src.precision;
        this.scale = src.scale;
        this.display_size = src.display_size;
    }

    // FieldInfo is immutable: there are getters only, and every "change" is a new
    // FieldInfo made by one of the static methods below. Whoever holds a FieldInfo
    // can be sure that nobody else refines it behind their back.

    public static FieldInfo with_rendered_type(FieldInfo src, String type) {
        FieldInfo res = new FieldInfo(src);
        String[] assign_parts = type.split("->");
        if (assign_parts.length > 1) {
            res.rendered_type = assign_parts[0].trim();
            res.assign_func = assign_parts[1].trim();
        } else {
            res.rendered_type = type.trim();
        }
        return res;
    }

    public static FieldInfo with_assign_func(FieldInfo src, String assign_func) {
        FieldInfo res = new FieldInfo(src);
        res.assign_func = assign_func;
        return res;
    }

    public static FieldInfo with_name(FieldInfo src, String name) { // Go formatting
        FieldInfo res = new FieldInfo(src);
        res.rendered_name = name;
        return res;
    }

    public static FieldInfo with_comment(FieldInfo src, String comment) {
        FieldInfo res = new FieldInfo(src);
        res.comment = comment;
        return res;
    }

    public static FieldInfo with_scalar_type(FieldInfo src, String type_name) {
        FieldInfo res = new FieldInfo(src);
        res.scalar_type = type_name;
        return res;
    }

    public static FieldInfo with_pk(FieldInfo src, boolean pk) {
        FieldInfo res = new FieldInfo(src);
        res.is_pk = pk;
        return res;
    }

    public static FieldInfo with_ai(FieldInfo src, boolean is_auto_increment) {
        FieldInfo res = new FieldInfo(src);
        res.is_auto_increment = is_auto_increment;
        return res;
    }

    public static FieldInfo with_fk(FieldInfo src, String fk) {
        FieldInfo res = new FieldInfo(src);
        res.foreign_key = fk;
        return res;
    }

    public static FieldInfo with_nullable(FieldInfo src, boolean nullable) {
        FieldInfo res = new FieldInfo(src);
        res.is_nullable = nullable;
        return res;
    }

    public static FieldInfo with_indexed(FieldInfo src, boolean indexed) {
        FieldInfo res = new FieldInfo(src);
        res.is_indexed = indexed;
        return res;
    }

    public static FieldInfo with_unique(FieldInfo src, boolean unique) {
        FieldInfo res = new FieldInfo(src);
        res.is_unique = unique;
        return res;
    }

    public static FieldInfo with_column_size(FieldInfo src, int size) {
        FieldInfo res = new FieldInfo(src);
        res.column_size = size;
        return res;
    }

    public static FieldInfo with_decimal_digits(FieldInfo src, int decimal_digits) {
        FieldInfo res = new FieldInfo(src);
        res.decimal_digits = decimal_digits;
        return res;
    }

    // JDBC ResultSetMetaData of a column: they always come together
    public static FieldInfo with_rs_md(FieldInfo src, int precision, int scale, int display_size) {
        FieldInfo res = new FieldInfo(src);
        res.precision = precision;
        res.scale = scale;
        res.display_size = display_size;
        return res;
    }

    public String getAssignFunc() { // this method is for use in "java.vm" and "go.vm"
        return this.assign_func;
    }

    public String getName() { // this method is for use in VM templates
        return this.rendered_name;
    }

    public String getLowerCamelCaseName() { // for Golang VM template
        return lower_camel_case;
    }

    public String getType() { // this method is for use in VM templates
        return rendered_type;
    }

    public String getColumnName() { // this method is for use in VM templates
        return this.column_name;
    }

    public int getColumnSize() { // this method is for use in VM templates
        if (this.is_auto_increment) {
            return 0;
        }
        return this.column_size;
    }

    public boolean isPK() {
        return this.is_pk;
    }

    public boolean isAI() {
        return this.is_auto_increment;
    }

    public String getFK() {
        return foreign_key;
    }

    public boolean isNullable() {
        return this.is_nullable;
    }

    public String getComment() { // this method is for use in VM templates
        return this.comment;
    }

    // this method is for use in VM templates ONLY
    public String getterMethod() { // NO_UCD (unused code)
        String s = this.name_prefix + this.rendered_name;
        String X = Names.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));
        return "get" + X;
    }

    // this method is for use in VM templates ONLY
    public String setterMethod() { // NO_UCD (unused code)
        String s = this.name_prefix + this.rendered_name;
        String X = Names.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));
        return "set" + X;
    }

    public String getOriginalType() {
        return original_type;
    }

    public boolean isIndexed() {
        return is_indexed;
    }

    public boolean isUnique() {
        return is_unique;
    }

    public String getOriginalScalarType() {
        return original_scalar_type;
    }

    public String getScalarType() {
        return scalar_type;
    }

    public String getModel() {
        return model;
    }

    public int getDecimalDigits() { // this method is for use in VM templates
        return this.decimal_digits;
    }

    public int getPrecision() {
        return this.precision;
    }

    public int getScale() {
        return this.scale;
    }

    public int getDisplaySize() {
        return this.display_size;
    }

}
