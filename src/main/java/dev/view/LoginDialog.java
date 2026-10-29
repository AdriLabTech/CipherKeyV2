package dev.view;

import dev.persistence.Vault;
import dev.persistence.VaultException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class LoginDialog extends JDialog {
    private JTextField pathField;
    private JPasswordField passwordField;
    private boolean succeeded;
    private Path selectedFile;

    public LoginDialog(Frame parent) {
        super(parent, "Iniciar sesión", true);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Campo de ruta del archivo
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Archivo de bóveda (.ckey):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        pathField = new JTextField(30);
        panel.add(pathField, gbc);

        // Botón Examinar
        gbc.gridx = 3; gbc.gridwidth = 1;
        JButton browseButton = new JButton("Examinar...");
        browseButton.addActionListener(this::onBrowse);
        panel.add(browseButton, gbc);

        // Botón Nueva bóveda
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        JButton newVaultButton = new JButton("Nueva bóveda");
        newVaultButton.addActionListener(this::onNewVault);
        panel.add(newVaultButton, gbc);

        // Campo de contraseña
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Contraseña maestra:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);

        // Botones Aceptar/Cancelar
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("Aceptar");
        JButton cancelButton = new JButton("Cancelar");
        okButton.addActionListener(this::onOk);
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        getRootPane().setDefaultButton(okButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void onBrowse(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de bóveda (*.ckey)", "ckey"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onNewVault(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de bóveda (*.ckey)", "ckey"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        String path = file.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".ckey")) {
            file = new File(path + ".ckey");
        }

        JPasswordField pwd1 = new JPasswordField(20);
        JPasswordField pwd2 = new JPasswordField(20);
        JPanel pwdPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        pwdPanel.add(new JLabel("Nueva contraseña maestra:"));
        pwdPanel.add(pwd1);
        pwdPanel.add(new JLabel("Confirmar:"));
        pwdPanel.add(pwd2);

        int pwdResult = JOptionPane.showConfirmDialog(this, pwdPanel,
                "Crear nueva bóveda", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (pwdResult != JOptionPane.OK_OPTION) {
            return;
        }

        char[] pass1 = pwd1.getPassword();
        char[] pass2 = pwd2.getPassword();
        try {
            if (pass1.length == 0) {
                JOptionPane.showMessageDialog(this, "La contraseña no puede estar vacía.");
                return;
            }
            if (!Arrays.equals(pass1, pass2)) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.");
                return;
            }
            Vault.create(file.toPath(), pass1).close();
            pathField.setText(file.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Bóveda creada correctamente.");
        } catch (VaultException ex) {
            JOptionPane.showMessageDialog(this, "Error al crear la bóveda: " + ex.getMessage());
        } finally {
            Arrays.fill(pass1, (char) 0);
            Arrays.fill(pass2, (char) 0);
        }
    }

    private void onOk(ActionEvent e) {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un archivo de bóveda.");
            return;
        }

        Path file = Paths.get(path);
        if (!Files.exists(file)) {
            JOptionPane.showMessageDialog(this, "El archivo no existe.");
            return;
        }

        selectedFile = file;
        succeeded = true;
        dispose();
    }

    public boolean showDialog() {
        setVisible(true);
        return succeeded;
    }

    public Path getSelectedFile() {
        return selectedFile;
    }

    /** Copia de la contraseña escrita; el llamador debe borrarla. */
    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public void clearPassword() {
        passwordField.setText("");
    }
}
