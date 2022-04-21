/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/**
 * @author sqldalmaker@gmail.com
 */
public class FieldInfo {

    private String rendered_field_name;

    private String rendered_field_type;
    private final String original_field_type;

    private final String database_column_name; // original name in database without conversions to lower/camel case etc.

    private boolean is_auto_increment = false;
    private boolean is_pk = false;
    private boolean is_nullable = false;

    private String comment;

    private String name_prefix = "";

    public FieldInfo(FieldNamesMode field_names_mode, String original_field_type, String jdbc_db_col_name, String comment) throws Exception {
        if (original_field_type == null) {
            throw new Exception("<field type=null. Ensure that XSD and XML are valid.");
        }
        this.original_field_type = original_field_type;
        this.database_column_name = jdbc_db_col_name;
        this.rendered_field_type = original_field_type;
        this.rendered_field_name = jdbc_db_col_name;
        if ("parameter".equals(comment) == false) {
            this.rendered_field_name = this.rendered_field_name.replace(" ", "_"); // for mysql!
            this.rendered_field_name = this.rendered_field_name.replace("[", "");
            this.rendered_field_name = this.rendered_field_name.replace("]", "");
            this.rendered_field_name = this.rendered_field_name.replace(".", "_"); // [OrderDetails].OrderID
            this.rendered_field_name = this.rendered_field_name.replace(":", "_"); // CustomerID:1 -- xerial SQLite3
        }
        if (FieldNamesMode.LOWER_CAMEL_CASE.equals(field_names_mode)) {
            this.rendered_field_name = Helpers.to_lower_camel_or_title_case(this.rendered_field_name, false);
        } else if (FieldNamesMode.TITLE_CASE.equals(field_names_mode)) {
            this.rendered_field_name = Helpers.to_lower_camel_or_title_case(this.rendered_field_name, true);
        } else if (FieldNamesMode.SNAKE_CASE.equals(field_names_mode)) {
            this.rendered_field_name = Helpers.camel_case_to_lower_snake_case(this.rendered_field_name);
            this.name_prefix = "_";
        }
        this.comment = comment;
    }

    public String getName() { // this method is for use in VM templates
        return this.rendered_field_name;
    }

    public void setName(String name) { // it may be changed
        this.rendered_field_name = name;
    }

    public String getLowerCamelCaseName() {
        String res = Helpers.to_lower_camel_or_title_case(this.rendered_field_name, false);
        return res;
    }

    public String getType() { // this method is for use in VM templates
        return rendered_field_type;
    }

    public void refine_rendered_type(String value) {
        this.rendered_field_type = value;
    }

    public String getColumnName() { // this method is for use in VM templates
        return this.database_column_name;
    }

    public boolean isPK() {
        return this.is_pk;
    }

    public void setPK(boolean pk) {
        this.is_pk = pk;
    }

    public boolean isAI() {
        return this.is_auto_increment;
    }

    public void setAI(boolean isAutoIncrement) {
        this.is_auto_increment = isAutoIncrement;
    }

    public boolean isNullable() {
        return this.is_nullable;
    }

    public void setNullable(boolean nullable) {
        this.is_nullable = nullable;
    }

    public String getComment() { // this method is for use in VM templates
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // this method is for use in VM templates ONLY
    public String getterMethod() { // NO_UCD (unused code)
        String s = this.name_prefix + this.rendered_field_name;
        String X = Helpers.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));
        return "get" + X;
    }

    // this method is for use in VM templates ONLY
    public String setterMethod() { // NO_UCD (unused code)
        String s = this.name_prefix + this.rendered_field_name;
        String X = Helpers.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));
        return "set" + X;
    }

    public String getOriginalType() {
        return original_field_type;
    }
}
