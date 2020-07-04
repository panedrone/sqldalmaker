/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.Const;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpPathHelpers {

    /**
     * Project folder is a folder that contains nbproject and other NetBeans
     * stuff.
     *
     * Situation is possible when source folder is outside of project folder.
     *
     * @param fo may be file or folder
     * @return never returns null
     * @throws java.lang.Exception
     * 
     */
    public static FileObject get_root_folder(FileObject fo) throws Exception {
        Project project_fo = get_project(fo);
        if (project_fo == null) {
            throw new Exception("Not in project: " + fo.getNameExt());
        }
        FileObject curr_fo = fo;
        int count = 0;
        while (true) {
            FileObject tmp = curr_fo.getParent(); // get parent goes through file system, not through project tree
            if (tmp == null) {
                throw new Exception("Cannot detect parent for " + fo.getNameExt());
            }
            if (!tmp.isFolder()) {
                throw new Exception("Cannot detect parent for " + fo.getNameExt());
            }
            // returns null if tmp is file-system file located outside project source folder
            Project tmp_project_fo = get_project(tmp);
            if (tmp_project_fo != project_fo) {
                return curr_fo;
            }
            curr_fo = tmp;
            count++;
            if (count > 2000) {
                throw new Exception("The path is too long for " + fo.getNameExt());
            }
        }
    }

    public static String get_root_folder_abs_path(FileObject fo) throws Exception {
        FileObject root = get_root_folder(fo);
        return root.getPath();
    }

    public static Project get_project(FileObject primary_file) {
        /* Google: netbeans get project folder
         ->
         https://code.google.com/p/show-path-in-title-netbeans-module/
         ->
         https://code.google.com/p/show-path-in-title-netbeans-module/source/browse/src/de/markiewb/netbeans/plugin/showpathintitle/PathUtil.java
         */
        Project project = FileOwnerQuery.getOwner(primary_file);
        return project;
    }

    public static String get_absolute_dir_path_str(SdmDataObject obj, String rel_path) throws Exception {
        String pp = get_root_folder_abs_path(obj.getPrimaryFile());
        return pp + "/" + rel_path;
    }

    public static String get_dto_xml_abs_path(SdmDataObject obj) {
        String folder = obj.getPrimaryFile().getParent().getPath();
        return folder + "/" + Const.DTO_XML;
    }

    public static String get_dto_xsd_abs_path(SdmDataObject obj) {
        String folder = obj.getPrimaryFile().getParent().getPath();
        return folder + "/" + Const.DTO_XSD;
    }

    public static String get_path_relative_to_root_folder(SdmDataObject obj) throws Exception {
        FileObject file = obj.getPrimaryFile();
        return NbpPathHelpers.get_path_relative_to_root_folder(file);
    }

    private static String get_path_relative_to_root_folder(FileObject file) throws Exception {
        String file_abs_path = file.getPath();
        String root_abs_path = get_root_folder(file).getPath();
        String res = file_abs_path.substring(root_abs_path.length() + 1);
        return res;
    }

    public static String get_relative_path(FileObject folder, FileObject file) {
        // http://bits.netbeans.org/7.4/javadoc/org-openide-filesystems/org/openide/filesystems/FileUtil.html
        String res = FileUtil.getRelativePath(folder, file);
        return res;
    }

    public static String get_folder_relative_path(SdmDataObject obj) throws Exception {
        FileObject folder = obj.getPrimaryFile().getParent();
        String root_abs_path = get_root_folder_abs_path(folder);
        String folder_path = folder.getPath();
        String res = folder_path.substring(root_abs_path.length() + 1);
        return res;
    }

    public static String get_metaprogram_abs_path(SdmDataObject obj) {
        FileObject folder = obj.getPrimaryFile().getParent();
        return folder.getPath();
    }
}
