/*
 * Copyright 2011-2015 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
@MimeRegistration(mimeType = "text/xml", service = CompletionProvider.class)
public class XmlEditorCompletionProvider implements CompletionProvider {

    /*
    
     Google: netbeans platform custom code assistance xml editor
     NetBeans Code Completion Tutorial for the NetBeans Platform
     https://platform.netbeans.org/tutorials/nbm-code-completion.html
     Software or Resource Version Required
     NetBeans IDE version 8.0 or above
     Java Developer Kit (JDK) version 7 or above    
    
     */
    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {

        // System.out.println("queryType: " + queryType);
        // === panedrone: COMPLETION_ALL_QUERY_TYPE occurs when completion list is 
        // displayed and Ctrl+Space is pressed again:
        //
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE
                && queryType != CompletionProvider.COMPLETION_ALL_QUERY_TYPE) {

            return null;
        }

        Document document = jtc.getDocument();

        if (document == null) {

            return null;
        }

        DataObject data_object = NbEditorUtilities.getDataObject(document);

        if (data_object == null) {

            return null;
        }

        final FileObject this_file_object = data_object.getPrimaryFile();

        if (this_file_object == null) {

            return null;
        }

        String doc_name = this_file_object.getNameExt();

        if (!FileSearchHelpers.is_dto_xml(doc_name) && !FileSearchHelpers.is_dao_xml(doc_name)) {

            return null;
        }

        return new AsyncCompletionTask(new AsyncCompletionQuery() {

            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {

                try {

                    XmlEditorUtil checker = new XmlEditorUtil();

                    String attribute_name = checker.verify_state(document, caretOffset);

                    if (attribute_name == null || attribute_name.length() == 0) {

                        return;
                    }

                    int start_offset = checker.get_start_offset() + 1;

                    int end_offset = checker.get_end_offset() + 1;

                    String current_value = checker.get_attribute_value();

                    int filter_len = caretOffset - start_offset;

                    if (filter_len > current_value.length()) {

                        filter_len = current_value.length();

                    } else if (filter_len < 0) { // position before starting "

                        return; // go to finally
                    }

                    String filter;

                    if (filter_len == 0) {

                        filter = "";

                    } else {

                        filter = current_value.substring(0, filter_len);
                    }

                    if (XmlEditorUtil.ATTRIBUTE.REF.equals(attribute_name)) {

                        Settings settings;

                        try {

                            settings = NbpHelpers.load_settings(this_file_object);

                        } catch (Throwable e) {
                            //e.printStackTrace();
                            return;
                        }

                        FileObject root;

                        try {

                            root = NbpPathHelpers.get_root_folder(this_file_object);

                        } catch (Exception ex) {
                            // ex.printStackTrace();
                            return;
                        }

                        FileObject sql_root_folder = root.getFileObject(settings.getFolders().getSql());

                        List<String> rel_path_names = new ArrayList<String>();

                        enum_sql_files(sql_root_folder, sql_root_folder, rel_path_names);

                        for (String rel_path_name : rel_path_names) {

                            boolean add;

                            if (filter.length() == 0) {

                                add = true;

                            } else {

                                add = rel_path_name.startsWith(filter);
                            }

                            if (add) {

                                completionResultSet.addItem(new XmlEditorCompletionItem(rel_path_name, start_offset, end_offset));
                            }
                        }

                    } else {

                        boolean is_dto_attr = XmlEditorUtil.ATTRIBUTE.DTO.equals(attribute_name);
                        boolean is_table_attr = XmlEditorUtil.ATTRIBUTE.TABLE.equals(attribute_name);

                        if (is_dto_attr || is_table_attr) {

                            FileObject folder = this_file_object.getParent();

                            List<DtoClass> dto_classes;

                            try {

                                dto_classes = NbpHelpers.get_dto_classes(folder);

                            } catch (Throwable ex) {
                                // ex.printStackTrace();
                                return;
                            }

                            for (DtoClass dto_class : dto_classes) {

                                String name;

                                boolean add;

                                if (is_dto_attr) {

                                    name = dto_class.getName();

                                    if (filter.length() == 0) {

                                        add = true;

                                    } else {

                                        add = name.startsWith(filter);
                                    }

                                } else if (is_table_attr) {

                                    name = dto_class.getRef();

                                    if (filter.length() == 0) {

                                        add = !name.endsWith(".sql");

                                    } else {

                                        add = name.startsWith(filter) && !name.endsWith(".sql");
                                    }

                                } else {

                                    add = false;

                                    name = null;
                                }

                                if (add) {
                                    // System.out.println(name);
                                    completionResultSet.addItem(new XmlEditorCompletionItem(name, start_offset, end_offset));
                                }
                            }

                        }
                    }

                } finally {

                    completionResultSet.finish(); // it must be called anyway to avoid red banner
                }
            }

        }, jtc);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        // For now, let's return 0. This means that the code 
        // completion box will never appear unless the user 
        // explicitly asks for it.
        return 0;
    }

    private static void enum_sql_files(FileObject root_sql_folder, FileObject current_folder, List<String> rel_path_names) {

        FileObject[] children = current_folder.getChildren();

        for (FileObject c : children) {

            if (c.isFolder()) {

                enum_sql_files(root_sql_folder, c, rel_path_names);

            } else {

                String path = NbpPathHelpers.get_relative_path(root_sql_folder, c);

                rel_path_names.add(path);
            }
        }
    }
}
