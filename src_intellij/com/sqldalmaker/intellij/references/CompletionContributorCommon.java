/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;


import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.sqldalmaker.common.Const;

/**
 * implementation is based on com.intellij.codeInsight.completion.XmlCompletionContributor
 * and https://confluence.jetbrains.com/display/IntelliJIDEA/Completion+Contributor
 *
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
                PlatformPatterns.psiElement().
                        inside(XmlPatterns.xmlAttributeValue().withLocalName(IdeaReferenceCompletion.ATTRIBUTE.REF).
                                withSuperParent(3, XmlPatterns.xmlTag().withName(IdeaReferenceCompletion.ELEMENT.DAO_CLASS))),
                new CompletionProviderSql());

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().inside(XmlPatterns.xmlAttributeValue().withLocalName(IdeaReferenceCompletion.ATTRIBUTE.REF)
                        .inside(XmlPatterns.xmlFile().withName(Const.DTO_XML))),
                new CompletionProviderSql());

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().
                        inside(XmlPatterns.xmlAttributeValue().withLocalName(IdeaReferenceCompletion.ATTRIBUTE.DTO).
                                withSuperParent(3, XmlPatterns.xmlTag().withName(IdeaReferenceCompletion.ELEMENT.DAO_CLASS))),
                new CompletionProviderDtoClass());

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().
                        inside(XmlPatterns.xmlAttributeValue().withLocalName(IdeaReferenceCompletion.ATTRIBUTE.TABLE).
                                withSuperParent(3, XmlPatterns.xmlTag().withName(IdeaReferenceCompletion.ELEMENT.DAO_CLASS))),
                new CompletionProviderTable());
    }
}