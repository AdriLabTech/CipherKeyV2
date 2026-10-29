package dev.view;

import dev.model.PasswordEntry;
import dev.security.Totp;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/** Alta y edición de una entrada. */
public class EntryDialog extends JDialog {
    private final PasswordEntry original;
    private final JTextField titleField = new JTextField();
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = Ui.passwordField(20);
    private final JTextField urlField = new JTextField();
    private final JTextArea notesArea = new JTextArea(4, 20);
    private final JTextField totpField = new JTextField();
    private final JLabel error = new JLabel(" ");
    private final JCheckBox favoriteChk = new JCheckBox("Marcar como favorita");
    private final StrengthMeter meter = new StrengthMeter();
    private boolean confirmed;

    /** @param entry entrada a editar, o {@code null} para una nueva */
    public EntryDialog(Frame parent, PasswordEntry entry, List<String> categories) {
        super(parent, entry == null ? "Nueva entrada" : "Editar entrada", true);
        Ui.configureDialog(this);
        this.original = entry;

        titleField.putClientProperty("JTextField.placeholderText", "Por ejemplo: GitHub");
        usernameField.putClientProperty("JTextField.placeholderText", "usuario o correo");
        urlField.putClientProperty("JTextField.placeholderText", "https://…");
        totpField.putClientProperty("JTextField.placeholderText", "Secreto en base32 o enlace otpauth://");
        error.putClientProperty("FlatLaf.style", "foreground: $App.danger");
        error.putClientProperty("FlatLaf.styleClass", "small");
        categoryBox.setEditable(true);
        categoryBox.addItem("");
        categories.forEach(categoryBox::addItem);
        categoryBox.putClientProperty("JTextField.placeholderText", "Sin categoría");
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.putClientProperty("JComponent.roundRect", true);

        JButton generate = Ui.iconButton(Icons.get("refresh"), "Generar contraseña", e -> onGenerate());
        generate.putClientProperty("JButton.buttonType", "roundRect");
        generate.setFocusable(true);
        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        passwordRow.setOpaque(false);
        passwordRow.add(passwordField, BorderLayout.CENTER);
        passwordRow.add(generate, BorderLayout.EAST);

        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { meter.update(passwordField.getPassword()); }
            public void removeUpdate(DocumentEvent e) { meter.update(passwordField.getPassword()); }
            public void changedUpdate(DocumentEvent e) { }
        });
        totpField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { clearTotpError(); }
            public void removeUpdate(DocumentEvent e) { clearTotpError(); }
            public void changedUpdate(DocumentEvent e) { }
        });
        titleField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { Ui.clearError(titleField); }
            public void removeUpdate(DocumentEvent e) { Ui.clearError(titleField); }
            public void changedUpdate(DocumentEvent e) { }
        });

        if (entry != null) {
            titleField.setText(entry.getTitle());
            categoryBox.setSelectedItem(entry.getCategory() == null ? "" : entry.getCategory());
            usernameField.setText(entry.getUsername());
            passwordField.setText(entry.getPassword());
            urlField.setText(entry.getUrl());
            notesArea.setText(entry.getNotes());
            favoriteChk.setSelected(entry.isFavorite());
            totpField.setText(entry.getTotpSecret());
        }
        meter.update(passwordField.getPassword());

        JPanel top = new JPanel(new GridLayout(1, 2, 14, 0));
        top.setOpaque(false);
        top.add(Ui.labeled("Nombre *", titleField));
        top.add(Ui.labeled("Categoría", categoryBox));

        VBox body = new VBox();
        body.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        body.row(Ui.title(getTitle()), 0);
        body.row(top, 16);
        body.row(Ui.labeled("Usuario", usernameField), 12);
        body.row(Ui.labeled("Contraseña", passwordRow), 12);
        body.row(meter, 6);
        body.row(Ui.labeled("Sitio web", urlField), 12);
        body.row(Ui.labeled("Clave 2FA (TOTP, opcional)", totpField), 12);
        body.row(Ui.labeled("Notas", notesScroll), 12);
        body.row(favoriteChk, 10);
        body.row(error, 6);

        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Guardar");
        save.addActionListener(e -> onSave());
        getRootPane().setDefaultButton(save);
        body.row(Ui.buttonBar(cancel, save), 10);

        setContentPane(body);
        setMinimumSize(new Dimension(500, 0));
        pack();
        setLocationRelativeTo(parent);
    }


    private void clearTotpError() {
        Ui.clearError(totpField);
        error.setText(" ");
    }

    private void onGenerate() {
        PasswordGeneratorDialog dlg = new PasswordGeneratorDialog(this, true);
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            passwordField.setText(dlg.getGeneratedPassword());
        }
    }

    private void onSave() {
        if (titleField.getText().isBlank()) {
            Ui.markError(titleField);
            return;
        }
        String totp = totpField.getText().trim();
        if (!totp.isEmpty()) {
            try {
                Totp.parse(totp);
            } catch (IllegalArgumentException ex) {
                error.setText("Clave 2FA no válida: " + ex.getMessage());
                Ui.markError(totpField);
                return;
            }
        }
        confirmed = true;
        dispose();
    }

    public boolean showDialog() {
        setVisible(true);
        return confirmed;
    }

    /** Nueva entrada con los datos del formulario, conservando id, fechas de creación y favorito de la original. */
    public PasswordEntry getEntry() {
        PasswordEntry e = new PasswordEntry();
        e.setTitle(titleField.getText().trim());
        Object cat = categoryBox.getEditor().getItem();
        e.setCategory(cat == null || cat.toString().isBlank() ? null : cat.toString().trim());
        e.setUsername(usernameField.getText().trim());
        e.setPassword(new String(passwordField.getPassword()));
        e.setUrl(urlField.getText().trim());
        e.setNotes(notesArea.getText().trim());
        e.setFavorite(favoriteChk.isSelected());
        String totp = totpField.getText().trim();
        e.setTotpSecret(totp.isEmpty() ? null : totp);
        if (original != null) {
            e.setId(original.getId());
            e.setCreated(original.getCreated());
        }
        return e;
    }
}
