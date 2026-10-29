package dev.view;

import dev.model.PasswordEntry;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PasswordTableModel extends AbstractTableModel {
    private List<PasswordEntry> entries;
    private final String[] columnNames = {"Título", "Usuario", "URL", "Última modificación"};
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PasswordTableModel(List<PasswordEntry> entries) {
        this.entries = entries;
    }

    public void setEntries(List<PasswordEntry> entries) {
        this.entries = entries;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() { return entries.size(); }

    @Override
    public int getColumnCount() { return columnNames.length; }

    @Override
    public String getColumnName(int column) { return columnNames[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PasswordEntry entry = entries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> entry.getTitle();
            case 1 -> entry.getUsername();
            case 2 -> entry.getUrl();
            case 3 -> entry.getModified().format(formatter);
            default -> null;
        };
    }
}
