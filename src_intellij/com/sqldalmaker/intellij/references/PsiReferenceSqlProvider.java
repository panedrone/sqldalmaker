/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 28.06.12
 * Time: 20:49
 */
public class PsiReferenceSqlProvider extends PsiReferenceProvider {

    private final Set<String> dao_tag_names = new HashSet<String>();

    public PsiReferenceSqlProvider() {

        List<String> list = Arrays.asList(IdeaReferenceCompletion.DAO_TAGS_USING_REF);
        dao_tag_names.addAll(list);
        dao_tag_names.add(IdeaReferenceCompletion.ELEMENT.DTO_CLASS);
    }

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {

        if (element instanceof XmlAttributeValue) {
            PsiElement attr_element = element.getParent();
            if (attr_element instanceof XmlAttribute) {
                final String attr_name = ((XmlAttribute) attr_element).getName();
                if (IdeaReferenceCompletion.ATTRIBUTE.REF.equals(attr_name)) {
                    PsiElement parent_tag = attr_element.getParent();
                    if (parent_tag instanceof XmlTag) {
                        final String tag_name = ((XmlTag) parent_tag).getName();
                        if (dao_tag_names.contains(tag_name)) {
                            return new PsiReference[]{new PsiReferenceSql(element/*, element.getTextRange()*/)};
                        }
                    }
                }
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}