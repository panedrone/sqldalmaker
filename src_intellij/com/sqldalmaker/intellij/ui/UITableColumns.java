/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * === panedrone:
 * Ширины колонок, выставленные в конструкторе панели, в свежих IDE (2026.2+) не доживают
 * до показа таблицы: раскладка колонок пересчитывается после того, как таблица попадает
 * в иерархию компонентов, и один только setPreferredWidth() при этом теряется.
 * Поэтому: (1) ширина фиксируется min/preferred/max, а не одним preferred,
 * (2) значения применяются повторно, когда таблица реально показана,
 * (3) размеры масштабируются под HiDPI через JBUI.scale().
 */
class UITableColumns {

    private UITableColumns() {
    }

    /**
     * Первые widths.length колонок получают заданную ширину, последняя колонка тянется по остатку.
     */
    static void setup(final JTable table, final int... widths) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        apply(table, widths);
        table.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && table.isShowing()) {
                    apply(table, widths);
                }
            }
        });
    }

    private static void apply(JTable table, int[] widths) {
        TableColumnModel cm = table.getColumnModel();
        int last = cm.getColumnCount() - 1;
        if (last < 1) {
            return;
        }
        for (int i = 0; i < widths.length && i < last; i++) {
            int w = JBUI.scale(widths[i]);
            TableColumn c = cm.getColumn(i);
            c.setMinWidth(w);       // без min ширина съедается при пересчёте раскладки
            c.setPreferredWidth(w);
            c.setMaxWidth(JBUI.scale(2000)); // остаток по ширине должна забирать последняя колонка, а не эта
        }
        TableColumn tail = cm.getColumn(last);
        tail.setMinWidth(JBUI.scale(40));
        tail.setMaxWidth(Integer.MAX_VALUE);
        table.doLayout();
    }
}
