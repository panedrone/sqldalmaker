/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.TargetLangUtils;
import com.sqldalmaker.intellij.ui.IdeaEditorHelpers;
import com.sqldalmaker.intellij.ui.IdeaHelpers;
import com.sqldalmaker.intellij.ui.IdeaTargetLanguageHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 28.06.12
 * Time: 14:46
 */
public class IdeaReferenceCompletion {

    public static final String[] DAO_TAGS_USING_DTO = new String[]{"crud", "crud-auto", "query-dto", "query-dto-list"};
    public static final String[] DAO_TAGS_USING_REF = new String[]{"query", "query-list", "query-dto", "query-dto-list", "exec-dml"};

    public static class ATTRIBUTE {
        public static final String DTO = "dto";
        public static final String REF = "ref";
        public static final String NAME = "name";
        public static final String TABLE = "table";
    }

    public static class ELEMENT {
        public static final String DTO_CLASS = "dto-class";
        public static final String DAO_CLASS = "dao-class";
    }

    public static @Nullable
    VirtualFile find_virtual_file(@NotNull PsiFile psi_file) {
        VirtualFile res = psi_file.getVirtualFile();
        if (res == null) { // ---: res == null happens during code completion
            PsiFile original_file = psi_file.getOriginalFile(); // @NotNull
            res = original_file.getVirtualFile();
            if (res == null) {
                return null;
            }
        }
        return res;
    }

    public static @Nullable
    PsiElement find_dto_class_xml_tag(@NotNull Project project,
                                      @NotNull VirtualFile dto_xml_file,
                                      @NotNull String dto_class_name) {

        PsiElement res = PsiManager.getInstance(project).findFile(dto_xml_file);// @Nullable
        if (!(res instanceof XmlFile)) {
            return null;
        }
        XmlFile xml_file = (XmlFile) res;
        XmlTag root = xml_file.getRootTag(); // nullable
        if (root == null) {
            return null;
        }
        PsiElement[] tags = root.getChildren(); // notnull;
        for (PsiElement el : tags) {
            if (!(el instanceof XmlTag)) {
                continue;
            }
            XmlTag t = (XmlTag) el;
            if (!t.getName().equals(ELEMENT.DTO_CLASS)) {
                continue;
            }
            XmlAttribute a = t.getAttribute(ATTRIBUTE.NAME);
            if (a == null) {
                continue;
            }
            String v = a.getValue();
            if (v == null || v.isEmpty()) {
                continue;
            }
            if (dto_class_name.equals(v)) {
                return el;
            }
        }
        return null;
    }

    public static @Nullable
    PsiElement find_dto_class_target_file(@NotNull Project project,
                                          @NotNull VirtualFile dto_xml_file,
                                          @NotNull String dto_class_name) {

        VirtualFile xml_file_dir = dto_xml_file.getParent();
        if (xml_file_dir == null) {
            return null;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.size() != 1) {
            return null;
        }
        VirtualFile root_file = root_files.get(0);
        Settings settings;
        try {
            settings = IdeaHelpers.load_settings(root_file);
        } catch (Exception e) {
            return null;
        }
        String target_folder_abs_path;
        try {
            target_folder_abs_path = IdeaTargetLanguageHelpers.get_target_folder_abs_path(project, root_file, settings, settings.getDto().getScope());
        } catch (Exception e) {
            return null;
        }
        String target_file_name;
        try {
            target_file_name = TargetLangUtils.file_name_from_class_name(root_file.getName(), dto_class_name);
        } catch (Exception e) {
            return null;
        }
        String target_file_abs_path = Helpers.concat_path(target_folder_abs_path, target_file_name);
        String rel_path;
        try {
            rel_path = IdeaHelpers.get_relative_path(project, target_file_abs_path);
        } catch (Exception e) {
            return null;
        }
        VirtualFile project_dir;
        try {
            project_dir = IdeaHelpers.get_project_base_dir(project);
        } catch (Exception e) {
            return null;
        }
        VirtualFile target_file;
        try {
            target_file = IdeaEditorHelpers.find_case_sensitive(project_dir, rel_path);
        } catch (Exception e) {
            return null;
        }
        PsiElement res = PsiManager.getInstance(project).findFile(target_file);// @Nullable
        return res;
    }
}