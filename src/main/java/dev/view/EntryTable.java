package dev.view;

import dev.model.PasswordEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Predicate;

/** Tabla de entradas con ordenación, filtro y celdas personalizadas. Trabaja siempre con índices de modelo. */
public class EntryTable extends JTable {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"));

    private final PasswordTableModel model;
    private final TableRowSorter<PasswordTableModel> sorter;

    public EntryTable(PasswordTableModel model) {
        super(model);
        this.model = model;
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setShowGrid(false);
        getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<>(model);
        sorter.setComparator(PasswordTableModel.COL_NAME,
                Comparator.comparing((PasswordEntry e) -> nullToEmpty(e.getTitle()), String.CASE_INSENSITIVE_ORDER));
        sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(PasswordTableModel.COL_NAME, SortOrder.ASCENDING)));
        setRowSorter(sorter);

        getColumnModel().getColumn(PasswordTableModel.COL_NAME).setPreferredWidth(240);
        getColumnModel().getColumn(PasswordTableModel.COL_USER).setPreferredWidth(170);
        getColumnModel().getColumn(PasswordTableModel.COL_URL).setPreferredWidth(170);
        getColumnModel().getColumn(PasswordTableModel.COL_MODIFIED).setMinWidth(112);
        getColumnModel().getColumn(PasswordTableModel.COL_MODIFIED).setMaxWidth(130);
        // Cabeceras alineadas a la izquierda, como el contenido.
        TableCellRenderer base = getTableHeader().getDefaultRenderer();
        getTableHeader().setDefaultRenderer((t, v, sel, foc, r, c) -> {
            Component comp = base.getTableCellRendererComponent(t, v, sel, foc, r, c);
            if (comp instanceof JLabel l) l.setHorizontalAlignment(SwingConstants.LEADING);
            return comp;
        });
        getColumnModel().getColumn(PasswordTableModel.COL_NAME).setCellRenderer(new TitleRenderer());
        getColumnModel().getColumn(PasswordTableModel.COL_URL).setCellRenderer(new MutedRenderer(false));
        getColumnModel().getColumn(PasswordTableModel.COL_MODIFIED).setCellRenderer(new MutedRenderer(true));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public void setFilter(Predicate<PasswordEntry> predicate) {
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends PasswordTableModel, ? extends Integer> e) {
                return predicate.test(e.getModel().entryAt(e.getIdentifier()));
            }
        });
    }

    public PasswordEntry selectedEntry() {
        int row = getSelectedRow();
        return row < 0 ? null : model.entryAt(convertRowIndexToModel(row));
    }

    public void selectById(int id) {
        for (int viewRow = 0; viewRow < getRowCount(); viewRow++) {
            if (model.entryAt(convertRowIndexToModel(viewRow)).getId() == id) {
                setRowSelectionInterval(viewRow, viewRow);
                scrollRectToVisible(getCellRect(viewRow, 0, true));
                return;
            }
        }
        clearSelection();
    }

    private static Color muted() {
        return UIManager.getColor("App.muted");
    }

    private static final class TitleRenderer extends JPanel implements TableCellRenderer {
        private final JLabel avatar = new JLabel();
        private final JLabel title = new JLabel();
        private final JLabel subtitle = new JLabel();
        private final JLabel star = new JLabel();

        TitleRenderer() {
            super(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            subtitle.putClientProperty("FlatLaf.styleClass", "small");
            JPanel text = new JPanel(new GridLayout(0, 1));
            text.setOpaque(false);
            text.add(title);
            text.add(subtitle);
            JPanel textWrap = new JPanel(new GridBagLayout());
            textWrap.setOpaque(false);
            textWrap.add(text, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            add(avatar, BorderLayout.WEST);
            add(textWrap, BorderLayout.CENTER);
            add(star, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            PasswordEntry e = (PasswordEntry) value;
            Color bg = selected ? table.getSelectionBackground() : table.getBackground();
            Color fg = selected ? table.getSelectionForeground() : table.getForeground();
            setBackground(bg);
            title.setForeground(fg);
            subtitle.setForeground(muted());
            title.setText(e.getTitle());
            String cat = e.getCategory();
            subtitle.setText(cat == null || cat.isBlank() ? " " : cat);
            avatar.setIcon(new Icons.Avatar(e.getTitle(), 28));
            star.setIcon(e.isFavorite() ? Icons.colored("star-filled", 14, () -> UIManager.getColor("App.warning")) : null);
            return this;
        }
    }

    private static final class MutedRenderer extends DefaultTableCellRenderer {
        private final boolean date;

        MutedRenderer(boolean date) {
            this.date = date;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            Object shown = date && value instanceof LocalDateTime dt ? DATE.format(dt) : value;
            super.getTableCellRendererComponent(table, shown, selected, false, row, col);
            if (!selected) setForeground(muted());
            return this;
        }
    }
}
