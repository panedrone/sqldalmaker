package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RootFileType implements FileType {
    /*
    Language and File Type
    https://jetbrains.org/intellij/sdk/docs/tutorials/custom_language_support/language_and_filetype.html
    Registering a File Type
    https://jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/registering_file_type.html
     */
    public static final RootFileType INSTANCE = new RootFileType();

    @NotNull
    @Override
    public String getName() {
        return getClass().getName();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "SDM root-file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "dal";
    }

    public Icon getIcon() {
        return SdmIcons.FILE;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @NonNls
    @Nullable
    @Override
    public String getCharset(@NotNull VirtualFile file, byte[] content) {
        return null;
    }
}
