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

    private final String database_column_name; // original name in database without conversions to lower/camel case etc.

    private boolean is_auto_increment;
    private boolean is_pk = false;

    private String comment;

    private String name_prefix = "";

    public FieldInfo(FieldNamesMode field_names_mode, String jdbc_java_type_name, String jdbc_db_col_name, String comment) throws Exception {
        if (jdbc_java_type_name == null) {
            throw new Exception("<field type=null. Ensure that XSD and XML are valid.");
        }
        this.database_column_name = jdbc_db_col_name;
        this.rendered_field_type = jdbc_java_type_name;
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

    public boolean isPK() { // this method is for use in VM templates
        return this.is_pk;
    }

    public void setPK(boolean pk) {
        this.is_pk = pk;
    }

    public boolean isAI() { // this method is for use in VM templates
        return this.is_auto_increment;
    }

    public void setAI(boolean isAutoIncrement) {
        this.is_auto_increment = isAutoIncrement;
    }

    public String getComment() { // this method is for use in VM templates
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String get_import() {
        if (rendered_field_type == null) {
            return null;
        }
        String[] parts = this.rendered_field_type.split(":");
        if (parts.length < 2) {
            return "";
        }
        return parts[0];
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
}
