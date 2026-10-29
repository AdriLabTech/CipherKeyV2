package dev.view;

import dev.model.PasswordEntry;

import javax.swing.*;
import java.awt.*;

public class EntryDialog extends JDialog {
    private EntryFormPanel formPanel;
    private boolean confirmed;

    public EntryDialog(Frame parent, String title, PasswordEntry entry) {
        super(parent, title, true);
        formPanel = new EntryFormPanel(parent);
        formPanel.setEntry(entry);

        JButton okButton = new JButton("Aceptar");
        JButton cancelButton = new JButton("Cancelar");

        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    public boolean showDialog() {
        setVisible(true);
        return confirmed;
    }

    public PasswordEntry getEntry() {
        return formPanel.getEntry();
    }
}
