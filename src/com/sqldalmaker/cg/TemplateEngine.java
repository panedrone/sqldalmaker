/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.SimpleNode;

/**
 * @author sqldalmaker@gmail.com
 */
public class TemplateEngine {

    private final Template template;

    public TemplateEngine(String vm_path, boolean is_file_system) throws Exception {

        String template_text;
        if (is_file_system) {
            template_text = Helpers.load_text_from_file(vm_path);
        } else {
            template_text = Helpers.read_from_jar_file_2(vm_path);
        }
        // Velocity loads ResourceManager in this way:
        // ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // return Class.forName(clazz, true, loader);
        // Default class loader in Eclipse is something like
        // 'org.eclipse.osgi.internal.framework.ContextFinder@1e9f32f9'
        // It may use velocity-1.5.jar from Eclipse bundles
        // instead of velocity-1.7-dep.jar from my plug-in.
        // Result is an exception like
        // "ResourceManagerImpl does not implement...".
        // Workaround is proposed here:
        // https://github.com/whitesource/whitesource-bamboo-agent/issues/9
        // Fortunately, section Context Class Loader in Context Class Loader
        // Enhancements
        // describes an easy workaround
        // -->
        // http://wiki.eclipse.org/index.php/Context_Class_Loader_Enhancements#Context_Class_Loader_2
        // ====== REQUIRED AFTER VELOCITY JAR IS PACKED TO ECLIPSE PLUGIN JAR
        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(this.getClass().getClassLoader());
        try {
            // How to use String as Velocity Template?
            // http://stackoverflow.com/questions/1432468/how-to-use-string-as-velocity-template
            // In this case, I share the same instance with JetBrains:
            //
            // RuntimeServices runtimeServices =
            // RuntimeSingleton.getRuntimeServices();
            //
            // It is not correct to change their properties.
            // I create new instance all the time instead to avoid problems of
            // caching.
            RuntimeServices runtime_services = new RuntimeInstance();
            // // VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL is responsible for global
            // // caching of macros by name
            // //
            // http://stackoverflow.com/questions/5567742/how-to-edit-a-velocimacro-without-restarting-velocity
            // // Seems like it cannot be changed after the first initialization
            // of
            // // RuntimeServices
            // runtimeServices.setProperty(
            // RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, true);
            //
            // ========= REQUIRED FOR INTELLIJ PLUG-IN ==============
            runtime_services.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                    org.apache.velocity.runtime.log.NullLogChute.class.getName());
            // {
            // // properties to prevent sharing macros between vm templates for
            // different languages.
            // // they must be set before runtimeServices.parse.
            //
            // // http://jira.xwiki.org/browse/XWIKI-4625
            //
            // runtimeServices.setProperty("string.resource.loader.class",
            // org.apache.velocity.runtime.resource.loader.StringResourceLoader.class.getName());
            // runtimeServices.setProperty("string.resource.loader.cache",
            // Boolean.FALSE.toString());
            // }
            StringReader reader = new StringReader(template_text);
            SimpleNode node = runtime_services.parse(reader, vm_path);
            template = new Template();
            template.setRuntimeServices(runtime_services);
            template.setData(node);
            template.initDocument();
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    public void merge(Map<String, Object> values, StringWriter sw) {

        VelocityContext context = new VelocityContext();
        for (String key : values.keySet()) {
            context.put(key, values.get(key));
        }
        template.merge(context, sw);
    }
}
