/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 14.02.24
 * Time: 14:40
 */
public class PsiReferenceExternalDaoXmllProvider extends PsiReferenceProvider {

    private final Set<String> dao_tag_names = new HashSet<String>();

    public PsiReferenceExternalDaoXmllProvider() {
        dao_tag_names.add(IdeaRefUtils.ELEMENT.DAO_CLASS);
    }

    @Override
    public PsiReference @NotNull [] getReferencesByElement(
            @NotNull PsiElement element,
            @NotNull ProcessingContext context) {

        if (XmlPsi.is_attribute_value(element)) {
            PsiElement attr_element = element.getParent();
            if (XmlPsi.is_attribute(attr_element)) {
                final String attr_name = XmlPsi.name_of(attr_element);
                if (IdeaRefUtils.ATTRIBUTE.REF.equals(attr_name)) {
                    PsiElement parent_tag = attr_element.getParent();
                    if (parent_tag != null) {
                        final String tag_name = XmlPsi.name_of(parent_tag);
                        if (dao_tag_names.contains(tag_name)) {
                            return new PsiReference[]{new PsiReferenceExternalDaoXml(element/*, element.getTextRange()*/)};
                        }
                    }
                }
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}