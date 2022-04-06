/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.sqldalmaker.common.FileSearchHelpers;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 28.06.12
 * Time: 0:43
 */
public class PsiReferenceDtoClassProvider extends PsiReferenceProvider {

//    private final Set<String> dao_tag_names = new HashSet<String>();

    public PsiReferenceDtoClassProvider() {
//        Collections.addAll(dao_tag_names, IdeaReferenceCompletion.DAO_TAGS_USING_DTO);
//        Collections.addAll(dao_tag_names, IdeaReferenceCompletion.ELEMENT.DTO_CLASS);
    }

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof XmlAttributeValue)) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiElement attr_element = element.getParent();
        if (!(attr_element instanceof XmlAttribute)) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiElement parent_tag = attr_element.getParent();
        if (!(parent_tag instanceof XmlTag)) {
            return PsiReference.EMPTY_ARRAY;
        }
        final PsiFile containing_file = parent_tag.getContainingFile();
        if (containing_file == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        String file_name = containing_file.getName();
        final String attr_name = ((XmlAttribute) attr_element).getName();
        if (FileSearchHelpers.is_dao_xml(file_name)) {
            if (IdeaReferenceCompletion.ATTRIBUTE.DTO.equals(attr_name)) {
//                    final String parent_tag_name = ((XmlTag) parent_tag).getName();
//                    if (dao_tag_names.contains(parent_tag_name)) {
                return new PsiReference[]{new PsiReferenceDtoClass(element/*, element.getTextRange()*/)};
//                    }
            }
        } else if (FileSearchHelpers.is_dto_xml(file_name)) {
            if (IdeaReferenceCompletion.ATTRIBUTE.NAME.equals(attr_name)) {
//                    final String parent_tag_name = ((XmlTag) parent_tag).getName();
//                    if (dao_tag_names.contains(parent_tag_name)) {
                return new PsiReference[]{new PsiReferenceDtoClass(element/*, element.getTextRange()*/)};
//                    }
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}