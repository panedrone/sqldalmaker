/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.FieldInfo;
import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.dto.ObjectFactory;
import com.sqldalmaker.jaxb.settings.Settings;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.RequestProcessor;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpIdeEditorHelpers {

    private static void open_in_editor_async(final FileObject file) {

        // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
        RequestProcessor.getDefault().post(new Runnable() {

            @Override
            public void run() {

                try {
                    // https://blogs.oracle.com/geertjan/entry/open_file_action

                    DataObject.find(file).getLookup().lookup(OpenCookie.class).open();

                } catch (DataObjectNotFoundException ex) {
                    // ex.printStackTrace();
                    NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
                }
            }
        });
    }

    public static void open_metaprogram_file_async(SdmDataObject obj, String file_name) {

        FileObject folder = obj.getPrimaryFile().getParent();

        open_metaprogram_file_in_editor_async(folder, file_name);
    }

    public static void open_metaprogram_file_in_editor_async(FileObject folder, String file_name) {

        FileObject file = folder.getFileObject(file_name);

        if (file == null) {

            NbpIdeMessageHelpers.show_error_in_ui_thread("File not found: " + file_name);

            return;
        }

        open_in_editor_async(file);
    }

    public static void open_project_file_in_editor_async(FileObject file, String rel_path) throws Exception {

        FileObject root = NbpPathHelpers.get_root_folder(file);

        FileObject res = root.getFileObject(rel_path);

        if (res == null || res.isFolder()) {

            throw new Exception("File not found: " + rel_path);
        }

        open_in_editor_async(res);
    }

    public static void open_project_file_in_editor_async(SdmDataObject obj, String rel_path) throws Exception {

        FileObject file = obj.getPrimaryFile();

        open_project_file_in_editor_async(file, rel_path);
    }

    public static void open_resource_file_in_editor_async(String res_title, String res_name) {

        open_resource_file_in_editor_async(res_title, "resources", res_name);
    }

    public static void open_resource_file_in_editor_async(String res_title, String res_path, String res_name) {

        try {

            String file_content = NbpHelpers.read_from_jar_file(res_path, res_name);

            // https://dzone.com/articles/in-memory-virtual-filesystems
            FileSystem fs = FileUtil.createMemoryFileSystem();

            FileObject root = fs.getRoot();

            FileObject file = root.getFileObject(res_title);

            if (file == null) {

                file = root.createData(res_title);
            }

            NbpHelpers.write_file_content(file, file_content);

            open_in_editor_async(file);

        } catch (Throwable ex) {

            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }

    public static void create_dao_xml(SdmDataObject obj, String name) {

        try {

            String output_dir_rel_path = NbpPathHelpers.get_folder_relative_path(obj);

            String file_content = NbpHelpers.read_from_jar_file(Const.EMPTY_DAO_XML);

            NbpHelpers.save_text_to_file(obj, output_dir_rel_path, name, file_content);

            open_metaprogram_file_async(obj, name);

        } catch (Throwable ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }

    public static void generate_tmp_field_tags_and_open_in_editor_async(SdmDataObject obj, String class_name, String ref) throws SQLException, Exception {

        Connection con = NbpHelpers.get_connection(obj);

        try {

            com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

            DtoClasses root = object_factory.createDtoClasses();

            DtoClass cls_element = object_factory.createDtoClass();
            cls_element.setName(class_name);
            cls_element.setRef(ref);
            root.getDtoClass().add(cls_element);

            Settings sett = NbpHelpers.load_settings(obj);

            String sql_root_folder_abs_path = NbpPathHelpers.get_absolute_dir_path_str(obj, sett.getFolders().getSql());

            gen_tmp_field_tags(con, object_factory, cls_element, sql_root_folder_abs_path);

            open_dto_in_editor_async(object_factory, root, true);

        } finally {

            con.close();
        }
    }

    private static void gen_tmp_field_tags(Connection con, ObjectFactory object_factory, DtoClass dto_class, String sql_root_abs_path) throws Exception {

        DbUtils db_utils= new DbUtils(con, FieldNamesMode.AS_IS, null);

        String jdbc_sql = db_utils.jdbc_sql_by_ref_query(dto_class.getRef(), sql_root_abs_path);

        ArrayList<FieldInfo> fields = new ArrayList<FieldInfo>();

        db_utils.get_dto_field_info(jdbc_sql, dto_class, fields);

        for (FieldInfo f : fields) {

            DtoClass.Field df = object_factory.createDtoClassField();
            df.setColumn(f.getColumnName());
            df.setJavaType(f.getType());
            dto_class.getField().add(df);
        }
    }

    public static void open_dto_in_editor_async(com.sqldalmaker.jaxb.dto.ObjectFactory object_factory, DtoClasses dto_classes, boolean remove_java_lang) throws Exception {

        String text = XmlHelpers.get_dto_xml_text(object_factory, dto_classes, remove_java_lang);

        open_text_in_editor_async("_dto.xml", text); // '%' throws URI exception in NB
    }

    private static void open_dao_in_editor_async(com.sqldalmaker.jaxb.dao.ObjectFactory object_factory, DaoClass dao_classes, String file_name) throws Exception {

        String text = XmlHelpers.get_dao_xml_text(object_factory, dao_classes, true);

        open_text_in_editor_async(file_name, text);
    }

    public static void open_dao_in_editor_async(com.sqldalmaker.jaxb.dao.ObjectFactory object_factory, DaoClass dao_classes) throws Exception {

        open_dao_in_editor_async(object_factory, dao_classes, "_crud_dao.xml"); // '%' throws URI exception in NB
    }

    public static void open_fk_dao_in_editor_async(com.sqldalmaker.jaxb.dao.ObjectFactory object_factory, DaoClass dao_classes) throws Exception {

        open_dao_in_editor_async(object_factory, dao_classes, "_fk_dao.xml"); // '%' throws URI exception in NB
    }

    public static void open_text_in_editor_async(String title, String text) throws IOException {

        FileSystem fs = FileUtil.createMemoryFileSystem();

        FileObject root = fs.getRoot();

        FileObject file = root.getFileObject(title);

        if (file == null) {

            file = root.createData(title);
        }

        NbpHelpers.write_file_content(file, text);

        open_in_editor_async(file);
    }
}
