/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 28.06.12
 * Time: 14:46
 */
public class IdeaReferenceCompletion {

    public static final String[] DAO_TAGS_USING_DTO = new String[]{"crud", "crud-auto", "query-dto", "query-dto-list"};
    public static final String[] DAO_TAGS_USING_REF = new String[]{"query", "query-list", "query-dto", "query-dto-list", "exec-dml"};

    public class ATTRIBUTE {

        public static final String DTO = "dto";
        public static final String REF = "ref";
        public static final String NAME = "name";
        public static final String TABLE = "table";
    }

    public class ELEMENT {

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

        XmlTag root;

        try {

            root = xml_file.getRootTag();

        } catch (Throwable th) {

            return null;
        }

        if (root == null) {

            return null;
        }

        PsiElement[] tags;

        try {

            tags = root.getChildren(); // notnull;

        } catch (Throwable th) {

            return null;
        }

        for (PsiElement el : tags) {

            if (el instanceof XmlTag) {

                XmlTag t = (XmlTag) el;

                if (t.getName().equals(ELEMENT.DTO_CLASS)) {

                    XmlAttribute a = t.getAttribute(ATTRIBUTE.NAME);

                    if (a != null) {

                        String v = a.getValue();

                        if (v != null && !v.isEmpty()) {

                            if (dto_class_name.equals(v)) {

                                return el;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}