/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class RootFileEditorTabTitleProvider implements EditorTabTitleProvider {

    @Override
    public String getEditorTabTitle(Project project, VirtualFile file) {

        return IdeaTargetLanguageHelpers.get_relative_path(project, file);
    }
}