/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import com.sqldalmaker.cg.Helpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * usage of PsiReferenceBase<PsiElement> is based on
 * https://confluence.jetbrains.com/display/IntelliJIDEA/Reference+Contributor
 * <p>
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 */
public class PsiReferenceExternalDaoXml extends PsiReferenceBase<PsiElement> {

    public PsiReferenceExternalDaoXml(PsiElement element) {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PsiFile containing_file = myElement.getContainingFile();
        if (containing_file == null) {
            return null;
        }
        String containing_file_name = containing_file.getName();
        if (!Helpers.is_sdm_xml(containing_file_name)) {
            return null;
        }
        VirtualFile this_sdm_xml_file = IdeaRefUtils.find_virtual_file(containing_file);
        if (this_sdm_xml_file == null) {
            return null;
        }
        VirtualFile this_sdm_xml_folder = this_sdm_xml_file.getParent();
        if (this_sdm_xml_folder == null) {
            return null;
        }
        String canonical_text = getCanonicalText(); // @NotNull
        String external_dao_xml_file_path = canonical_text;
        VirtualFile external_dao_xml_file = this_sdm_xml_folder.findFileByRelativePath(external_dao_xml_file_path);
        if (external_dao_xml_file == null) {
            return null;
        }
        Project project = containing_file.getProject(); // @NotNull
        // http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview
        PsiElement res = PsiManager.getInstance(project).findFile(external_dao_xml_file); // @Nullable;
        if (res == null) {
            return null; // just to debug
        }
        return res;
    }

    @Override
    public Object @NotNull [] getVariants() {
        // code completion is implemented with CompletionProvider API
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean isSoft() {
        // defaults: if 'false' then wrong value is highlighted in red
//        PsiFile containing_file = myElement.getContainingFile();
//        if (containing_file == null) {
//            return false;
//        }
//        String canonical_text = getCanonicalText(); // @NotNull
//        String file_name = containing_file.getName();
//        if (canonical_text.trim().isEmpty()) {
//            return true; // ref is empty
//        }
//        if (Helpers.is_sdm_xml(file_name)) {
//            if (Helpers.is_dao_xml(canonical_text)) {
//                return false; // not soft
//            }
//            return true;
//        } else if (Helpers.is_dao_xml(file_name)) {
//            return false;
//        }
        return false;
    }

//      === panedrone: 1) marked as @Experimental 2) supresses links in idea 2020

//    @SuppressWarnings("rawtypes")
//    @NotNull
//    @Override
//    public Collection resolveReference() {
//        // --- panedrone: implementation to compile and work with IDEA 13...2019. @SuppressWarnings("unchecked") is needed before class declaration.
//        return Collections.emptyList();
//    }
}