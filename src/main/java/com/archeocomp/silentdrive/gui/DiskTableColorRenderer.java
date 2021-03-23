/*
 * table cell renderrer
 */
package com.archeocomp.silentdrive.gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 */
public class DiskTableColorRenderer extends JLabel implements TableCellRenderer {
    
    public DiskTableColorRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(
                            JTable table, Object value,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
        String newValue = (String)value;
		Color newColor = new Color(AllocationBlockStateEnum.valueOf(newValue).getStateColorRGB());
        setBackground(newColor);
        setToolTipText("Allocation block");
        return this;
    }

}
