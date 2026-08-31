/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.go;

import com.sqldalmaker.cg.FieldInfo;
import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.jaxb.sdm.DtoClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * @author sqldalmaker@gmail.com
 *
 * Rendering of the fields of a Go struct: the columns of names, types and tags
 * are aligned the way 'go fmt' would align them, and the extra fields declared
 * in "<dto-class custom=..." are added.
 *
 * It has nothing to do with generating DAO methods, it used to live in 'GoCG'
 * only because that is where it was written.
 */
class GoStructFormatter {

    private static class FieldsBlock {
        int max_len = 0;
    }

    private static Map<String, FieldsBlock> _get_tp_blocks(Map<String, CustomField> custom_fields, List<FieldInfo> fields) {
        Map<String, FieldsBlock> res = new HashMap<String, FieldsBlock>();
        FieldsBlock current_block = null;
        for (FieldInfo fi : fields) {
            String just_type = GoTypes.get_type_without_import_and_tag(fi);
            if (fi.getName().isEmpty()) {
                continue;
            }
            int just_tp_len = just_type.length();
            String type_name = GoTypes.get_type_without_import(fi.getType());
            boolean tag_exists = just_tp_len < type_name.length();
            String comment = null;
            CustomField cf = custom_fields.get(fi.getName());
            if (cf != null) {
                comment = cf.comment;
            }
            if (!tag_exists && comment == null) {
                current_block = null;
            } else {
                if (current_block == null) {
                    current_block = new FieldsBlock();
                }
                if (just_tp_len > current_block.max_len) {
                    current_block.max_len = just_tp_len;
                }
            }
            res.put(fi.getName(), current_block);
        }
        return res;
    }

    private static Map<String, FieldsBlock> _get_tag_blocks(Map<String, CustomField> custom_fields, List<FieldInfo> fields) {
        Map<String, FieldsBlock> res = new HashMap<String, FieldsBlock>();
        FieldsBlock current_block = null;
        for (FieldInfo fi : fields) {
            String just_type = GoTypes.get_type_without_import_and_tag(fi);
            if (fi.getName().isEmpty()) {
                continue;
            }
            String comment = null;
            CustomField cf = custom_fields.get(fi.getName());
            if (cf != null) {
                comment = cf.comment;
            }
            if (comment == null) {
                current_block = null;
            } else {
                String type_name = GoTypes.get_type_without_import(fi.getType());
                int just_tp_len = just_type.length();
                boolean tag_exists = just_tp_len < type_name.length();
                if (tag_exists) {
                    if (current_block == null) {
                        current_block = new FieldsBlock();
                    }
                    String just_tag = type_name.substring(just_tp_len + 1).trim();
                    int just_tag_len = just_tag.length();
                    if (just_tag_len > current_block.max_len) {
                        current_block.max_len = just_tag_len;
                    }
                }
            }
            res.put(fi.getName(), current_block);
        }
        return res;
    }

    static List<FormattedField> format_fields(DtoClass jaxb_dto_class, List<FieldInfo> fields) throws Exception {
        List<CustomField> custom_field_list = _get_custom_fields(jaxb_dto_class);
        _add_custom_fields(custom_field_list, fields);
        int max_name_len = -1;
        List<FormattedField> formatted_fields = new ArrayList<FormattedField>();
        for (FieldInfo fi : fields) {
            String just_type = GoTypes.get_type_without_import_and_tag(fi);
            String name = fi.getName();
            if (name.isEmpty()) {
                formatted_fields.add(new FormattedField(just_type));
                continue;
            }
            int name_len = name.length();
            if (name_len > max_name_len) {
                max_name_len = name_len;
            }
        }
        Map<String, CustomField> custom_fields = new HashMap<>();
        for (CustomField cf : custom_field_list) {
            if (!cf.name.isEmpty()) {
                custom_fields.put(cf.name, cf);
            }
        }
        Map<String, FieldsBlock> tp_blocks = _get_tp_blocks(custom_fields, fields);
        Map<String, FieldsBlock> tag_blocks = _get_tag_blocks(custom_fields, fields);
        _set_formatted(formatted_fields, fields, max_name_len, tp_blocks, tag_blocks, custom_fields);
        return formatted_fields;
    }

