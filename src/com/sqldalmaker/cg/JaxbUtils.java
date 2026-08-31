/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;

import com.sqldalmaker.jaxb.sdm.Crud;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.sdm.ExecDml;
import com.sqldalmaker.jaxb.sdm.Query;
import com.sqldalmaker.jaxb.sdm.QueryDto;
import com.sqldalmaker.jaxb.sdm.QueryDtoList;
import com.sqldalmaker.jaxb.sdm.QueryList;
import com.sqldalmaker.jaxb.sdm.TypeMethod;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.08.2026 14:00 1.331 Claude refactor
 * 30.05.2024 13:00 1.299
 * 11.02.2024 06.-- 1.293
 * 27.03.2023 10:23
 * 21.11.2022 22:07
 * 16.11.2022 08:02 1.269
 * 25.10.2022 09:26
 * 06.08.2022 08:37 1.261
 * 05.05.2022 10:44
 * 21.04.2022 20:21
 * 13.09.2021 10:15
 * 11.05.2021 05:21 1.201
 * 22.03.2021 21:19
 * 15.05.2020 19:11
 */
public class JaxbUtils {

    /**
     * Flattens the XML meta-program of one DAO class into a list of methods to render.
     * Everything that does not depend on the target language is resolved here: method
     * names (including the default ones of an empty "&lt;crud/&gt;"), parameter descriptors,
     * table name, PK. The caller just walks the list - this function calls nothing back.
     */
    public static List<DaoMethodInfo> plan_dao_methods(
            DaoClass jaxb_dao_class,
            List<DtoClass> jaxb_dto_classes,
            FieldNamesMode field_names_mode) throws Exception {

        List<DaoMethodInfo> res = new ArrayList<DaoMethodInfo>();
        List<Object> jaxb_elements = jaxb_dao_class.getCrudOrQueryOrQueryList();
        if (jaxb_elements == null) {
            return res;
        }
        for (Object jaxb_element : jaxb_elements) {
            if (jaxb_element instanceof Query || jaxb_element instanceof QueryList
                    || jaxb_element instanceof QueryDto || jaxb_element instanceof QueryDtoList) {
                res.add(_plan_query(jaxb_element));
            } else if (jaxb_element instanceof ExecDml) {
                res.add(_plan_exec_dml((ExecDml) jaxb_element));
            } else if (jaxb_element instanceof Crud) {
                _plan_crud((Crud) jaxb_element, jaxb_dto_classes, field_names_mode, res);
            } else {
                throw new Exception("Unexpected element found in DTO XML file");
            }
        }
        return res;
    }

    private static DaoQueryMethodInfo _plan_query(Object jaxb_query) throws Exception {
        boolean fetch_list = (jaxb_query instanceof QueryDtoList) || (jaxb_query instanceof QueryList);
        boolean return_type_is_dto = (jaxb_query instanceof QueryDto) || (jaxb_query instanceof QueryDtoList);
        String jaxb_method;
        String jaxb_ref;
        boolean jaxb_is_external_sql;
        String jaxb_dto_or_return_type;
        if (jaxb_query instanceof Query) {
            Query q = (Query) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getReturnType();
        } else if (jaxb_query instanceof QueryList) {
            QueryList q = (QueryList) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getReturnType();
        } else if (jaxb_query instanceof QueryDto) {
            QueryDto q = (QueryDto) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getDto();
        } else if (jaxb_query instanceof QueryDtoList) {
            QueryDtoList q = (QueryDtoList) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getDto();
        } else {
            throw new Exception("Unexpected JAXB node: " + get_jaxb_node_name(jaxb_query));
        }
        String xml_node_name = get_jaxb_node_name(jaxb_query);
        String error_context = "<" + xml_node_name + " method=\"" + jaxb_method
                + "\" ref=\"" + jaxb_ref + "\"...\n";
        MethodDeclarations.check_required_attr(xml_node_name, jaxb_method);
        try {
            String[] parsed = MethodDeclarations.parse_method_declaration(jaxb_method);
            DaoQueryMethodInfo res = new DaoQueryMethodInfo();
            res.error_context = error_context;
            res.method_name = parsed[0];
            res.param_descriptors = MethodDeclarations.get_listed_items(parsed[1], false);
            res.ref = jaxb_ref;
            res.external_sql = jaxb_is_external_sql;
            res.return_type_is_dto = return_type_is_dto;
            res.dto_or_return_type = jaxb_dto_or_return_type;
            res.fetch_list = fetch_list;
            return res;
        } catch (Throwable e) {
            throw new Exception(Helpers.get_error_message(error_context, e));
        }
    }

