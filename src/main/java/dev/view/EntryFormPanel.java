package dev.view;

import dev.model.PasswordEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class EntryFormPanel extends JPanel {
    private JTextField titleField, usernameField, urlField, notesField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheck;
    private JButton generateButton;
    private Frame parentFrame; // para abrir diálogos modales

    public EntryFormPanel(Frame parent) {
        this.parentFrame = parent;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Título:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        titleField = new JTextField(20);
        add(titleField, gbc);

        // Usuario
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        usernameField = new JTextField(20);
        add(usernameField, gbc);

        // Contraseña
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1;
        passwordField = new JPasswordField(20);
        add(passwordField, gbc);

        // Botón generar
        generateButton = new JButton("Generar");
            generateButton.addActionListener(this::onGenerate);
        gbc.gridx = 2;
        add(generateButton, gbc);

        // Mostrar contraseña
        showPasswordCheck = new JCheckBox("Mostrar");
        showPasswordCheck.addActionListener(e -> {
            if (showPasswordCheck.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });
        gbc.gridx = 3; // nueva columna
        add(showPasswordCheck, gbc);

        // URL
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        add(new JLabel("URL:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        urlField = new JTextField(20);
        add(urlField, gbc);

        // Notas
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        add(new JLabel("Notas:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        notesField = new JTextField(20);
        add(notesField, gbc);
    }

    private void onGenerate(ActionEvent e) {
        PasswordGeneratorDialog dlg = new PasswordGeneratorDialog(parentFrame);
        dlg.setVisible(true);
        if(dlg.isAccepted()){
            String generated = dlg.getGeneratedPassword();
            if (generated != null && !generated.isEmpty()) {
                passwordField.setText(generated);

                // Opcional: copiar al portapapeles con temporizador
                // SecureClipboard.copyToClipboard(generated); // si se desea
            }
        }
    }

    public void setEntry(PasswordEntry entry) {
        if (entry != null) {
            titleField.setText(entry.getTitle());
            usernameField.setText(entry.getUsername());
            passwordField.setText(entry.getPassword());
            urlField.setText(entry.getUrl());
            notesField.setText(entry.getNotes());
        } else {
            clear();
        }
    }

    public void clear() {
        titleField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        urlField.setText("");
        notesField.setText("");
        showPasswordCheck.setSelected(false);
        passwordField.setEchoChar('•');
    }

    public PasswordEntry getEntry() {
        PasswordEntry entry = new PasswordEntry();
        entry.setTitle(titleField.getText().trim());
        entry.setUsername(usernameField.getText().trim());
        entry.setPassword(new String(passwordField.getPassword()));
        entry.setUrl(urlField.getText().trim());
        entry.setNotes(notesField.getText().trim());
        return entry;
    }
}