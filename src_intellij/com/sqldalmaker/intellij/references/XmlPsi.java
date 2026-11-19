/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.lang.ASTNode;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * The XML tree of sdm.xml and DAO XML by the generic PSI of the platform: tags, attributes and their values
 * are told by the names of their element types (XML_TAG, XML_ATTRIBUTE, ...), not by com.intellij.psi.xml.
 * Since IntelliJ 2026.x the XML PSI lives in the plug-in "XML and HTML", and SDM does not depend on it.
 * Without that plug-in there is no XML PSI at all, and nothing here is ever called.
 *
 * 19.11.2026
 */
public class XmlPsi {

    private static final String TAG = "XML_TAG";
    private static final String ATTRIBUTE = "XML_ATTRIBUTE";
    private static final String ATTRIBUTE_VALUE = "XML_ATTRIBUTE_VALUE";
    private static final String NAME = "XML_NAME";
    private static final String DOCUMENT = "XML_DOCUMENT";

    private static boolean is(@Nullable PsiElement e, @NotNull String type) {
        if (e == null) {
            return false;
        }
        ASTNode node = e.getNode();
        return node != null && type.equals(node.getElementType().toString());
    }

    public static boolean is_tag(@Nullable PsiElement e) {
        return is(e, TAG);
    }

    public static boolean is_attribute(@Nullable PsiElement e) {
        return is(e, ATTRIBUTE);
    }

    public static boolean is_attribute_value(@Nullable PsiElement e) {
        return is(e, ATTRIBUTE_VALUE);
    }

    private static @Nullable String first_name(@NotNull PsiElement e) {
        for (PsiElement c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (is(c, NAME)) {
                return c.getText();
            }
        }
        return null;
    }

    // the name of a tag or of an attribute
    public static @Nullable String name_of(@Nullable PsiElement tag_or_attribute) {
        if (tag_or_attribute == null) {
            return null;
        }
        return first_name(tag_or_attribute);
    }

    // the text of an attribute value without quotes
    public static @NotNull String value_text(@NotNull PsiElement attribute_value) {
        String s = attribute_value.getText();
        if (s.length() >= 2) {
            char q = s.charAt(0);
            if ((q == '"' || q == '\'') && s.charAt(s.length() - 1) == q) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    public static @Nullable String attribute_value(@NotNull PsiElement tag, @NotNull String attribute_name) {
        for (PsiElement c = tag.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (is_attribute(c) && attribute_name.equals(first_name(c))) {
                for (PsiElement v = c.getFirstChild(); v != null; v = v.getNextSibling()) {
                    if (is_attribute_value(v)) {
                        return value_text(v);
                    }
                }
                return null;
            }
        }
        return null;
    }

    public static @Nullable PsiElement root_tag(@Nullable PsiFile file) {
        if (file == null) {
            return null;
        }
        for (PsiElement c = file.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (is(c, DOCUMENT)) {
                for (PsiElement t = c.getFirstChild(); t != null; t = t.getNextSibling()) {
                    if (is_tag(t)) {
                        return t;
                    }
                }
            }
        }
        return null;
    }

    public static @NotNull List<PsiElement> sub_tags(@Nullable PsiElement tag) {
        List<PsiElement> res = new ArrayList<>();
        if (tag == null) {
            return res;
        }
        for (PsiElement c = tag.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (is_tag(c)) {
                res.add(c);
            }
        }
        return res;
    }

    // the attribute value that contains e, e included
    public static @Nullable PsiElement attribute_value_of(@Nullable PsiElement e) {
        for (; e != null && !(e instanceof PsiFile); e = e.getParent()) {
            if (is_attribute_value(e)) {
                return e;
            }
        }
        return null;
    }

    /*
     * The pattern of XmlPatterns.xmlAttributeValue().withLocalName(attribute_name)
     * .withSuperParent(level, XmlPatterns.xmlTag().withName(tag_names)), optionally in a file of the name
     * file_name, for the element itself or any of its parents: level 2 is the tag of the attribute, 3 is its
     * parent tag (<dao-class> around <query...>).
     */
    public static @NotNull PsiElementPattern.Capture<PsiElement> inside_attribute_value(
            @NotNull final String attribute_name,
            final int level,
            @NotNull final String[] tag_names,
            @Nullable final String file_name) {

        final Set<String> tags = new HashSet<>(Arrays.asList(tag_names));
        return PlatformPatterns.psiElement().with(new PatternCondition<PsiElement>("sdmAttributeValue") {
            @Override
            public boolean accepts(@NotNull PsiElement e, ProcessingContext context) {
                PsiElement value = attribute_value_of(e);
                if (value == null) {
                    return false;
                }
                PsiElement attribute = value.getParent();
                if (!is_attribute(attribute) || !attribute_name.equals(name_of(attribute))) {
                    return false;
                }
                PsiElement tag = attribute.getParent();
                for (int i = 2; i < level && tag != null; i++) {
                    tag = tag.getParent();
                }
                if (!is_tag(tag) || !tags.contains(name_of(tag))) {
                    return false;
                }
                if (file_name == null) {
                    return true;
                }
                PsiFile f = value.getContainingFile();
                if (f == null) {
                    return false;
                }
                PsiFile original = f.getOriginalFile();
                return file_name.equals(original.getName()) || file_name.equals(f.getName());
            }
        });
    }
}
