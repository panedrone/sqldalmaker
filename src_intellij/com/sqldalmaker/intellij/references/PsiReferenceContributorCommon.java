/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.util.ProcessingContext;
import com.sqldalmaker.cg.Helpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/*
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 26.06.12
 * Time: 22:13
 * <p/>
 * link: https://confluence.jetbrains.com/display/IntelliJIDEA/Reference+Contributor
 */
public class PsiReferenceContributorCommon extends PsiReferenceContributor {

    private static boolean in_sdm_xml(PsiElement e) {
        String name = file_name(e);
        return name != null && Helpers.is_sdm_xml(name);
    }

    private static boolean in_sdm_or_dao_xml(PsiElement e) {
        String name = file_name(e);
        return name != null && (Helpers.is_sdm_xml(name) || Helpers.is_dao_xml(name));
    }

    private static @Nullable String file_name(PsiElement e) {
        PsiFile pf = e.getContainingFile();
        if (pf == null) {
            return null;
        }
        VirtualFile vf = pf.getVirtualFile();
        if (vf == null) {
            return null;
        }
        return vf.getName();
    }

    /*
     * The attribute value itself, as the attribute value reference providers of the XML plug-in: attribute_name of a tag
     * from tag_names, in a file that the filter accepts.
     */
    private static PsiElementPattern.Capture<PsiElement> attribute_value(
            final String attribute_name, final String[] tag_names, final boolean sdm_xml_only) {

        final Set<String> tags = new HashSet<>(Arrays.asList(tag_names));
        return PlatformPatterns.psiElement().with(new PatternCondition<PsiElement>("sdmReference") {
            @Override
            public boolean accepts(@NotNull PsiElement e, ProcessingContext context) {
                if (!XmlPsi.is_attribute_value(e)) {
                    return false;
                }
                PsiElement attribute = e.getParent();
                if (!XmlPsi.is_attribute(attribute) || !attribute_name.equals(XmlPsi.name_of(attribute))) {
                    return false;
                }
                PsiElement tag = attribute.getParent();
                if (!XmlPsi.is_tag(tag) || !tags.contains(XmlPsi.name_of(tag))) {
                    return false;
                }
                return sdm_xml_only ? in_sdm_xml(e) : in_sdm_or_dao_xml(e);
            }
        });
    }

    public void registerReferenceProviders(@NotNull final PsiReferenceRegistrar registrar) {
        // based on XmlReferenceContributor of the XML plug-in
        {
            PsiReferenceDtoClassProvider ref_provider_dto_class = new PsiReferenceDtoClassProvider();
            registrar.registerReferenceProvider(attribute_value("name",
                    new String[]{IdeaRefUtils.ELEMENT.DTO_CLASS}, true), ref_provider_dto_class);
            registrar.registerReferenceProvider(attribute_value("name",
                    new String[]{IdeaRefUtils.ELEMENT.DAO_CLASS}, true), ref_provider_dto_class);
            registrar.registerReferenceProvider(attribute_value("dto",
                    IdeaRefUtils.DAO_TAGS_USING_DTO, false), ref_provider_dto_class);
        }
        {
            PsiReferenceSqlProvider ref_provider_sql_file = new PsiReferenceSqlProvider();
            registrar.registerReferenceProvider(attribute_value("ref",
                    new String[]{IdeaRefUtils.ELEMENT.DTO_CLASS}, true), ref_provider_sql_file);
            registrar.registerReferenceProvider(attribute_value("ref",
                    IdeaRefUtils.DAO_TAGS_USING_REF, false), ref_provider_sql_file);
        }
        {
            PsiReferenceExternalDaoXmllProvider ref_provider_external_dao_xml_file = new PsiReferenceExternalDaoXmllProvider();
            registrar.registerReferenceProvider(attribute_value("ref",
                    new String[]{IdeaRefUtils.ELEMENT.DAO_CLASS}, true), ref_provider_external_dao_xml_file);
        }
    }
}
