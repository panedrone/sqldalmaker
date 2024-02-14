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
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by sqldalmaker@gmail.com
 * on 17.02.2015.
 * <p/>
 * usage of PsiReferenceBase<PsiElement> is based on
 * <a href="https://confluence.jetbrains.com/display/IntelliJIDEA/Reference+Contributor">...</a>
 */
@SuppressWarnings("unchecked")
public class PsiReferenceDtoClass extends PsiReferenceBase<PsiElement> {

    public PsiReferenceDtoClass(PsiElement element) {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        String canonical_text = getCanonicalText().trim();  // @NotNull
        if (canonical_text.isEmpty()) {
            return null;
        }
        final PsiFile containing_file = myElement.getContainingFile();
        if (containing_file == null) {
            return null;
        }
        VirtualFile this_xml_file = IdeaRefUtils.find_virtual_file(containing_file);
        if (this_xml_file == null) {
            return null;
        }
        VirtualFile xml_file_dir = this_xml_file.getParent();
        if (xml_file_dir == null) {
            return null;
        }
        VirtualFile sdm_xml_file = xml_file_dir.findFileByRelativePath(Const.SDM_XML);
        if (sdm_xml_file == null) {
            return null;
        }
        Project project = containing_file.getProject(); // @NotNull
        String this_file_name = containing_file.getName();
        if (Helpers.is_dao_xml(this_file_name)) {
            return IdeaRefUtils.find_dto_class_xml_tag(project, sdm_xml_file, canonical_text);
        } else if (Helpers.is_sdm_xml(this_file_name)) {
            PsiElement parent = myElement.getParent();
            if (parent.getText().startsWith(IdeaRefUtils.ATTRIBUTE.DTO)) {
                return IdeaRefUtils.find_dto_class_xml_tag(project, sdm_xml_file, canonical_text);
            }
            String tag = parent.getParent().getText().replace("<", "").trim();
            if (tag.startsWith(IdeaRefUtils.ELEMENT.DAO_CLASS)) {
                String dao_class_name = canonical_text;
                return IdeaRefUtils.find_dao_class_target_file(project, sdm_xml_file, dao_class_name);
            }
            String dto_class_name = canonical_text;
            return IdeaRefUtils.find_dto_class_target_file(project, sdm_xml_file, dto_class_name);

        }
        return null;
    }

    @NotNull
    public Object @NotNull [] getVariants() {
        // code completion is not implemented for this attribute value yet
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean isSoft() {
        PsiElement parent = myElement.getParent();
        if (parent.getText().startsWith(IdeaRefUtils.ATTRIBUTE.DTO)) {
            return false;
        }
        // defaults: if 'false' then wrong class name is highlighted in red
        PsiFile containing_file = myElement.getContainingFile();
        if (containing_file == null) {
            return true;
        }
        String name = containing_file.getName();
        return Helpers.is_sdm_xml(name);
    }

//      === panedrone: 1) marked as @Experimental 2) suppresses links in idea 2020
//
//    @NotNull
//    // @Override
//    public Collection resolveReference() {
//        // --- panedrone: implementation to compile and work with IDEA 13...2019. @SuppressWarnings("unchecked") is needed before class declaration.
//        return Collections.emptyList();
//    }
}