    private static DaoExecDmlMethodInfo _plan_exec_dml(ExecDml jaxb_exec_dml) throws Exception {
        String method = jaxb_exec_dml.getMethod();
        String ref = jaxb_exec_dml.getRef();
        String xml_node_name = get_jaxb_node_name(jaxb_exec_dml);
        String error_context = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";
        MethodDeclarations.check_required_attr(xml_node_name, method);
        try {
            String[] parsed = MethodDeclarations.parse_method_declaration(method);
            DaoExecDmlMethodInfo res = new DaoExecDmlMethodInfo();
            res.error_context = error_context;
            res.xml_node_name = xml_node_name;
            res.method_name = parsed[0];
            res.param_descriptors = MethodDeclarations.get_listed_items(parsed[1], true);
            res.ref = ref;
            res.external_sql = jaxb_exec_dml.isExternalSql();
            return res;
        } catch (Throwable e) {
            throw new Exception(Helpers.get_error_message(error_context, e));
        }
    }

    private static void _plan_crud(
            Crud jaxb_crud,
            List<DtoClass> jaxb_dto_classes,
            FieldNamesMode field_names_mode,
            List<DaoMethodInfo> res) throws Exception {

        String node_name = get_jaxb_node_name(jaxb_crud);
        String dto_class_name = jaxb_crud.getDto();
        if (dto_class_name == null || dto_class_name.isEmpty()) {
            throw new Exception("<" + node_name + "...\nDTO class is not set");
        }
        String error_context = "<" + node_name + " dto=\"" + dto_class_name + "\" table=\""
                + jaxb_crud.getTable() + "\"...\n";
        try {
            DtoClass jaxb_dto_class = find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            TypeMethod create = jaxb_crud.getCreate();
            TypeMethod read_all = jaxb_crud.getReadAll();
            TypeMethod read = jaxb_crud.getRead();
            TypeMethod update = jaxb_crud.getUpdate();
            TypeMethod delete = jaxb_crud.getDelete();
            if (create == null && read_all == null && read == null && update == null && delete == null) {
                // an empty "<crud/>" means "render all the five methods with default names"
                TypeMethod all = new TypeMethod();
                create = read_all = read = update = delete = all;
            }
            String table_name = refine_table_name(jaxb_dto_class, jaxb_crud.getTable());
            String explicit_pk = jaxb_dto_class.getPk();
            if (create != null) {
                DaoCrudMethodInfo mi = _new_crud_method(jaxb_crud, jaxb_dto_class, error_context, table_name);
                mi.kind = DaoCrudMethodInfo.Kind.CREATE;
                mi.method_name = _crud_method_name(create, "create", dto_class_name, field_names_mode);
                mi.explicit_pk = explicit_pk;
                res.add(mi);
            }
            if (read_all != null) {
                DaoCrudMethodInfo mi = _new_crud_method(jaxb_crud, jaxb_dto_class, error_context, table_name);
                mi.kind = DaoCrudMethodInfo.Kind.READ;
                mi.method_name = _crud_method_name(read_all, "read", dto_class_name + "List", field_names_mode);
                mi.explicit_pk = null; // "read all" has no WHERE
                mi.fetch_list = true;
                res.add(mi);
            }
            if (read != null) {
                DaoCrudMethodInfo mi = _new_crud_method(jaxb_crud, jaxb_dto_class, error_context, table_name);
                mi.kind = DaoCrudMethodInfo.Kind.READ;
                mi.method_name = _crud_method_name(read, "read", dto_class_name, field_names_mode);
                mi.explicit_pk = explicit_pk;
                res.add(mi);
            }
            if (update != null) {
                DaoCrudMethodInfo mi = _new_crud_method(jaxb_crud, jaxb_dto_class, error_context, table_name);
                mi.kind = DaoCrudMethodInfo.Kind.UPDATE;
                mi.method_name = _crud_method_name(update, "update", dto_class_name, field_names_mode);
                mi.explicit_pk = explicit_pk;
                res.add(mi);
            }
            if (delete != null) {
                DaoCrudMethodInfo mi = _new_crud_method(jaxb_crud, jaxb_dto_class, error_context, table_name);
                mi.kind = DaoCrudMethodInfo.Kind.DELETE;
                mi.method_name = _crud_method_name(delete, "delete", dto_class_name, field_names_mode);
                mi.explicit_pk = explicit_pk;
                res.add(mi);
            }
        } catch (Throwable e) {
            throw new Exception(Helpers.get_error_message(error_context, e));
        }
    }

