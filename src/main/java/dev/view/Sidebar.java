package dev.view;

import dev.model.PasswordEntry;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Barra lateral: logo, vistas (todas / favoritas), categorías y botón de bloqueo. */
public class Sidebar extends JPanel {
    /** Una vista de la lista de entradas; {@code key} identifica la vista para conservar la selección. */
    public record View(String key, String label, Icon icon, int count, Predicate<PasswordEntry> filter) {}

    private final DefaultListModel<Object> listModel = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(listModel);
    private final Consumer<View> onSelect;
    private boolean updating;

    public Sidebar(Consumer<View> onSelect, Runnable onLock) {
        super(new BorderLayout());
        this.onSelect = onSelect;
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createEmptyBorder(18, 10, 14, 10));

        JLabel brand = new JLabel(Icons.logo(170));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 8, 14, 0));
        brand.setHorizontalAlignment(SwingConstants.LEFT);

        list.setOpaque(false);
        list.setCellRenderer(new Renderer());
        list.setFixedCellHeight(36);
        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int from, int to) {
                if (from >= 0 && from < listModel.size() && listModel.get(from) instanceof String) return; // cabecera
                super.setSelectionInterval(from, to);
            }
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updating) return;
            Object v = list.getSelectedValue();
            if (v instanceof View view) {
                onSelect.accept(view);
            } else if (v == null) {
                list.setSelectedIndex(0);
            }
        });

        JButton lock = new JButton("Bloquear bóveda", Icons.get("lock", 16));
        lock.putClientProperty("JButton.buttonType", "borderless");
        lock.setHorizontalAlignment(SwingConstants.LEFT);
        lock.setToolTipText("Bloquear (Ctrl+L)");
        lock.addActionListener(e -> onLock.run());

        add(brand, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
        add(lock, BorderLayout.SOUTH);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(UIManager.getColor("App.sidebar.background"));
    }

    /** Reconstruye las vistas con los recuentos actuales, conservando la vista seleccionada si sigue existiendo. */
    public void refresh(List<PasswordEntry> entries) {
        String keep = list.getSelectedValue() instanceof View v ? v.key() : "all";
        updating = true;
        listModel.clear();

        long favorites = entries.stream().filter(PasswordEntry::isFavorite).count();
        listModel.addElement(new View("all", "Todas las entradas", Icons.get("list"), entries.size(), e -> true));
        listModel.addElement(new View("favorites", "Favoritas", Icons.get("star"), (int) favorites, PasswordEntry::isFavorite));

        Map<String, Integer> categories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (PasswordEntry e : entries) {
            String c = e.getCategory();
            if (c != null && !c.isBlank()) categories.merge(c, 1, Integer::sum);
        }
        if (!categories.isEmpty()) {
            listModel.addElement("CATEGORÍAS");
            categories.forEach((name, count) -> listModel.addElement(new View("cat:" + name.toLowerCase(), name,
                    Icons.get("folder"), count, e -> name.equalsIgnoreCase(e.getCategory()))));
        }

        int index = 0;
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i) instanceof View v && v.key().equals(keep)) index = i;
        }
        list.setSelectedIndex(index);
        updating = false;
        onSelect.accept((View) listModel.get(index));
    }

    public List<String> categories() {
        return java.util.Collections.list(listModel.elements()).stream()
                .filter(o -> o instanceof View v && v.key().startsWith("cat:"))
                .map(o -> ((View) o).label()).toList();
    }

    private static final class Renderer extends JPanel implements ListCellRenderer<Object> {
        private final JLabel text = new JLabel();
        private final JLabel count = new JLabel();
        private boolean selected;
        private boolean header;

        Renderer() {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            count.putClientProperty("FlatLaf.styleClass", "small");
            text.setIconTextGap(10);
            add(text, BorderLayout.CENTER);
            add(count, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean isSelected, boolean focus) {
            selected = isSelected && value instanceof View;
            header = value instanceof String;
            if (value instanceof View v) {
                text.setText(v.label());
                text.setIcon(v.icon());
                text.setFont(l.getFont());
                text.setForeground(selected ? UIManager.getColor("Component.accentColor") : l.getForeground());
                count.setText(String.valueOf(v.count()));
                count.setForeground(UIManager.getColor("App.muted"));
            } else {
                text.setText(value.toString());
                text.setIcon(null);
                text.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() - 2f));
                text.setForeground(UIManager.getColor("App.muted"));
                count.setText("");
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color accent = UIManager.getColor("Component.accentColor");
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 38));
                g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 10, 10);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
