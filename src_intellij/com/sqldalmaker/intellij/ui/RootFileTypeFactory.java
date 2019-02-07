/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 20.06.12
 */
public class RootFileTypeFactory extends FileTypeFactory {

    private static final FileType fileType = new MyFileType();

    public static final class MyFileType implements FileType {

        private static Icon icon = IconLoader.getIcon("/img/sqldalmaker.gif");

        @NotNull
        @Override
        public String getName() {
            return getClass().getName();
        }

        @NotNull
        @Override
        public String getDescription() {
            return "SQL DAL Maker root file";
        }

        @NotNull
        @Override
        public String getDefaultExtension() {
            return "";
        }

        public Icon getIcon() {
            return icon;
        }

        @Override
        public boolean isBinary() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @NotNull
        @Override
        public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
            return "";
        }
    }

    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        IdeaTargetLanguageHelpers.register(consumer, fileType);
    }
}