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
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.sqldalmaker.cg.Helpers;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sqldalmaker@gmail.com
 * on 14.02.2024.
 */
public class CompletionProviderExternalDaoXml extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull ProcessingContext context,
            @NotNull CompletionResultSet result) {

        XmlAttributeValue attribute_value = PsiTreeUtil.getParentOfType(parameters.getPosition(), XmlAttributeValue.class, false);
        if (attribute_value == null) {
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
        ArrayList<String> res = new ArrayList<String>();
        String sdm_xml_path = xml_file_dir.getPath();
        find_dao_xml_files_recursive(xml_file_dir, res);
        for (String s : res) {
            s = s.substring(sdm_xml_path.length() + 1);
            result.addElement(LookupElementBuilder.create(s));
        }
    }

    @SuppressWarnings("rawtypes")
    private void find_dao_xml_files_recursive(VirtualFile base_dir, final List<String> res) {
        VfsUtilCore.visitChildrenRecursively(base_dir, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    String folder = file.getParent().getPath();
                    if (base_dir.getPath().equals(folder)) {
                        String name = file.getName();
                        if (Helpers.is_dao_xml(name)) {
                            String path = file.getPath();
                            res.add(path);
                        }
                    }
                }
                return true;
            }
        });
    }
}