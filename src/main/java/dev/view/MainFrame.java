package dev.view;

import dev.controller.PasswordManager;
import dev.model.PasswordEntry;
import dev.security.SecureClipboard;
import dev.App;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class MainFrame extends JFrame {
    private PasswordManager manager;
    private JTable entryTable;
    private PasswordTableModel tableModel;
    private InactivityListener inactivityListener;
    private Timer autoLockTimer;

    private JButton themeButton;

    public MainFrame(PasswordManager manager) {
        this.manager = manager;
        setTitle("Gestor de Contraseñas");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        setSize(800, 500);
        setLocationRelativeTo(null);

        initMenu();
        initComponents();
        loadEntries();

        // Iniciar listener de inactividad (5 minutos)
        inactivityListener = new InactivityListener(5 * 60, this::lock);
        Toolkit.getDefaultToolkit().addAWTEventListener(inactivityListener,
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);

        // Establecer tema claro por defecto y actualizar el botón
        ThemeManager.setLightTheme();
        SwingUtilities.updateComponentTreeUI(this);
        themeButton.setText(ThemeManager.isDarkTheme() ? "Light" : "Dark");
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Archivo");
        JMenuItem lockItem = new JMenuItem("Cerrar sesión");
        lockItem.addActionListener(e -> lock());
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> shutdown());
        fileMenu.add(lockItem);
        fileMenu.add(exitItem);

        JMenu toolsMenu = new JMenu("Herramientas");
        JMenuItem generateItem = new JMenuItem("Generar contraseña");
        generateItem.addActionListener(e -> openGenerator());
        JMenuItem settingsItem = new JMenuItem("Cambiar contraseña maestra");
        settingsItem.addActionListener(e -> openSettings());
        toolsMenu.add(generateItem);
        toolsMenu.add(settingsItem);

        JMenu helpMenu = new JMenu("Ayuda");
        JMenuItem aboutItem = new JMenuItem("Acerca de");
        aboutItem.addActionListener(e -> new AboutDialog(this).setVisible(true));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        // Espaciador para empujar el botón a la derecha
        menuBar.add(Box.createHorizontalGlue());

        // Botón de cambio de tema
        themeButton = new JButton("Light"); // Inicialmente claro
        themeButton.addActionListener(e -> toggleTheme());
        menuBar.add(themeButton);

        setJMenuBar(menuBar);
    }

    private void toggleTheme() {
        ThemeManager.toggleTheme();
        themeButton.setText(ThemeManager.isDarkTheme() ? "Light" : "Dark");
    }

    private void initComponents() {
        tableModel = new PasswordTableModel(manager.getEntries());
        entryTable = new JTable(tableModel);
        entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        entryTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Doble clic para editar
        entryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    editSelectedEntry();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(entryTable);

        // Botones
        JButton addButton = new JButton("Añadir");
        JButton editButton = new JButton("Editar");
        JButton deleteButton = new JButton("Eliminar");
        JButton copyUserButton = new JButton("Copiar usuario");
        JButton copyPassButton = new JButton("Copiar contraseña");

        addButton.addActionListener(this::onAdd);
        editButton.addActionListener(e -> editSelectedEntry());
        deleteButton.addActionListener(this::onDelete);
        copyUserButton.addActionListener(e -> copyUsername());
        copyPassButton.addActionListener(e -> copyPassword());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(copyUserButton);
        buttonPanel.add(copyPassButton);

        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadEntries() {
        tableModel.setEntries(manager.getEntries());
    }

    private void onAdd(ActionEvent e) {
        EntryDialog dialog = new EntryDialog(this, "Nueva entrada", null);
        if (dialog.showDialog()) {
            PasswordEntry newEntry = dialog.getEntry();
            manager.addEntry(newEntry);
            tableModel.fireTableDataChanged();
            saveData();
        }
    }

    private void editSelectedEntry() {
        int selectedRow = entryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una entrada para editar.");
            return;
        }
        PasswordEntry entry = manager.getEntries().get(selectedRow);
        EntryDialog dialog = new EntryDialog(this, "Editar entrada", entry);
        if (dialog.showDialog()) {
            PasswordEntry updated = dialog.getEntry();
            updated.setId(entry.getId()); // conservar ID
            updated.setCreated(entry.getCreated());
            manager.updateEntry(updated);
            tableModel.fireTableDataChanged();
            saveData();
        }
    }

    private void onDelete(ActionEvent e) {
        int selectedRow = entryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una entrada para eliminar.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de eliminar esta entrada?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            PasswordEntry entry = manager.getEntries().get(selectedRow);
            manager.deleteEntry(entry.getId());
            tableModel.fireTableDataChanged();
            saveData();
        }
    }

    private void copyUsername() {
        int row = entryTable.getSelectedRow();
        if (row == -1) return;
        String username = manager.getEntries().get(row).getUsername();
        SecureClipboard.copyToClipboard(username);
        JOptionPane.showMessageDialog(this, "Usuario copiado al portapapeles (se borrará en 30s)");
    }

    private void copyPassword() {
        int row = entryTable.getSelectedRow();
        if (row == -1) return;
        String password = manager.getEntries().get(row).getPassword();
        SecureClipboard.copyToClipboard(password);
        JOptionPane.showMessageDialog(this, "Contraseña copiada al portapapeles (se borrará en 30s)");
    }

    private void openGenerator() {
        PasswordGeneratorDialog dlg = new PasswordGeneratorDialog(this);
        dlg.setVisible(true);
    }

    private void openSettings() {
        SettingsDialog dlg = new SettingsDialog(this, manager);
        dlg.setVisible(true);
        // Después de cambiar contraseña, los datos ya están recifrados
    }

    private void saveData() {
        try {
            manager.saveData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage());
        }
    }

    private void lock() {
        // Quitar listener de inactividad
        Toolkit.getDefaultToolkit().removeAWTEventListener(inactivityListener);
        saveData();
        manager.lock();
        dispose();
        // Volver al login
        App.showLogin();
    }

    private void shutdown() {
        saveData();
        manager.lock();
        System.exit(0);
    }
}
