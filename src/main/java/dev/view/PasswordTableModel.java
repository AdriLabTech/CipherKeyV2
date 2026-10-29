package dev.view;

import dev.model.PasswordEntry;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDateTime;
import java.util.List;

public class PasswordTableModel extends AbstractTableModel {
    static final int COL_NAME = 0, COL_USER = 1, COL_URL = 2, COL_MODIFIED = 3;

    private final String[] columnNames = {"Nombre", "Usuario", "Sitio web", "Modificado"};
    private List<PasswordEntry> entries;

    public PasswordTableModel(List<PasswordEntry> entries) {
        this.entries = entries;
    }

    public void setEntries(List<PasswordEntry> entries) {
        this.entries = entries;
        fireTableDataChanged();
    }

    public PasswordEntry entryAt(int modelRow) {
        return entries.get(modelRow);
    }

    @Override
    public int getRowCount() { return entries.size(); }

    @Override
    public int getColumnCount() { return columnNames.length; }

    @Override
    public String getColumnName(int column) { return columnNames[column]; }

    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case COL_NAME -> PasswordEntry.class;
            case COL_MODIFIED -> LocalDateTime.class;
            default -> String.class;
        };
    }

    /** La columna "Nombre" devuelve la entrada completa para que el renderizador pinte avatar, categoría y favorito. */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PasswordEntry entry = entries.get(rowIndex);
        return switch (columnIndex) {
            case COL_NAME -> entry;
            case COL_USER -> entry.getUsername();
            case COL_URL -> entry.getUrl();
            case COL_MODIFIED -> entry.getModified();
            default -> null;
        };
    }
}
