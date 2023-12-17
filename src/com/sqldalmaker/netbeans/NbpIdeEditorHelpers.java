/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.sdm.ObjectFactory;
import com.sqldalmaker.jaxb.sdm.Sdm;
import com.sqldalmaker.jaxb.settings.Settings;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.netbeans.api.actions.Openable;
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

    public static void open_in_editor_async(final FileObject file) {
        // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                try {
                    // https://blogs.oracle.com/geertjan/entry/open_file_action
                    DataObject obj = DataObject.find(file); // throws DataObjectNotFoundException
                    Openable openable = obj.getLookup().lookup(OpenCookie.class);
                    if (openable == null) {
                        throw new Exception( "Not an openable: " + file.getNameExt());
                    }
                    openable.open();
                } catch (Exception ex) {
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
            ObjectFactory object_factory = new ObjectFactory();
            Sdm root = object_factory.createSdm();
            DtoClass cls_element = object_factory.createDtoClass();
            cls_element.setName(class_name);
            cls_element.setRef(ref);
            root.getDtoClass().add(cls_element);
            Settings sett = NbpHelpers.load_settings(obj);
            String sql_root_folder_abs_path = NbpPathHelpers.get_absolute_dir_path_str(obj, sett.getFolders().getSql());
            SdmUtils.gen_field_wizard_jaxb(sett, con, object_factory, cls_element, sql_root_folder_abs_path);
            open_dto_in_editor_async(object_factory, root);
        } finally {
            con.close();
        }
    }

    public static void open_dao_in_editor_async(ObjectFactory object_factory, DaoClass dao_classes) throws Exception {
        open_dao_in_editor_async(object_factory, dao_classes, "_crud_dao.xml"); // '%' throws URI exception in NB
    }

    public static void open_fk_dao_in_editor_async(ObjectFactory object_factory, DaoClass dao_classes) throws Exception {
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

    public static void open_dto_in_editor_async(ObjectFactory object_factory, Sdm sdm) throws Exception {
        String text = XmlHelpers.get_sdm_xml_text(object_factory, sdm);
        String[] parts = text.split("\\?>");
        text = parts[0] + Const.COMMENT_GENERATED_SDM_XML + parts[1];
        open_text_in_editor_async("_dto.xml", text); // '%' throws URI exception in NB
    }

    private static void open_dao_in_editor_async(ObjectFactory object_factory, DaoClass dao_classes, String file_name) throws Exception {
        String text = XmlHelpers.get_dao_xml_text(object_factory, dao_classes);
        String[] parts = text.split("\\?>");
        text = parts[0] + Const.COMMENT_GENERATED_DAO_XML + parts[1];
        open_text_in_editor_async(file_name, text);
    }
}
