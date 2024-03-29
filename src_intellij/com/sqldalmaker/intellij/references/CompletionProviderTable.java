/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.sqldalmaker.cg.SqlUtils;
import com.sqldalmaker.common.Const;
import org.jetbrains.annotations.NotNull;

/**
 * Created by sqldalmaker@gmail.com
 * on 13.10.2015.
 */
public class CompletionProviderTable extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet completionResultSet) {

        XmlAttributeValue attribute_value = PsiTreeUtil.getParentOfType(completionParameters.getPosition(), XmlAttributeValue.class, false);
        if (attribute_value == null) {
            // we are injected, only getContext() returns attribute value
            return;
        }
        PsiElement attr_element = attribute_value.getParent();
        if (!(attr_element instanceof XmlAttribute)) {
            return;
        }
        PsiFile containing_file = attr_element.getContainingFile();
        if (containing_file == null) {
            return;
        }
        VirtualFile this_xml_file = IdeaRefUtils.find_virtual_file(containing_file);
        if (this_xml_file == null) {
            return;
        }
        VirtualFile xml_file_dir = this_xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        // the same folder as prop-file
        VirtualFile sdm_xml_file = xml_file_dir.findFileByRelativePath(Const.SDM_XML);
        if (sdm_xml_file == null) {
            return;
        }
        Project project = containing_file.getProject();
//        if (project == null) {
//            return null; // ---- it is @NotNull
//        }
        PsiElement res = PsiManager.getInstance(project).findFile(sdm_xml_file);// @Nullable
        if (!(res instanceof XmlFile)) {
            return;
        }
        XmlFile xml_file = (XmlFile) res;
        XmlTag root;
        try {
            root = xml_file.getRootTag();
        } catch (Throwable th) {
            return;
        }
        if (root == null) {
            return;
        }
        PsiElement[] tags;
        try {
            tags = root.getChildren(); // notnull;
        } catch (Throwable th) {
            return;
        }
        for (PsiElement el : tags) {
            if (el instanceof XmlTag) {
                XmlTag t = (XmlTag) el;
                if (IdeaRefUtils.ELEMENT.DTO_CLASS.equals(t.getName())) {
                    XmlAttribute a = t.getAttribute(IdeaRefUtils.ATTRIBUTE.REF);
                    if (a != null) {
                        String ref_value = a.getValue();
                        if (SqlUtils.is_table_ref(ref_value)) {
                            completionResultSet.addElement(LookupElementBuilder.create(ref_value));
                        }
                    }
                }
            }
        }
    }
}