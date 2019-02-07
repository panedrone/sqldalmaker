/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class FieldInfo {

    private String type;

    private String name;

    private String name_prefix = "";

    private final String column_name; // original name in database without conversions to lower/camel case etc.

    private boolean is_auto_increment;

    public FieldInfo(FieldNamesMode field_names_mode, String field_type_name, String col_name) {

        column_name = col_name;

        type = field_type_name;

        if (FieldNamesMode.TO_LOWER_CASE.equals(field_names_mode)) {

            name = column_name.toLowerCase();

        } else if (FieldNamesMode.TO_LOWER_CAMEL_CASE.equals(field_names_mode)) {

            name = toLowerCamelCase(column_name);

        } else if (FieldNamesMode.PYTHON_RUBY.equals(field_names_mode)) {

            name = Helpers.camel_case_to_lower_under_scores(column_name);

            name_prefix = "_";

        } else {

            name = column_name;
        }

        name = name.replace(" ", "_"); // for mysql!

        name = name.replace(".", "_"); // [OrderDetails].OrderID
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getColumnName() {
        return column_name;
    }

    public boolean isAutoIncrement() {
        return is_auto_increment;
    }

    public void setAutoIncrement(boolean isAutoIncrement) {
        this.is_auto_increment = isAutoIncrement;
    }

    // called from Velocity script
    public String getterMethod() { // NO_UCD (unused code)

        String s = name_prefix + name;

        String X = Helpers.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));

        return "get" + X;
    }

    // called from Velocity script
    public String setterMethod() { // NO_UCD (unused code)

        String s = name_prefix + name;

        String X = Helpers.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));

        return "set" + X;
    }

    public static String toLowerCamelCase(String str) {

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

                    sb.append(s.substring(1, s.length()).toLowerCase());
                }
            }
        }

        return sb.toString();
    }
}
