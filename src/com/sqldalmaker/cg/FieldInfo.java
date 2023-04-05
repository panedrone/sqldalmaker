/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
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
    private final String original_scalar_type;
    private String scalar_type;
    private String model;

    private final String database_column_name; // original name in database without conversions to lower/camel case etc.

    private int database_column_size = 0;

    private boolean is_auto_increment = false;
    private boolean is_pk = false;

    private boolean is_nullable = false;

    private int decimal_digits;

    private String foreign_key = null;

    private boolean is_indexed = false;
    private boolean is_unique = false;

    private String comment;

    private String name_prefix = "";

    private String assign_func;

    public FieldInfo(FieldNamesMode field_names_mode, String original_field_type, String jdbc_db_col_name, String comment) throws Exception {
        if (original_field_type == null) {
            throw new Exception("<field type=null. Ensure that XSD and XML files of meta-program are valid.");
        }
        this.original_field_type = original_field_type;
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
        this.scalar_type = original_scalar_type;
        this.database_column_name = jdbc_db_col_name;
        this.rendered_field_type = original_field_type;
        this.rendered_field_name = jdbc_db_col_name;

        if (this.rendered_field_name.contains("func(") == false &&
                this.rendered_field_name.contains("new RowHandler[]") == false) {

            this.rendered_field_name = this.rendered_field_name.replace("-", "_"); // for MySQL: `api-key`
            this.rendered_field_name = this.rendered_field_name.replace(".", "_"); // [OrderDetails].OrderID
            this.rendered_field_name = this.rendered_field_name.replace(":", "_"); // CustomerID:1 -- xerial SQLite3
            if ("parameter".equals(comment) == false) {
                // apply only for fields
                this.rendered_field_name = this.rendered_field_name.replace(" ", "_"); // for MySQL!
                this.rendered_field_name = this.rendered_field_name.replace("[", "");  // [OrderDetails].OrderID
                this.rendered_field_name = this.rendered_field_name.replace("]", "");  // [OrderDetails].OrderID
            }
            // don't apply to callback-params
            if (FieldNamesMode.LOWER_CAMEL_CASE.equals(field_names_mode)) {
                this.rendered_field_name = Helpers.to_lower_camel_or_title_case(this.rendered_field_name, false);
            } else if (FieldNamesMode.TITLE_CASE.equals(field_names_mode)) {
                this.rendered_field_name = Helpers.to_lower_camel_or_title_case(this.rendered_field_name, true);
            } else if (FieldNamesMode.SNAKE_CASE.equals(field_names_mode)) {
                this.rendered_field_name = Helpers.camel_case_to_lower_snake_case(this.rendered_field_name);
                this.name_prefix = "_";
            }
        }
        this.comment = comment;
    }

    public String getAssignFunc() { // this method is for use in VM templates
        if (assign_func == null || assign_func.trim().length() == 0) {
            assign_func = "SetAny";
        }
        return this.assign_func;
    }

    public String getName() { // this method is for use in VM templates
        return this.rendered_field_name;
    }

    public void setName(String name) { // it may be changed
        this.rendered_field_name = name;
    }

    public String getLowerCamelCaseName() { // for Golang VM template
        String res = Helpers.to_lower_camel_or_title_case(this.rendered_field_name, false);
        return res;
    }

    public String getType() { // this method is for use in VM templates
        return rendered_field_type;
    }

    public void refine_rendered_type(String type) {
        String[] assign_parts = type.split("->");
        if (assign_parts.length > 1) {
            this.rendered_field_type = assign_parts[0].trim();
            this.assign_func = assign_parts[1].trim();
            return;
        }
        this.rendered_field_type = type.trim();
    }

    public String getColumnName() { // this method is for use in VM templates
        return this.database_column_name;
    }

    public int getColumnSize() { // this method is for use in VM templates
        if (this.is_auto_increment) {
            return 0;
        }
        return this.database_column_size;
    }

    public void setColumnSize(int size) {
        this.database_column_size = size;
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

    public String getFK() {
        return foreign_key;
    }

    public void setFK(String fk) {
        foreign_key = fk;
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

    public boolean isIndexed() {
        return is_indexed;
    }

    public void setIndexed(boolean indexed) {
        is_indexed = indexed;
    }

    public boolean isUnique() {
        return is_unique;
    }

    public void setUnique(boolean unique) {
        is_unique = unique;
    }

    public String getOriginalScalarType() {
        return original_scalar_type;
    }

    public String getScalarType() {
        return scalar_type;
    }

    public void refine_scalar_type(String type_name) {
        scalar_type = type_name;
    }

    public String getModel() {
        return model;
    }

    public int getDecimalDigits() { // this method is for use in VM templates
        return this.decimal_digits;
    }

    public void setDecimalDigits(int decimal_digits) {
        this.decimal_digits = decimal_digits;
    }
}
