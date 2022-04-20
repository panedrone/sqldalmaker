/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.*;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.GlobalMacro;
import com.sqldalmaker.jaxb.settings.GlobalMacros;
import com.sqldalmaker.jaxb.settings.Type;
import com.sqldalmaker.jaxb.settings.TypeMap;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
public class JaxbUtils {

    public static class JaxbMacros {

        private final Map<String, String> built_in = new HashMap<String, String>();
        private final Map<String, String> custom = new HashMap<String, String>();
        private final Map<String, String> custom_vm = new HashMap<String, String>();

        public JaxbMacros(GlobalMacros jaxb_global_makros) {
            if (jaxb_global_makros == null) {
                return;
            }
            for (GlobalMacro m : jaxb_global_makros.getGlobalMacro()) {
                String name = m.getName();
                String value = m.getValue();
                if (value.equals("=built-in=")) {
                    built_in.put(name, value);
                } else if (name.startsWith("${vm:") && name.endsWith("}")) {
                    custom_vm.put(name, value);
                } else {
                    custom.put(name, value);
                }
            }
        }

        public Set<String> get_built_in_names() {
            return built_in.keySet();
        }

        public Set<String> get_custom_names() {
            return custom.keySet();
        }

        public Set<String> get_custom_vm_names() {
            return custom_vm.keySet();
        }

        public String get_custom(String name) {
            return custom.get(name);
        }

        public String get_custom_vm(String name) {
            return custom_vm.get(name);
        }

        public void substitute_type_params(FieldInfo fi) {
            String type_name = fi.getType();
            int local_field_type_params_start = type_name.indexOf('|');
            Map<String, String> params = new HashMap<String, String>();
            if (local_field_type_params_start != -1) {
                String params_str = type_name.substring(local_field_type_params_start);
                String[] parts = params_str.split("[|]");
                for (String p : parts) {
                    int param_value_start = p.indexOf(':');
                    if (param_value_start == -1) {
                        continue;
                    }
                    String param_name = p.substring(0, param_value_start);
                    String param_value = p.substring(param_value_start + 1);
                    params.put("${" + param_name + "}", param_value);
                }
                type_name = type_name.substring(0, local_field_type_params_start);
                for (String name : params.keySet()) {
                    if (type_name.contains(name)) {
                        String param_value = params.get(name);
                        type_name = type_name.replace(name, param_value);
                    }
                }
            }
            Set<String> macro_calls = _find_macro_calls(type_name);
            for (String macro_call : macro_calls) {
                type_name = type_name.replace(macro_call, "");
            }
            fi.refine_rendered_type(type_name);
        }
    }

    private static Set<String> _find_macro_calls(String input) {
        // https://stackoverflow.com/questions/53904144/regex-matching-even-amount-of-brackets
        Set<String> res = new HashSet<String>();
        int count = 0;
        int start = 0;
        // int end;
        for (int i = 0; i < input.length(); ++i) {
            String tail = input.substring(i);
            // if (input.charAt(i) == '{') {
            if (tail.startsWith("${")) {
                // ++count;
                // if (count == 1) {
                count += 2;
                if (count == 2) {
                    start = i;
                }
            }
            if (input.charAt(i) == '}') {
                //--count;
                count -= 2;
                if (count == 0) {
                    res.add(input.substring(start, i + 1));
                    //System.out.println();
                }
            }
        }
        return res;
    }

    public static class JaxbTypeMap {

        private final Map<String, String> detected = new HashMap<String, String>();
        private final String default_type;

        public JaxbTypeMap(TypeMap jaxb_type_map) {
            if (jaxb_type_map == null) {
                default_type = null;
                return;
            }
            default_type = jaxb_type_map.getDefault();
            for (Type t : jaxb_type_map.getType()) {
                detected.put(t.getDetected(), t.getTarget());
            }
        }

        // 'detected' in here means
        //      1) detected using JDBC or
        //      2) detected from explicit declarations in XML meta-program

        public String get_target_type_name(String detected_type_name) {
            if (detected.isEmpty()) {
                // if no re-definitions, pass any type as-is (independently of 'default')
                return detected_type_name;
            }
            if (detected.containsKey(detected_type_name)) {
                return detected.get(detected_type_name);
            }
            if (default_type == null || default_type.trim().length() == 0) {
                return detected_type_name; // rendered as-is if not found besides of "detected"
            }
            return default_type;
        }

    } // class JaxbTypeMap

    public static String get_jaxb_node_name(Object jaxb_node) {
        XmlRootElement attr = jaxb_node.getClass().getAnnotation(XmlRootElement.class);
        return attr.name();
    }

    public static DtoClass find_jaxb_dto_class(
            String dto_class_name,
            DtoClasses jaxb_dto_classes) throws Exception {

        if (dto_class_name == null || dto_class_name.length() == 0) {
            throw new Exception("Invalid name of DTO class: " + dto_class_name);
        }
        DtoClass res = null;
        int found = 0;
        for (DtoClass cls : jaxb_dto_classes.getDtoClass()) {
            String name = cls.getName();
            if (name != null && name.equals(dto_class_name)) {
                res = cls;
                found++;
            }
        }
        if (found == 0) {
            throw new Exception("DTO XML element not found: '" + dto_class_name + "'");
        } else if (found > 1) {
            throw new Exception("Duplicate DTO XML elements for name='" + dto_class_name + "' found.");
        }
        return res;
    }

