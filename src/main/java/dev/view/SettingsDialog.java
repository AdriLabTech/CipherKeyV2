package dev.view;

import dev.controller.PasswordManager;
import dev.persistence.VaultException;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class SettingsDialog extends JDialog {
    private PasswordManager manager;
    private JPasswordField currentPwd, newPwd, confirmPwd;

    public SettingsDialog(Frame parent, PasswordManager manager) {
        super(parent, "Configuración", true);
        this.manager = manager;

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Contraseña actual:"), gbc);
        gbc.gridx = 1;
        currentPwd = new JPasswordField(20);
        panel.add(currentPwd, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Nueva contraseña:"), gbc);
        gbc.gridx = 1;
        newPwd = new JPasswordField(20);
        panel.add(newPwd, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Confirmar nueva:"), gbc);
        gbc.gridx = 1;
        confirmPwd = new JPasswordField(20);
        panel.add(confirmPwd, gbc);

        JButton changeBtn = new JButton("Cambiar contraseña maestra");
        changeBtn.addActionListener(e -> changeMasterPassword());

        JButton closeBtn = new JButton("Cerrar");
        closeBtn.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(changeBtn);
        buttonPanel.add(closeBtn);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private void changeMasterPassword() {
        char[] oldPwd = currentPwd.getPassword();
        char[] newPwdChars = newPwd.getPassword();
        char[] confirm = confirmPwd.getPassword();

        if (oldPwd.length == 0 || newPwdChars.length == 0 || confirm.length == 0) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios");
            return;
        }
        if (!Arrays.equals(newPwdChars, confirm)) {
            JOptionPane.showMessageDialog(this, "Las nuevas contraseñas no coinciden");
            return;
        }
        try {
            manager.changeMasterPassword(oldPwd, newPwdChars);
            JOptionPane.showMessageDialog(this, "Contraseña maestra cambiada correctamente");
            dispose();
        } catch (VaultException.InvalidPassword ex) {
            JOptionPane.showMessageDialog(this, "La contraseña actual es incorrecta");
        } catch (VaultException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        } finally {
            // Limpiar arrays
            Arrays.fill(oldPwd, (char) 0);
            Arrays.fill(newPwdChars, (char) 0);
            Arrays.fill(confirm, (char) 0);
        }
    }
}