    private static void _add_custom_fields(List<CustomField> custom_field_list, List<FieldInfo> fields) throws Exception {
        for (CustomField cf : custom_field_list) {
            if (cf.type.isEmpty()) {
                continue;
            }
            FieldInfo fi = new FieldInfo(FieldNamesMode.AS_IS, "", "", "");
            fi.refine_name(cf.name);
            fi.refine_rendered_type(String.format("%s %s", cf.type, cf.tag));
            fields.add(fi);
        }
    }

    private static List<CustomField> _get_custom_fields(DtoClass jaxb_dto_class) {
        List<CustomField> res = new ArrayList<CustomField>();
        String custom = jaxb_dto_class.getCustom();
        if (custom != null) {
            _parse_custom(custom, res);
        }
        return res;
    }

    private static void _set_formatted(
            List<FormattedField> formatted_fields,
            List<FieldInfo> fields,
            int max_name_len,
            Map<String, FieldsBlock> tp_blocks,
            Map<String, FieldsBlock> tag_blocks,
            Map<String, CustomField> custom_fields) {

        final String name_format = "%-" + max_name_len + "." + max_name_len + "s";
        for (FieldInfo fi : fields) {
            String just_type = GoTypes.get_type_without_import_and_tag(fi);
            if (fi.getName().isEmpty()) {
                continue;
            }
            String just_tp_fmt;
            {
                FieldsBlock block = tp_blocks.get(fi.getName());
                int max_tp_len = block == null ? 0 : block.max_len;
                if (max_tp_len == 0) {
                    just_tp_fmt = "%s";
                } else {
                    just_tp_fmt = "%-" + max_tp_len + "." + max_tp_len + "s";
                }
            }
            String type_name = GoTypes.get_type_without_import(fi.getType());
            int just_tp_len = just_type.length();
            boolean tag_exists = just_tp_len < type_name.length();
            if (tag_exists) {
                String type_format;
                FieldsBlock block = tag_blocks.get(fi.getName());
                int max_tag_len = block == null ? 0 : block.max_len;
                if (max_tag_len > 0) {
                    type_format = just_tp_fmt + " %-" + max_tag_len + "." + max_tag_len + "s";
                } else {
                    type_format = just_tp_fmt + " %s";
                }
                String just_tag = type_name.substring(just_tp_len + 1).trim();
                type_name = String.format(type_format, just_type, just_tag); // no trim
            } else {
                type_name = String.format(just_tp_fmt, just_type, ""); // no trim
            }
            String name = String.format(name_format, fi.getName());
            String fmt;
            String comment = null;
            CustomField cf = custom_fields.get(fi.getName());
            if (cf != null) {
                comment = cf.comment;
            }
            if (comment == null) {
                fmt = String.format("%s %s", name, type_name).trim();
            } else {
                if (comment.isEmpty()) {
                    fmt = String.format("%s %s //", name, type_name);
                } else {
                    fmt = String.format("%s %s // %s", name, type_name, comment.trim());
                }
            }
            formatted_fields.add(new FormattedField(fmt));
        }
    }

    private static class CustomField {
        String name = "";
        String type = "";
        String tag = "";
        String comment = null;
    }

    private static void _parse(String line, CustomField cf) {
        String[] field_parts = line.split("\\s+");
        if (field_parts.length > 0) {
            cf.name = field_parts[0];
        }
        if (field_parts.length > 1) {
            cf.type = field_parts[1]; // no spaces inside type!!!
        }
        if (field_parts.length > 2) {
            int pos_name = line.indexOf(cf.name);
            if (pos_name != -1) {
                String after_name = line.substring(pos_name + cf.name.length());
                int post_type = after_name.indexOf(cf.type);
                if (post_type != -1) {
                    cf.tag = after_name.substring(post_type + cf.type.length()).trim();
                }
            }
        }
    }

    private static final String BASE = "{base}";

    private static void _parse_custom(String custom, List<CustomField> res) {
        String[] lines = custom.split("[\\r\\n]+");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            CustomField cf = new CustomField();
            if (line.startsWith(BASE)) {
                String tp = line.substring(BASE.length()).trim();
                if (tp.isEmpty()) {
                    continue;
                }
                cf.type = tp; // no comments for {base}
                res.add(cf);
                continue;
            }
            int pos = line.indexOf("//");
            if (pos >= 0) {
                String field = line.substring(0, pos).trim();
                _parse(field, cf);
                cf.comment = line.substring(pos + 2);
            } else {
                _parse(line, cf);
            }
            res.add(cf);
        }
    }
}
