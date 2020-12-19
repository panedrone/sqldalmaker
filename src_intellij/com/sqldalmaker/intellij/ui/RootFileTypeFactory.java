/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 20.06.12
 */
public class RootFileTypeFactory extends FileTypeFactory {
    /*
    Language and File Type
    https://jetbrains.org/intellij/sdk/docs/tutorials/custom_language_support/language_and_filetype.html
    Registering a File Type
    https://jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/registering_file_type.html
     */
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        IdeaTargetLanguageHelpers.register(consumer, RootFileType.INSTANCE);
    }
}