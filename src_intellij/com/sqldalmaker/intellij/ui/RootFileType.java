/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.05.2024 20:00 1.299
 *
 */
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

    // deprecated with no class --> Deprecated method IconLoader.findIcon
    // https://plugins.jetbrains.com/docs/intellij/work-with-icons-and-images.html#how-to-organize-and-how-to-use-icons
    private static final Icon SDM_ICON = IconLoader.findIcon("/img/sqldalmaker.png", RootFileType.class);

    public Icon getIcon() {
        return SDM_ICON;
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
    public String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
        return null;
    }
}
