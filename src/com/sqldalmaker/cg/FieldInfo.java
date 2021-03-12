/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.cg;

/**
 * @author sqldalmaker@gmail.com
 */
public class FieldInfo {

    private String field_type_name;
    private String field_name;
    private String name_prefix = "";
    private final String db_col_name; // original name in database without conversions to lower/camel case etc.
    private boolean is_auto_increment;
    private String comment;

    public FieldInfo(FieldNamesMode field_names_mode, String field_type_name, String db_col_name, String comment) throws Exception {
        this.db_col_name = db_col_name;
        if (field_type_name == null) {
            throw new Exception("<field type=null. Ensure that XSD and XML are valid.");
        }
        this.field_type_name = field_type_name;
        field_name = db_col_name;
        if ("parameter".equals(comment) == false) {
            this.field_name = this.field_name.replace(" ", "_"); // for mysql!
            this.field_name = this.field_name.replace("[", "");
            this.field_name = this.field_name.replace("]", "");
            this.field_name = this.field_name.replace(".", "_"); // [OrderDetails].OrderID
            this.field_name = this.field_name.replace(":", "_"); // CustomerID:1 -- for latest xenian SQLite3
        }
        if (FieldNamesMode.LOWER_CAMEL_CASE.equals(field_names_mode)) {
            this.field_name = Helpers.to_lower_camel_or_title_case(this.field_name, false);
        } else if (FieldNamesMode.TITLE_CASE.equals(field_names_mode)) {
            this.field_name = Helpers.to_lower_camel_or_title_case(this.field_name, true);
        } else if (FieldNamesMode.SNAKE_CASE.equals(field_names_mode)) {
            this.field_name = Helpers.camel_case_to_snake_case(this.field_name);
            this.name_prefix = "_";
        }
        this.comment = comment;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getImport() {
        String[] parts = this.field_type_name.split(":");
        if (parts.length < 2) {
            return null;
        }
        return parts[0];
    }

    public String getType() {
        String[] parts = this.field_type_name.split(":");
        if (parts.length < 2) {
            return this.field_type_name;
        }
        return parts[1];
    }

    boolean type_renamed = false;

    public void set_type_by_map(String field_type_name) throws Exception {
        if (type_renamed) {
            // System.out.println(this.field_type_name);
            return;
        }
        if (field_type_name == null) {
            throw new Exception("Invalid <type-map... Ensure that XSD and XML are valid.");
        }
        type_renamed = true;
        this.field_type_name = field_type_name;
    }

    public void refine_type(String field_type_name) {
        this.field_type_name = field_type_name;
    }

    public String getName() {
        return this.field_name;
    }

    public void setName(String name) { // it may be changed
        this.field_name = name;
    }

    public String getLowerCamelCaseName() {
        String res = Helpers.to_lower_camel_or_title_case(this.field_name, false);
        return res;
    }

    public String getColumnName() {
        return this.db_col_name;
    }

    public boolean isAutoIncrement() {
        return this.is_auto_increment;
    }

    public void setAutoIncrement(boolean isAutoIncrement) {
        this.is_auto_increment = isAutoIncrement;
    }

    // called from Velocity script
    public String getterMethod() { // NO_UCD (unused code)
        String s = this.name_prefix + this.field_name;
        String X = Helpers.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));
        return "get" + X;
    }

    // called from Velocity script
    public String setterMethod() { // NO_UCD (unused code)
        String s = this.name_prefix + this.field_name;
        String X = Helpers.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));
        return "set" + X;
    }
}
