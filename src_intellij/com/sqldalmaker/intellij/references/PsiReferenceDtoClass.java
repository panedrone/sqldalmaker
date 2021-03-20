/*
 * Copyright 2011-2021 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: http://sqldalmaker.sourceforge.net
 */
package com.sqldalmaker.intellij.references;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import org.jetbrains.annotations.Nullable;

/**
 * Created by sqldalmaker@gmail.com
 * on 17.02.2015.
 * <p/>
 * usage of PsiReferenceBase<PsiElement> is based on
 * https://confluence.jetbrains.com/display/IntelliJIDEA/Reference+Contributor
 */
@SuppressWarnings("unchecked")
public class PsiReferenceDtoClass extends PsiReferenceBase<PsiElement> {

    public PsiReferenceDtoClass(PsiElement element) {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        String canonical_text = getCanonicalText();  // @NotNull
        if (canonical_text.trim().length() == 0) {
            return null;
        }
        final PsiFile containing_file = myElement.getContainingFile();
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
        VirtualFile dto_xml_file = xml_file_dir.findFileByRelativePath(Const.DTO_XML);
        if (dto_xml_file == null) {
            return null;
        }
        Project project = containing_file.getProject(); // @NotNull
        return IdeaReferenceCompletion.find_dto_class_xml_tag(project, dto_xml_file, canonical_text);
    }

//    @NotNull
//    @Override
//    public Object[] getVariants() {
//        // code completion is not implemented for this attribute value yet
//        return ArrayUtil.EMPTY_OBJECT_ARRAY;
//    }

    @Override
    public boolean isSoft() {
        // defaults: if 'false' then wrong class name is highlighted in red
        PsiFile containing_file = myElement.getContainingFile();
        if (containing_file == null) {
            return true;
        }
        String name = containing_file.getName();
        return FileSearchHelpers.is_dto_xml(name);
    }

//      === panedrone: 1) marked as @Experimental 2) supresses links in idea 2020
//
//    @NotNull
//    // @Override
//    public Collection resolveReference() {
//        // --- panedrone: implementation to compile and work with IDEA 13...2019. @SuppressWarnings("unchecked") is needed before class declaration.
//        return Collections.emptyList();
//    }
}