/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.AndFilter;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.ScopeFilter;
import com.intellij.psi.filters.TagNameFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.xml.util.XmlUtil;
import com.sqldalmaker.common.FileSearchHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 26.06.12
 * Time: 22:13
 * <p/>
 * link: https://confluence.jetbrains.com/display/IntelliJIDEA/Reference+Contributor
 */
public class PsiReferenceContributorCommon extends PsiReferenceContributor {

    private class DtoXmlDocElementFilter implements ElementFilter {

        @Override
        public boolean isAcceptable(Object element, @Nullable PsiElement context) {

            if (!(element instanceof XmlAttributeValue)){

                return false;
            }

            XmlAttributeValue v = (XmlAttributeValue) element;

            PsiFile pf = v.getContainingFile();

            if (pf == null) {

                return false;
            }

            VirtualFile vf = pf.getVirtualFile();

            if (vf == null) {

                return false;
            }

            String name = vf.getName();

            return FileSearchHelpers.is_dto_xml(name);
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return false;
        }
    }

    private class DaoXmlDocElementFilter implements ElementFilter {

        @Override
        public boolean isAcceptable(Object element, @Nullable PsiElement context) {

            if (!(element instanceof XmlAttributeValue)) {

                return false;
            }

            XmlAttributeValue v = (XmlAttributeValue) element;

            PsiFile pf = v.getContainingFile();

            if (pf == null) {

                return false;
            }

            VirtualFile vf = pf.getVirtualFile();

            if (vf == null) {

                return false;
            }

            String name = vf.getName();

            return FileSearchHelpers.is_dao_xml(name);
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return false;
        }
    }

    public void registerReferenceProviders(@NotNull final PsiReferenceRegistrar registrar) {

        // based on com.intellij.xml.util.XmlReferenceContributor

        PsiReferenceDtoClassProvider ref_provider_dto_class = new PsiReferenceDtoClassProvider();

        XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{"name"}, new ScopeFilter(
                new AndFilter(
                        new DtoXmlDocElementFilter(),
                        new ParentElementFilter(new TagNameFilter(IdeaReferenceCompletion.ELEMENT.DTO_CLASS), 2)
                )
        ), true, ref_provider_dto_class);

        XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{"dto"}, new ScopeFilter(
                new AndFilter(
                        new DaoXmlDocElementFilter(),
                        new ParentElementFilter(new TagNameFilter(IdeaReferenceCompletion.DAO_TAGS_USING_DTO), 2)
                )
        ), true, ref_provider_dto_class);

        /////////////////////////////////////////////////////////

        PsiReferenceSqlProvider ref_provider_sql_file = new PsiReferenceSqlProvider();

        XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{"ref"}, new ScopeFilter(
                new AndFilter(
                        new DtoXmlDocElementFilter(),
                        new ParentElementFilter(new TagNameFilter(IdeaReferenceCompletion.ELEMENT.DTO_CLASS), 2)
                )
        ), true, ref_provider_sql_file);

        XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{"ref"}, new ScopeFilter(
                new AndFilter(
                        new DaoXmlDocElementFilter(),
                        new ParentElementFilter(new TagNameFilter(IdeaReferenceCompletion.DAO_TAGS_USING_REF), 2)
                )
        ), true, ref_provider_sql_file);
    }
}