    // what all the five CRUD methods share; 'kind', 'method_name' and 'explicit_pk'
    // are assigned by the caller
    private static DaoCrudMethodInfo _new_crud_method(
            Crud jaxb_crud,
            DtoClass jaxb_dto_class,
            String error_context,
            String table_name) {

        DaoCrudMethodInfo res = new DaoCrudMethodInfo();
        res.error_context = error_context;
        res.dto_class_name = jaxb_crud.getDto();
        res.table_name = table_name;
        res.auto_column = jaxb_dto_class.getAuto();
        res.fetch_generated = jaxb_crud.isFetchGenerated();
        res.fetch_list = false;
        return res;
    }

    private static String _crud_method_name(
            TypeMethod jaxb_method,
            String base,
            String dto_class_name,
            FieldNamesMode field_names_mode) {

        String method_name = jaxb_method.getMethod();
        if (method_name == null || method_name.isEmpty()) {
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index == -1) {
                method_name = base + dto_class_name;
            } else {
                method_name = base + dto_class_name.substring(model_name_end_index + 1);
            }
        }
        return Names.get_method_name(method_name, field_names_mode);
    }

    public static String get_jaxb_node_name(Object jaxb_node) {
        XmlRootElement attr = jaxb_node.getClass().getAnnotation(XmlRootElement.class);
        return attr.name();
    }

    public static DtoClass find_jaxb_dto_class(String dto_class_name, List<DtoClass> jaxb_dto_classes) throws Exception {
        if (dto_class_name == null || dto_class_name.trim().isEmpty()) {
            throw new Exception("Invalid DTO class name: " + dto_class_name);
        }
        for (DtoClass cls : jaxb_dto_classes) {
            String name = cls.getName();
            if (name != null && name.equals(dto_class_name)) {
                return cls;
            }
        }
        throw new Exception("XML element not found <dto-class name=\"" + dto_class_name + "\"");
    }

    public static DaoClass find_jaxb_dao_class(String dao_class_name, List<DaoClass> jaxb_dao_classes) throws Exception {
        if (dao_class_name == null || dao_class_name.trim().isEmpty()) {
            throw new Exception("Invalid DAO class name: " + dao_class_name);
        }
        for (DaoClass cls : jaxb_dao_classes) {
            String name = cls.getName();
            if (name != null && name.equals(dao_class_name)) {
                return cls;
            }
        }
        throw new Exception("XML element not found <dao-class name=\"" + dao_class_name + "\"");
    }

    public static String get_dao_class_name_by_dao_xml_path(List<DaoClass> jaxb_dao_classes, String dao_xml_path) throws Exception {
        if (dao_xml_path == null || dao_xml_path.trim().isEmpty()) {
            throw new Exception("Invalid DAO class ref: " + dao_xml_path);
        }
        Path p = Paths.get(dao_xml_path);
        String file_name = p.getFileName().toString();
        for (DaoClass cls : jaxb_dao_classes) {
            String ref = cls.getRef();
            if (ref != null && ref.equals(file_name)) {
                return cls.getName();
            }
        }
        throw new Exception("XML element not found <dao-class ref=\"" + file_name + "\"");
    }

    public static Set<String> get_pk_col_name_aliases_from_jaxb(String explicit_pk) throws Exception {
        // if PK are specified explicitly, don't use getPrimaryKeys at all
        String[] gen_keys_arr = MethodDeclarations.get_listed_items(explicit_pk, false);
        MethodDeclarations.check_duplicates(gen_keys_arr);
        for (int i = 0; i < gen_keys_arr.length; i++) {
            gen_keys_arr[i] = Names.get_pk_col_name_alias(gen_keys_arr[i].toLowerCase());
        }
        return new HashSet<String>(Arrays.asList(gen_keys_arr));
    }

    static String refine_table_name(DtoClass jaxb_dto_class, String dao_table_name) throws Exception {
        if (dao_table_name == null || dao_table_name.isEmpty()) {
            throw new Exception("'table' is empty");
        }
        if ("*".equals(dao_table_name)) {
            String dto_class_ref = jaxb_dto_class.getRef();
            if (!SqlUtils.is_table_ref(dto_class_ref)) {
                String dto_node_name = JaxbUtils.get_jaxb_node_name(jaxb_dto_class);
                String dto_class_name = jaxb_dto_class.getName();
                throw new Exception(", but 'ref' is not a table: <" + dto_node_name + " name=\"" + dto_class_name +
                        "\" ref=\"" + dto_class_ref + "\"..");
            }
            return dto_class_ref;
        }
        return dao_table_name;
    }
}
