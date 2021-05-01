/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/**
 * @author sqldalmaker@gmail.com
 */
public class FieldInfo {

    private String target_type_name;
    private String jaxb_type = null;
    private String field_name;
    private String name_prefix = "";
    private final String jdbc_db_col_name; // original name in database without conversions to lower/camel case etc.
    private boolean is_auto_increment;
    private String comment;

    public FieldInfo(FieldNamesMode field_names_mode, String jdbc_java_type_name, String jdbc_db_col_name, String comment) throws Exception {
        if (jdbc_java_type_name == null) {
            throw new Exception("<field type=null. Ensure that XSD and XML are valid.");
        }
        this.jdbc_db_col_name = jdbc_db_col_name;
        this.target_type_name = jdbc_java_type_name;
        this.field_name = jdbc_db_col_name;
        if ("parameter".equals(comment) == false) {
            this.field_name = this.field_name.replace(" ", "_"); // for mysql!
            this.field_name = this.field_name.replace("[", "");
            this.field_name = this.field_name.replace("]", "");
            this.field_name = this.field_name.replace(".", "_"); // [OrderDetails].OrderID
            this.field_name = this.field_name.replace(":", "_"); // CustomerID:1 -- for latest xerial SQLite3
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

    public String get_import() throws Exception {
        if (target_type_name == null) {
            throw new Exception("Not set: target_type_name");
        }
        String[] parts = this.target_type_name.split(":");
        if (parts.length < 2) {
            return "";
        }
        return parts[0];
    }

    private String rendered_type = null;

    public void refine_rendered_type(String rendered_type) {
        this.rendered_type = rendered_type;
    }

    // this method is only for use in VM templates, don't use it in Java code:

    public String getType() throws Exception {
        if (rendered_type != null) {
            return rendered_type;
        }
        return calc_target_type_name();
    }

    public String calc_target_type_name() throws Exception {
        if (target_type_name == null) {
            throw new Exception("Not set: target_type_name");
        }
        String[] parts = this.target_type_name.split(":");
        String res;
        if (parts.length < 2) {
            res = this.target_type_name;
        } else {
            res = parts[1];
        }
        return res;
    }

    public String type_comment_from_jaxb_type_name() {
        if (jaxb_type == null) {
            return "";
        }
        int pos = this.jaxb_type.indexOf(' ');
        if (pos == -1) {
            return "";
        } else {
            return this.jaxb_type.substring(pos + 1);
        }
    }

    public String _type_name_from_jaxb_type_name() {
        if (this.jaxb_type == null) {
            return "";
        }
        int pos = this.jaxb_type.indexOf(' ');
        if (pos == -1) {
            return this.jaxb_type;
        } else {
            return this.jaxb_type.substring(0, pos);
        }
    }

    public void refine_jdbc_java_type_name(String jdbc_java_type_name) {
        this.target_type_name = jdbc_java_type_name;
    }

    public void assign_jaxb_type(String jaxb_type) throws Exception {
        if (jaxb_type == null) {
            throw new Exception("Invalid <field type...");
        }
        this.jaxb_type = jaxb_type.trim().replace("'", "\"");
        jaxb_type = _type_name_from_jaxb_type_name();
        if (!jaxb_type.equals("*")) {
            this.target_type_name = jaxb_type;
        }
    }

    boolean type_renamed = false;

    public void set_target_type_by_map(String target_type_name) throws Exception {
        if (this.type_renamed) {
            // System.out.println(this.field_type_name);
            return;
        }
        if (target_type_name == null) {
            throw new Exception("Invalid <type-map... Ensure that XSD and XML are valid.");
        }
        this.type_renamed = true;
        this.target_type_name = target_type_name;
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
        return this.jdbc_db_col_name;
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
