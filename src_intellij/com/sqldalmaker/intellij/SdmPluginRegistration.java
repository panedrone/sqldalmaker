/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.BaseComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class SdmPluginRegistration implements BaseComponent {

    @Override
    public void initComponent() {

        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/206149699-Combobox-in-toolbar?page=1#community_comment_206239775

        // http://www.jetbrains.org/intellij/sdk/docs/basics/action_system.html

        ActionManager am = ActionManager.getInstance();

        ActionGroup my_group = (ActionGroup) am.getAction("SdmActionGroup");

        DefaultActionGroup toolbar_find_group = (DefaultActionGroup) am.getAction("ToolbarFindGroup");
        toolbar_find_group.addSeparator();
        toolbar_find_group.add(my_group);

        DefaultActionGroup find_menu_group = (DefaultActionGroup) am.getAction("FindMenuGroup");
        find_menu_group.addSeparator();
        find_menu_group.add(my_group);
    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return getClass().getName();
    }
}