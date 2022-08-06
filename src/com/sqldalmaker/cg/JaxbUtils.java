/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.*;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Macros;
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
        private final Map<String, Macros.Macro> custom_vm = new HashMap<String, Macros.Macro>();

        public JaxbMacros(Macros jaxb_makros) {
            if (jaxb_makros == null) {
                return;
            }
            for (Macros.Macro m : jaxb_makros.getMacro()) {
                String name = m.getName();
                String value = m.getValue();
                if (value == null) { // it is optional
                    if (m.getVm() != null || m.getVmXml() != null) {
                        custom_vm.put(name, m);
                    }
                } else {
                    if (value.equals("=built-in=")) {
                        built_in.put(name, value);
                    } else {
                        custom.put(name, value);
                    }
                }
            }
        }

        public Set<String> get_built_in_names() {
            return built_in.keySet();
        }

        public Set<String> get_custom_names() {
            return custom.keySet();
        }

        public Set<String> get_vm_macro_names() {
            return custom_vm.keySet();
        }

        public String get_custom(String name) {
            return custom.get(name);
        }

        public Macros.Macro get_vm_macro(String name) {
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
        boolean inside_of_macro = false;
        int start = -1;
        // int end;
        for (int i = 0; i < input.length(); ++i) {
            String tail = input.substring(i);
            if (tail.startsWith("${")) {
                inside_of_macro = true;
                start = i;
                continue;
            }
            if (inside_of_macro && input.charAt(i) == '}') {
                res.add(input.substring(start, i + 1));
                inside_of_macro = false;
                start = -1;
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

    public static DtoClass find_jaxb_dto_class(String dto_class_name,
                                               DtoClasses jaxb_dto_classes) throws Exception {

        if (dto_class_name == null || dto_class_name.length() == 0) {
            throw new Exception("Invalid DTO class name: " + dto_class_name);
        }
        for (DtoClass cls : jaxb_dto_classes.getDtoClass()) {
            String name = cls.getName();
            if (name != null && name.equals(dto_class_name)) {
                return cls;
            }
        }
        throw new Exception("DTO XML element not found: '" + dto_class_name + "'");
    }

    public static void process_jaxb_dao_class(IDaoCG dao_cg,
                                              String dao_class_name,
                                              DaoClass jaxb_dao_class,
                                              List<String> methods) throws Exception {

        if (jaxb_dao_class.getCrudOrQueryOrQueryList() != null) {
            for (int i = 0; i < jaxb_dao_class.getCrudOrQueryOrQueryList().size(); i++) {
                Object jaxb_element = jaxb_dao_class.getCrudOrQueryOrQueryList().get(i);
                if (jaxb_element instanceof Query || jaxb_element instanceof QueryList
                        || jaxb_element instanceof QueryDto || jaxb_element instanceof QueryDtoList) {
                    StringBuilder buf = dao_cg.render_jaxb_query(jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof ExecDml) {
                    StringBuilder buf = dao_cg.render_jaxb_exec_dml((ExecDml) jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof Crud) {
                    StringBuilder buf = dao_cg.render_jaxb_crud(dao_class_name, (Crud) jaxb_element);
                    methods.add(buf.toString());
                } else {
                    throw new Exception("Unexpected element found in DTO XML file");
                }
            }
        }
    }

    private static boolean _process_jaxb_crud_create(IDaoCG dao_cg,
                                                     Crud jaxb_type_crud,
                                                     String dto_class_name,
                                                     String table_name,
                                                     FieldNamesMode field_names_mode,
                                                     StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getCreate() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getCreate().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("create", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        boolean fetch_generated = jaxb_type_crud.isFetchGenerated();
        String generated = jaxb_type_crud.getGenerated();
        StringBuilder tmp = dao_cg.render_crud_create(null, method_name, table_name, dto_class_name, fetch_generated,
                generated);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_read_all(IDaoCG dao_cg,
                                                       Crud jaxb_type_crud,
                                                       String dto_class_name,
                                                       String table_name,
                                                       FieldNamesMode field_names_mode,
                                                       StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getReadAll() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getReadAll().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("read", dto_class_name + "List");
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, null, true);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_read(IDaoCG dao_cg,
                                                   Crud jaxb_type_crud,
                                                   String dto_class_name,
                                                   String table_name,
                                                   String explicit_pk,
                                                   FieldNamesMode field_names_mode,
                                                   StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getRead() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getRead().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("read", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, explicit_pk, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_update(IDaoCG dao_cg,
                                                     Crud jaxb_type_crud,
                                                     String dao_class_name,
                                                     String dto_class_name,
                                                     String table_name,
                                                     String explicit_primary_keys,
                                                     FieldNamesMode field_names_mode,
                                                     StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getUpdate() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getUpdate().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("update", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_update(dao_class_name, method_name, table_name, explicit_primary_keys,
                dto_class_name, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_delete(IDaoCG dao_cg,
                                                     Crud jaxb_type_crud,
                                                     String dao_class_name,
                                                     String dto_class_name,
                                                     String table_name,
                                                     String explicit_pk,
                                                     FieldNamesMode field_names_mode,
                                                     StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getDelete() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getDelete().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("delete", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_delete(dao_class_name, dto_class_name, method_name, table_name, explicit_pk);
        code_buff.append(tmp);
        return true;
    }

    private static String build_method_name(String base, String dto_class_name) {
        int model_name_end_index = dto_class_name.indexOf('-');
        if (model_name_end_index == -1) {
            return base + dto_class_name;
        }
        dto_class_name = dto_class_name.substring(model_name_end_index + 1);
        return base + dto_class_name;
    }

    public static StringBuilder process_jaxb_crud(IDaoCG dao_cg,
                                                  FieldNamesMode field_names_mode,
                                                  Crud jaxb_type_crud,
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
        if (is_empty) {
            jaxb_type_crud.setCreate(new TypeMethod());
            jaxb_type_crud.setRead(new TypeMethod());
            jaxb_type_crud.setReadAll(new TypeMethod());
            jaxb_type_crud.setUpdate(new TypeMethod());
            jaxb_type_crud.setDelete(new TypeMethod());
            return process_jaxb_crud(dao_cg, field_names_mode, jaxb_type_crud, dao_class_name, dto_class_name);
        }
        return code_buff;
    }
}
