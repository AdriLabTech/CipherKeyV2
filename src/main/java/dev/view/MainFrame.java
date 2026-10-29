package dev.view;

import com.formdev.flatlaf.FlatClientProperties;
import dev.App;
import dev.controller.PasswordManager;
import dev.model.PasswordEntry;
import dev.persistence.AppPrefs;
import dev.persistence.EntryCsv;
import dev.persistence.VaultException;
import dev.security.PasswordAudit;
import dev.security.SecureClipboard;
import dev.security.Totp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Predicate;

public class MainFrame extends JFrame implements DetailPanel.Actions {
    private static final String CARD_TABLE = "table", CARD_EMPTY = "empty", CARD_NO_RESULTS = "noresults";

    private final PasswordManager manager;
    private final PasswordTableModel tableModel;
    private final EntryTable table;
    private final Sidebar sidebar;
    private final DetailPanel detail;
    private final JTextField searchField = new JTextField();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JButton themeButton;
    private final InactivityListener inactivityListener;

    private Predicate<PasswordEntry> viewFilter = e -> true;

    public MainFrame(PasswordManager manager) {
        super("CipherKey");
        this.manager = manager;
        setIconImage(Icons.appIcon());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        tableModel = new PasswordTableModel(manager.getEntries());
        table = new EntryTable(tableModel);
        detail = new DetailPanel(this);
        sidebar = new Sidebar(view -> {
            viewFilter = view.filter();
            applyFilter();
        }, this::openAudit, this::lock);
        themeButton = Ui.iconButton(themeIcon(), "Cambiar tema", e -> toggleTheme());

        buildLayout();
        installShortcuts();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) detail.show(table.selectedEntry());
        });
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && table.selectedEntry() != null) edit();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) { maybePopup(evt); }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) { maybePopup(evt); }
        });

        inactivityListener = new InactivityListener(AppPrefs.autoLockMinutes() * 60, this::lock);
        Toolkit.getDefaultToolkit().addAWTEventListener(inactivityListener,
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);

        sidebar.refresh(manager.getEntries());
        setMinimumSize(new Dimension(940, 580));
        Dimension saved = AppPrefs.windowSize();
        setSize(saved != null ? saved : new Dimension(1200, 740));
        setLocationRelativeTo(null);
        if (AppPrefs.isWindowMaximized()) setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // ---------- construcción ----------

    private void buildLayout() {
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setViewportBorder(null);
        tableScroll.getViewport().setBackground(UIManager.getColor("Table.background"));
        cardPanel.add(tableScroll, CARD_TABLE);
        cardPanel.add(buildMessage("vault", "Tu bóveda está vacía",
                "Guarda aquí tus contraseñas; se cifran en tu equipo.", "Añadir la primera entrada"), CARD_EMPTY);
        cardPanel.add(buildMessage("search", "Sin resultados", "Prueba con otra búsqueda o cambia de vista.", null), CARD_NO_RESULTS);

        JPanel center = new JPanel(new BorderLayout());
        center.add(buildToolbar(), BorderLayout.NORTH);
        JPanel main = new JPanel(new BorderLayout());
        main.add(cardPanel, BorderLayout.CENTER);
        main.add(detail, BorderLayout.EAST);
        center.add(main, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JComponent buildToolbar() {
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre, usuario, sitio o notas…  (Ctrl+F)");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.get("search"));
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 5,10,5,10");

        JButton generator = Ui.iconButton(Icons.get("key", 18), "Generador de contraseñas",
                e -> new PasswordGeneratorDialog(this, false).setVisible(true));
        JButton more = Ui.iconButton(Icons.get("more", 18), "Más opciones", e -> {});
        more.addActionListener(e -> buildMoreMenu().show(more, 0, more.getHeight()));
        JButton add = Ui.primary("Nueva entrada", Icons.white("plus", 16), e -> addEntry());
        add.setToolTipText("Ctrl+N");

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.add(generator);
        right.add(themeButton);
        right.add(more);
        right.add(Box.createHorizontalStrut(6));
        right.add(add);

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 14));
        bar.add(searchField, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPopupMenu buildMoreMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem settings = new JMenuItem("Ajustes…", Icons.get("key"));
        settings.addActionListener(e -> openSettings());
        JMenuItem importItem = new JMenuItem("Importar desde CSV…", Icons.get("download"));
        importItem.addActionListener(e -> importCsv());
        JMenuItem exportItem = new JMenuItem("Exportar a CSV…", Icons.get("upload"));
        exportItem.addActionListener(e -> exportCsv());
        JMenuItem backupItem = new JMenuItem("Copia de seguridad…", Icons.get("save"));
        backupItem.addActionListener(e -> backupVault());
        JMenuItem auditItem = new JMenuItem("Auditoría de seguridad", Icons.get("shield"));
        auditItem.addActionListener(e -> openAudit());
        JMenuItem about = new JMenuItem("Acerca de CipherKey", Icons.get("info"));
        about.addActionListener(e -> new AboutDialog(this).setVisible(true));
        JMenuItem lockItem = new JMenuItem("Bloquear bóveda", Icons.get("lock"));
        lockItem.addActionListener(e -> lock());
        JMenuItem exit = new JMenuItem("Salir", Icons.get("x"));
        exit.addActionListener(e -> shutdown());
        menu.add(settings);
        menu.add(auditItem);
        menu.addSeparator();
        menu.add(importItem);
        menu.add(exportItem);
        menu.add(backupItem);
        menu.addSeparator();
        menu.add(about);
        menu.addSeparator();
        menu.add(lockItem);
        menu.add(exit);
        return menu;
    }

    private JComponent buildMessage(String icon, String title, String text, String buttonText) {
        JPanel p = new JPanel(new GridBagLayout());
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel i = new JLabel(Icons.accent(icon, 56));
        JLabel t = Ui.title(title);
        JLabel d = Ui.muted(text);
        for (JComponent c : new JComponent[]{i, t, d}) c.setAlignmentX(CENTER_ALIGNMENT);
        box.add(i);
        box.add(Box.createVerticalStrut(12));
        box.add(t);
        box.add(Box.createVerticalStrut(4));
        box.add(d);
        if (buttonText != null) {
            JButton b = Ui.primary(buttonText, Icons.white("plus", 16), e -> addEntry());
            b.setAlignmentX(CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(18));
            box.add(b);
        }
        p.add(box);
        return p;
    }

    private void installShortcuts() {
        JRootPane root = getRootPane();
        bind(root, "ctrl N", "new", this::addEntry);
        bind(root, "ctrl F", "search", () -> {
            searchField.requestFocusInWindow();
            searchField.selectAll();
        });
        bind(root, "ctrl L", "lock", this::lock);
        bind(root, "ctrl B", "copyPassword", this::copyPassword);
        bind(root, "ctrl U", "copyUser", this::copyUsername);
        bind(root, "ctrl E", "edit", this::edit);

        InputMap tableKeys = table.getInputMap(JComponent.WHEN_FOCUSED);
        tableKeys.put(KeyStroke.getKeyStroke("DELETE"), "deleteEntry");
        tableKeys.put(KeyStroke.getKeyStroke("ENTER"), "editEntry");
        table.getActionMap().put("deleteEntry", action(this::delete));
        table.getActionMap().put("editEntry", action(this::edit));

        searchField.registerKeyboardAction(e -> {
            if (searchField.getText().isEmpty()) table.requestFocusInWindow();
            else searchField.setText("");
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_FOCUSED);
        searchField.registerKeyboardAction(e -> table.requestFocusInWindow(),
                KeyStroke.getKeyStroke("DOWN"), JComponent.WHEN_FOCUSED);
    }

    private static void bind(JRootPane root, String stroke, String name, Runnable r) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(stroke), name);
        root.getActionMap().put(name, action(r));
    }

    private static Action action(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                r.run();
            }
        };
    }

    private void maybePopup(java.awt.event.MouseEvent evt) {
        if (!evt.isPopupTrigger()) return;
        int row = table.rowAtPoint(evt.getPoint());
        if (row < 0) return;
        table.setRowSelectionInterval(row, row);
        PasswordEntry e = table.selectedEntry();

        JPopupMenu menu = new JPopupMenu();
        add(menu, "Copiar usuario", "copy", this::copyUsername);
        add(menu, "Copiar contraseña", "copy", this::copyPassword);
        if (e.getUrl() != null && !e.getUrl().isBlank()) add(menu, "Abrir sitio web", "external-link", this::openUrl);
        menu.addSeparator();
        add(menu, "Editar", "edit", this::edit);
        add(menu, e.isFavorite() ? "Quitar de favoritas" : "Marcar como favorita", "star", this::toggleFavorite);
        menu.addSeparator();
        add(menu, "Eliminar", "trash", this::delete);
        menu.show(table, evt.getX(), evt.getY());
    }

    private static void add(JPopupMenu menu, String text, String icon, Runnable r) {
        JMenuItem item = new JMenuItem(text, Icons.get(icon));
        item.addActionListener(e -> r.run());
        menu.add(item);
    }

    // ---------- filtro y estado ----------

    private void applyFilter() {
        String[] tokens = searchField.getText().toLowerCase(Locale.ROOT).trim().split("\\s+");
        table.setFilter(entry -> {
            if (!viewFilter.test(entry)) return false;
            String haystack = (nz(entry.getTitle()) + " " + nz(entry.getUsername()) + " " + nz(entry.getUrl()) + " "
                    + nz(entry.getCategory()) + " " + nz(entry.getNotes())).toLowerCase(Locale.ROOT);
            for (String t : tokens) {
                if (!t.isEmpty() && !haystack.contains(t)) return false;
            }
            return true;
        });
        updateCard();
        detail.show(table.selectedEntry());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void updateCard() {
        if (manager.getEntries().isEmpty()) cards.show(cardPanel, CARD_EMPTY);
        else if (table.getRowCount() == 0) cards.show(cardPanel, CARD_NO_RESULTS);
        else cards.show(cardPanel, CARD_TABLE);
    }

    /** Tras cambiar los datos: actualiza tabla, barra lateral y selección. */
    private void refresh(Integer selectId) {
        tableModel.setEntries(manager.getEntries());
        sidebar.refresh(manager.getEntries());
        updateCard();
        if (selectId != null) table.selectById(selectId);
        detail.show(table.selectedEntry());
    }

    private Icon themeIcon() {
        return Icons.get(ThemeManager.isDark() ? "sun" : "moon", 18);
    }

    private void toggleTheme() {
        ThemeManager.toggle();
        themeButton.setIcon(themeIcon());
    }

    // ---------- acciones ----------

    private void addEntry() {
        EntryDialog dialog = new EntryDialog(this, null, sidebar.categories());
        if (dialog.showDialog()) {
            PasswordEntry created = dialog.getEntry();
            manager.addEntry(created);
            if (save()) Toast.show(this, "Entrada añadida");
            clearViewFilters();
            refresh(created.getId());
        }
    }

    /** Evita que una entrada recién creada quede oculta por la búsqueda activa. */
    private void clearViewFilters() {
        if (!searchField.getText().isEmpty()) searchField.setText("");
    }

    @Override
    public void edit() {
        PasswordEntry entry = table.selectedEntry();
        if (entry == null) return;
        EntryDialog dialog = new EntryDialog(this, entry, sidebar.categories());
        if (dialog.showDialog()) {
            PasswordEntry updated = dialog.getEntry();
            manager.updateEntry(updated);
            if (save()) Toast.show(this, "Cambios guardados");
            refresh(updated.getId());
        }
    }

    @Override
    public void delete() {
        PasswordEntry entry = table.selectedEntry();
        if (entry == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar «" + entry.getTitle() + "»? Esta acción no se puede deshacer.",
                "Eliminar entrada", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            manager.deleteEntry(entry.getId());
            if (save()) Toast.show(this, "Entrada eliminada");
            refresh(null);
        }
    }

    @Override
    public void toggleFavorite() {
        PasswordEntry entry = table.selectedEntry();
        if (entry == null) return;
        entry.setFavorite(!entry.isFavorite());
        save();
        refresh(entry.getId());
    }

    @Override
    public void copyUsername() {
        PasswordEntry e = table.selectedEntry();
        if (e == null || e.getUsername() == null || e.getUsername().isEmpty()) return;
        SecureClipboard.copyToClipboard(e.getUsername());
        Toast.show(this, "Usuario copiado · se borrará en 30 s");
    }

    @Override
    public void copyPassword() {
        PasswordEntry e = table.selectedEntry();
        if (e == null || e.getPassword() == null || e.getPassword().isEmpty()) return;
        SecureClipboard.copyToClipboard(e.getPassword());
        Toast.show(this, "Contraseña copiada · se borrará en 30 s");
    }

    @Override
    public void openUrl() {
        PasswordEntry e = table.selectedEntry();
        if (e == null || e.getUrl() == null || e.getUrl().isBlank()) return;
        String url = e.getUrl().trim();
        if (!url.contains("://")) url = "https://" + url;
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            // Solo web: nunca ejecutar esquemas arbitrarios (file:, javascript:…) guardados en una entrada.
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                Toast.show(this, "Solo se pueden abrir enlaces http(s)");
                return;
            }
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            Toast.show(this, "No se pudo abrir el enlace");
        }
    }

    @Override
    public void copyTotp() {
        PasswordEntry e = table.selectedEntry();
        if (e == null || e.getTotpSecret() == null) return;
        try {
            SecureClipboard.copyToClipboard(Totp.parse(e.getTotpSecret()).code(Instant.now()));
            Toast.show(this, "Código 2FA copiado · se borrará en 30 s");
        } catch (IllegalArgumentException ex) {
            Toast.show(this, "La clave 2FA no es válida");
        }
    }

    @Override
    public void showHistory() {
        PasswordEntry e = table.selectedEntry();
        if (e != null) new HistoryDialog(this, e).setVisible(true);
    }

    private void openAudit() {
        PasswordAudit.Report report = PasswordAudit.analyze(manager.getEntries(), LocalDateTime.now(),
                PasswordAudit.DEFAULT_OLD_DAYS);
        new AuditDialog(this, report, finding -> {
            if (!searchField.getText().isEmpty()) searchField.setText("");
            sidebar.selectAll();
            table.selectById(finding.entry().getId());
        }).setVisible(true);
    }

    private void importCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar contraseñas desde CSV");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos CSV", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            EntryCsv.Imported imported = EntryCsv.read(Files.readString(fc.getSelectedFile().toPath()));
            int added = manager.importEntries(imported.entries());
            if (added > 0 && !save()) return;
            clearViewFilters();
            refresh(null);
            int duplicates = imported.entries().size() - added;
            JOptionPane.showMessageDialog(this,
                    "<html><b>Importadas: " + added + "</b><br>Duplicadas (ya existían): " + duplicates
                            + "<br>Omitidas (no son inicios de sesión): " + imported.skipped()
                            + "<br><br>El CSV contiene tus contraseñas <b>sin cifrar</b>: bórralo cuando termines.</html>",
                    "Importación terminada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo importar: " + ex.getMessage(), "Importar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        int ok = JOptionPane.showConfirmDialog(this,
                "<html>El archivo exportado contendrá todas tus contraseñas <b>en texto plano</b>.<br>"
                        + "Guárdalo en un lugar seguro y bórralo cuando ya no lo necesites.<br><br>¿Continuar?</html>",
                "Exportar a CSV", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        Path target = chooseSaveTarget("Exportar a CSV", "cipherkey-export.csv", "csv", "Archivos CSV");
        if (target == null) return;
        try {
            EntryCsv.writeFile(target, manager.getEntries());
            Toast.show(this, "Exportadas " + manager.getEntries().size() + " entradas");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(), "Exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** La bóveda ya está cifrada y guardada tras cada cambio, así que basta con copiar el archivo. */
    private void backupVault() {
        Path source = manager.getVaultFile();
        String stem = source.getFileName().toString().replaceFirst("\\.ckey$", "");
        String name = stem + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".ckey";
        Path target = chooseSaveTarget("Copia de seguridad", name, "ckey", "Bóvedas CipherKey");
        if (target == null) return;
        try {
            if (Files.isSameFile(source, target)) {
                JOptionPane.showMessageDialog(this, "Elige un destino distinto de la bóveda original.");
                return;
            }
        } catch (IOException ignored) {
            // el destino aún no existe: no es el mismo archivo
        }
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            Toast.show(this, "Copia guardada (sigue cifrada con tu clave)");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo copiar: " + ex.getMessage(), "Copia de seguridad", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Selector de archivo para guardar, con aviso si el destino ya existe. Devuelve {@code null} si se cancela. */
    private Path chooseSaveTarget(String title, String defaultName, String extension, String description) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setSelectedFile(new java.io.File(defaultName));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(description, extension));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        java.io.File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + extension)) f = new java.io.File(f.getAbsolutePath() + "." + extension);
        if (f.exists() && JOptionPane.showConfirmDialog(this, "«" + f.getName() + "» ya existe. ¿Sustituirlo?",
                title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return null;
        }
        return f.toPath();
    }

    private void saveWindowState() {
        boolean maximized = (getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
        AppPrefs.setWindowMaximized(maximized);
        if (!maximized) AppPrefs.setWindowSize(getWidth(), getHeight());
    }

    private void openSettings() {
        SettingsDialog dlg = new SettingsDialog(this, manager);
        dlg.setVisible(true);
        inactivityListener.setTimeoutSeconds(AppPrefs.autoLockMinutes() * 60);
        if (dlg.isPasswordChanged()) Toast.show(this, "Clave maestra cambiada");
    }

    /** Cada cambio se guarda en el acto (cifrar es barato: la KDF solo corre al abrir). */
    private boolean save() {
        try {
            manager.saveData();
            return true;
        } catch (VaultException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void lock() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(inactivityListener);
        saveWindowState();
        manager.lock();
        dispose();
        App.showLogin();
    }

    private void shutdown() {
        saveWindowState();
        manager.lock();
        System.exit(0);
    }
}