    public static void process_jaxb_dao_class(
            IDaoCG dao_cg,
            String dao_class_name,
            DaoClass jaxb_dao_class,
            List<String> methods) throws Exception {

        if (jaxb_dao_class.getCrudOrCrudAutoOrQuery() != null) {
            for (int i = 0; i < jaxb_dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {
                Object jaxb_element = jaxb_dao_class.getCrudOrCrudAutoOrQuery().get(i);
                if (jaxb_element instanceof Query || jaxb_element instanceof QueryList
                        || jaxb_element instanceof QueryDto || jaxb_element instanceof QueryDtoList) {
                    StringBuilder buf = dao_cg.render_jaxb_query(jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof ExecDml) {
                    StringBuilder buf = dao_cg.render_jaxb_exec_dml((ExecDml) jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof TypeCrud) {
                    StringBuilder buf = dao_cg.render_jaxb_crud(dao_class_name, (TypeCrud) jaxb_element);
                    methods.add(buf.toString());
                } else {
                    throw new Exception("Unexpected element found in DTO XML file");
                }
            }
        }
    }

    private static boolean _process_jaxb_crud_create(
            IDaoCG dao_cg,
            TypeCrud jaxb_type_crud,
            String dto_class_name,
            String table_name,
            FieldNamesMode field_names_mode,
            StringBuilder code_buff) throws Exception {

        String method_name = null;
        if (jaxb_type_crud.getCreate() != null) {
            method_name = jaxb_type_crud.getCreate().getMethod();
        } else {
            if (jaxb_type_crud instanceof CrudAuto) {
                method_name = "create" + dto_class_name;
            }
        }
        if (method_name == null) {
            return true;
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        boolean fetch_generated = jaxb_type_crud.isFetchGenerated();
        String generated = jaxb_type_crud.getGenerated();
        StringBuilder tmp = dao_cg.render_crud_create(null, method_name, table_name, dto_class_name, fetch_generated,
                generated);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_read_all(
            IDaoCG dao_cg,
            TypeCrud jaxb_type_crud,
            String dto_class_name,
            String table_name,
            FieldNamesMode field_names_mode,
            StringBuilder code_buff) throws Exception {

        String method_name = null;
        if (jaxb_type_crud.getReadAll() != null) {
            method_name = jaxb_type_crud.getReadAll().getMethod();
        } else {
            if (jaxb_type_crud instanceof CrudAuto) {
                method_name = "read" + dto_class_name + "List";
            }
        }
        if (method_name == null) {
            return true;
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, null, true);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_read(
            IDaoCG dao_cg,
            TypeCrud jaxb_type_crud,
            String dto_class_name,
            String table_name,
            String explicit_pk,
            FieldNamesMode field_names_mode,
            StringBuilder code_buff) throws Exception {

        String method_name = null;
        if (jaxb_type_crud.getRead() != null) {
            method_name = jaxb_type_crud.getRead().getMethod();
        } else {
            if (jaxb_type_crud instanceof CrudAuto) {
                method_name = "read" + dto_class_name;
            }
        }
        if (method_name == null) {
            return true;
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, explicit_pk, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_update(
            IDaoCG dao_cg,
            TypeCrud jaxb_type_crud,
            String dao_class_name,
            String dto_class_name,
            String table_name,
            String explicit_primary_keys,
            FieldNamesMode field_names_mode,
            StringBuilder code_buff) throws Exception {

        String method_name = null;
        if (jaxb_type_crud.getUpdate() != null) {
            method_name = jaxb_type_crud.getUpdate().getMethod();
        } else {
            if (jaxb_type_crud instanceof CrudAuto) {
                method_name = "update" + dto_class_name;
            }
        }
        if (method_name == null) {
            return true;
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_update(dao_class_name, method_name, table_name, explicit_primary_keys,
                dto_class_name, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_delete(
            IDaoCG dao_cg,
            TypeCrud jaxb_type_crud,
            String dao_class_name,
            String dto_class_name,
            String table_name,
            String explicit_pk,
            FieldNamesMode field_names_mode,
            StringBuilder code_buff) throws Exception {

        String method_name = null;
        if (jaxb_type_crud.getDelete() != null) {
            method_name = jaxb_type_crud.getDelete().getMethod();
        } else {
            if (jaxb_type_crud instanceof CrudAuto) {
                method_name = "delete" + dto_class_name;
            }
        }
        if (method_name == null) {
            return true;
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_delete(dao_class_name, dto_class_name, method_name, table_name, explicit_pk);
        code_buff.append(tmp);
        return true;
    }

    public static StringBuilder process_jaxb_crud(IDaoCG dao_cg,
                                                  FieldNamesMode field_names_mode,
                                                  TypeCrud jaxb_type_crud,
                                                  String dao_class_name,
                                                  String dto_class_name) throws Exception {

        String table_name = jaxb_type_crud.getTable();
        String explicit_primary_keys = jaxb_type_crud.getPk();
        boolean is_empty = true;
        StringBuilder code_buff = new StringBuilder();
        if (_process_jaxb_crud_create(dao_cg, jaxb_type_crud, dto_class_name, table_name, field_names_mode,
                code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_read_all(dao_cg, jaxb_type_crud, dto_class_name, table_name, field_names_mode,
                code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_read(dao_cg, jaxb_type_crud, dto_class_name, table_name, explicit_primary_keys,
                field_names_mode, code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_update(dao_cg, jaxb_type_crud, dao_class_name, dto_class_name, table_name, explicit_primary_keys,
                field_names_mode, code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_delete(dao_cg, jaxb_type_crud, dao_class_name, dto_class_name, table_name, explicit_primary_keys,
                field_names_mode, code_buff)) {
            is_empty = false;
        }
        if ((jaxb_type_crud instanceof Crud) && is_empty) {
            String node_name = get_jaxb_node_name(jaxb_type_crud);
            throw new Exception("Element '" + node_name + "' is empty. Add the method declarations or change to 'crud-auto'");
        }
        return code_buff;
    }
}
