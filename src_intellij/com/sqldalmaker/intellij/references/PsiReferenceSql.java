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
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.SqlUtils;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.intellij.ui.IdeaHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * usage of PsiReferenceBase<PsiElement> is based on
 * https://confluence.jetbrains.com/display/IntelliJIDEA/Reference+Contributor
 * <p>
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 */
@SuppressWarnings("unchecked")
public class PsiReferenceSql extends PsiReferenceBase<PsiElement> {

    public PsiReferenceSql(PsiElement element) {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        String canonical_text = getCanonicalText();

        if (canonical_text.length() == 0) {

            return null;
        }

        if (!SqlUtils.is_sql_file_ref(canonical_text)) {

            return null;
        }

        PsiFile containing_file = myElement.getContainingFile();

        if (containing_file == null) {

            return null;
        }

        VirtualFile this_xml_file = IdeaReferenceCompletion.find_virtual_file(containing_file);

        if (this_xml_file == null) {

            return null;
        }

        VirtualFile xml_file_dir = this_xml_file.getParent();

        if (xml_file_dir == null) {

            return null;
        }

        Project project = containing_file.getProject(); // @NotNull

        VirtualFile project_dir;

        try {

            project_dir = IdeaHelpers.get_project_base_dir(project);

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }

        String sql_root_rel_path;

        try {

            final Settings settings = SdmUtils.load_settings(xml_file_dir.getPath());

            sql_root_rel_path = settings.getFolders().getSql();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }

        String rel_path = sql_root_rel_path + "/" + canonical_text;

        VirtualFile sql_file = project_dir.findFileByRelativePath(rel_path);

        if (sql_file == null) {

            return null;
        }

        // http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview
        //
        return PsiManager.getInstance(project).findFile(sql_file); // @Nullable;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        // code completion is implemented with CompletionProvider API
        //
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean isSoft() {

        // defaults: if 'false' then wrong value is highlighted in red

        PsiFile containing_file = myElement.getContainingFile();

        if (containing_file == null) {

            return false;
        }

        String canonical_text = getCanonicalText(); // @NotNull

        String name = containing_file.getName();

        if (FileSearchHelpers.is_dto_xml(name)) {

            if (/*canonical_text == null ||*/ canonical_text.trim().length() == 0) {

                return true; // ref is empty
            }
            // Allowed in DTO: empty string, table name, path to sql-file
            // only path to sql-file must be highlighted in red if invalid
            //
            if (SqlUtils.is_table_ref(canonical_text)) {

                return true;
            }

            // if stored function returns table, then meta-data about columns of this table
            // may be obtained using 'ref' like 'select * from get_test_table_by_rating(?)'

            if (SqlUtils.is_stored_func_call_shortcut(canonical_text)) {

                return true;
            }

        } else if (FileSearchHelpers.is_dao_xml(name)) {

            // Allowed in DAO: ONLY path to sql-file. So, everything must be checked
            //
            if (SqlUtils.is_sql_shortcut_ref(canonical_text)) {

                return true;
            }

            if (SqlUtils.is_jdbc_stored_proc_call(canonical_text)) {

                return true;
            }

            if (SqlUtils.is_stored_proc_call_shortcut(canonical_text)) {

                return true;
            }

            if (SqlUtils.is_stored_func_call_shortcut(canonical_text)) {

                return true;
            }

            return false;
        }

        return false;
    }

    @SuppressWarnings("rawtypes")
    @NotNull
    @Override
    public Collection resolveReference() {

        // --- panedrone: implementation to compile and work with IDEA 13...2019. @SuppressWarnings("unchecked") is needed before class declaration.

        return Collections.emptyList();
    }
}