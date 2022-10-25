package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.settings.Macros;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JaxbMacros {

    private final Map<String, String> built_in = new HashMap<String, String>();
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
}

