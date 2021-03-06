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
            this.field_name = this.field_name.replace(".", "_"); // [OrderDetails].OrderID
            this.field_name = this.field_name.replace(":", "_"); // CustomerID:1 -- for latest xenian SQLite3
        }
        if (FieldNamesMode.LOWER_CASE.equals(field_names_mode)) {
            this.field_name = this.field_name.toLowerCase();
        } else if (FieldNamesMode.LOWER_CAMEL_CASE.equals(field_names_mode)) {
            this.field_name = toCamelCase(this.field_name, false);
        } else if (FieldNamesMode.TITLE_CASE.equals(field_names_mode)) {
            this.field_name = toCamelCase(this.field_name, true);
        } else if (FieldNamesMode.SNAKE_CASE.equals(field_names_mode)) {
            this.field_name = Helpers.camel_case_to_lower_under_scores(this.field_name);
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

    public String getType() {
        return this.field_type_name;
    }

    boolean type_renamed = false;

    public void set_type_by_map(String field_type_name) throws Exception {
    	if (type_renamed) {
        	System.out.println(this.field_type_name);
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

    public static String toCamelCase(String str, boolean title_case) {
        if (!str.contains("_")) {
            boolean all_is_upper_case = Helpers.is_upper_case(str);
            if (all_is_upper_case) {
                str = str.toLowerCase();
            }
            return Helpers.replace_char_at(str, 0, Character.toLowerCase(str.charAt(0)));
        }
        // http://stackoverflow.com/questions/1143951/what-is-the-simplest-way-to-convert-a-java-string-from-all-caps-words-separated
        StringBuilder sb = new StringBuilder();
        String[] arr = str.split("_");
        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            if (s.length() == 0) {
                continue; // E.g. _ALL_FILE_GROUPS
            }
            if (i == 0) {
                sb.append(s.toLowerCase());
            } else {
                sb.append(Character.toUpperCase(s.charAt(0)));
                if (s.length() > 1) {
                    sb.append(s.substring(1).toLowerCase());
                }
            }
        }
        if (title_case) {
            char upper = Character.toTitleCase(sb.charAt(0));
            sb.setCharAt(0, upper);
        }
        return sb.toString();
    }
}
