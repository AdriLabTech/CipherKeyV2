package dev.view;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends JDialog {
    public AboutDialog(Frame parent) {
        super(parent, "Acerca de", true);
        JTextArea textArea = new JTextArea(
                "Gestor de Contraseñas Seguro\n" +
                        "Versión 2.0 (Mejorada)\n\n" +
                        "Cifrado AES-256 GCM con PBKDF2 (300k iteraciones)\n" +
                        "Limpieza de portapapeles automática\n" +
                        "Protección contra fuerza bruta\n" +
                        "Cierre por inactividad\n" +
                        "© 2026"
        );
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        add(textArea, BorderLayout.CENTER);
        JButton okButton = new JButton("Aceptar");
        okButton.addActionListener(e -> dispose());
        JPanel panel = new JPanel();
        panel.add(okButton);
        add(panel, BorderLayout.SOUTH);
        setSize(400, 250);
        setLocationRelativeTo(parent);
    }
}
