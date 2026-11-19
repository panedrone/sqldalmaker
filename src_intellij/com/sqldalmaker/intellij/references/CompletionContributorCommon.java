/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;


import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.sqldalmaker.common.Const;

/*
 * implementation is based on com.intellij.codeInsight.completion.XmlCompletionContributor
 * and https://confluence.jetbrains.com/display/IntelliJIDEA/Completion+Contributor
 * <p>
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 */
public class CompletionContributorCommon extends CompletionContributor {

    public CompletionContributorCommon() {

        // ===: extensions must be registered as
        // <completion.contributor language="XML" implementationClass="my.reference.MyXmlCompletionContributor"/>
        // to prevent
        // com.intellij.diagnostic.PluginException: No key specified for extension of class class my.reference.MyXmlCompletionContributor

        extend(CompletionType.BASIC,
                XmlPsi.inside_attribute_value(IdeaRefUtils.ATTRIBUTE.REF, 3,
                        new String[]{IdeaRefUtils.ELEMENT.DAO_CLASS}, null),
                new CompletionProviderSql());
        extend(CompletionType.BASIC,
                XmlPsi.inside_attribute_value(IdeaRefUtils.ATTRIBUTE.REF, 2,
                        new String[]{IdeaRefUtils.ELEMENT.DTO_CLASS}, Const.SDM_XML),
                new CompletionProviderSql());
        extend(CompletionType.BASIC,
                XmlPsi.inside_attribute_value(IdeaRefUtils.ATTRIBUTE.REF, 2,
                        new String[]{IdeaRefUtils.ELEMENT.DAO_CLASS}, Const.SDM_XML),
                new CompletionProviderExternalDaoXml());
        extend(CompletionType.BASIC,
                XmlPsi.inside_attribute_value(IdeaRefUtils.ATTRIBUTE.DTO, 3,
                        new String[]{IdeaRefUtils.ELEMENT.DAO_CLASS}, null),
                new CompletionProviderDtoClass());
        extend(CompletionType.BASIC,
                XmlPsi.inside_attribute_value(IdeaRefUtils.ATTRIBUTE.TABLE, 3,
                        new String[]{IdeaRefUtils.ELEMENT.DAO_CLASS}, null),
                new CompletionProviderTable());
    }
}