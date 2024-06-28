/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.settings.Macros;

import java.io.StringWriter;
import java.util.*;

/*
 * 06.04.2023 02:56 1.281
 * 25.10.2022 09:26
 *
 */
class JaxbMacros {

    private final Map<String, String> custom = new HashMap<String, String>();
    private final Map<String, Macros.Macro> custom_vm = new HashMap<String, Macros.Macro>();

    public JaxbMacros(Macros jaxb_macros) {
        if (jaxb_macros == null) {
            return;
        }
        for (Macros.Macro m : jaxb_macros.getMacro()) {
            String name = m.getName();
            String value = m.getValue();
            if (value == null) { // it is optional
                if (m.getVm() != null || m.getVmXml() != null) {
                    custom_vm.put(name, m);
                }
            } else {
                if (!value.equals("=built-in=")) {
                    custom.put(name, value);
                }
            }
        }
    }

    private static String _substitute_type_params(String type_name) {
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
        return type_name;
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

    public String parse_target_type_name(final String target_type_name, FieldInfo base_fi) throws Exception {
        String parsed = _parse_target_type_recursive(0, target_type_name, base_fi);
        parsed = _substitute_built_in_macros(parsed, base_fi);
        String no_params = _substitute_type_params(parsed);
        return no_params;
    }

    private String _parse_target_type_recursive(int depth, String target_type_name, FieldInfo base_fi) throws Exception {
        if (depth > 100) {
            throw new Exception("depth overflow: " + target_type_name);
        }
        String res = target_type_name;
        boolean found = false;
        for (String m_name : custom.keySet()) {
            if (res.contains(m_name)) { // single replacement in one pass
                res = res.replace(m_name, custom.get(m_name));
                found = true;
            }
        }
        if (!found) {
            String tmp = _substitute_custom_vm_macros(base_fi, res);
            if (tmp != null) {
                res = tmp; // single replacement in one pass
                found = true;
            }
        }
        if (!found) {
            return res;
        }
        return _parse_target_type_recursive(depth + 1, res, base_fi);
    }

    private String _substitute_custom_vm_macros(FieldInfo fi, String target_type_name) throws Exception {
        for (String m_name : custom_vm.keySet()) {
            if (!target_type_name.contains(m_name)) {
                continue;
            }
            String vm_template;
            Macros.Macro vm_macro = custom_vm.get(m_name);
            if (vm_macro.getVm() != null) {
                vm_template = vm_macro.getVm().trim();
            } else if (vm_macro.getVmXml() != null) {
                vm_template = Xml2Vm.parse(vm_macro.getVmXml());
            } else {
                throw new Exception("Expected <vm> or <vm-xml> in " + m_name);
            }
            TemplateEngine te = new TemplateEngine(vm_template, m_name);
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("fi", fi);
            StringWriter sw = new StringWriter();
            te.merge(values, sw);
            String value = sw.toString();
            target_type_name = target_type_name.replace(m_name, value);
            return target_type_name; // single replacement in one pass
        }
        return null;
    }

    private interface IMacro {
        String exec(FieldInfo base_fi);
    }

    private static Map<String, IMacro> _get_built_in_macros() {
        Map<String, IMacro> macros = new HashMap<String, IMacro>();
        macros.put("${lower_snake_case(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo base_fi) {
                return Helpers.camel_case_to_lower_snake_case(base_fi.getColumnName());
            }
        });
        macros.put("${camelCase(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo base_fi) {
                return Helpers.lower_camel_case(base_fi.getColumnName());
            }
        });
        macros.put("${TitleCase(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo base_fi) {
                return Helpers.title_case(base_fi.getColumnName());
            }
        });
        macros.put("${kebab-case(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo base_fi) {
                return Helpers.to_kebab_case(base_fi.getColumnName());
            }
        });
        macros.put("${column}", new IMacro() {
            @Override
            public String exec(FieldInfo base_fi) {
                return base_fi.getColumnName();
            }
        });
        return macros;
    }

    private static String _substitute_built_in_macros(String curr_type, FieldInfo base_fi) {
        Map<String, IMacro> built_in_macros = _get_built_in_macros();
        if (curr_type.isEmpty()) {
            return curr_type;
        }
        for (String name : built_in_macros.keySet()) {
            if (!curr_type.contains(name)) {
                continue;
            }
            String value = built_in_macros.get(name).exec(base_fi);
            curr_type = curr_type.replace(name, value);
        }
        return curr_type;
    }
}

