package dev.view;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Arrays;

/** Pide la clave maestra de una bóveda nueva (con confirmación y medidor de fuerza). */
public class CreateVaultDialog extends JDialog {
    static final int MIN_LENGTH = 8;

    private final JPasswordField pwd1 = Ui.passwordField(20);
    private final JPasswordField pwd2 = Ui.passwordField(20);
    private final StrengthMeter meter = new StrengthMeter();
    private final JLabel error = new JLabel(" ");
    private char[] result;

    public CreateVaultDialog(Window parent, String fileName) {
        super(parent, "Nueva bóveda", ModalityType.APPLICATION_MODAL);
        Ui.configureDialog(this);

        pwd1.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { meter.update(pwd1.getPassword()); }
            public void removeUpdate(DocumentEvent e) { meter.update(pwd1.getPassword()); }
            public void changedUpdate(DocumentEvent e) { }
        });
        error.putClientProperty(FlatClientProperties.STYLE, "foreground: $App.danger");
        error.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");

        JLabel warning = Ui.muted("<html>Si olvidas la clave maestra <b>no hay forma de recuperar</b> tus datos.<br>"
                + "Usa una frase larga que puedas recordar.</html>");

        VBox body = new VBox();
        body.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        body.row(Ui.title("Nueva bóveda"), 0);
        body.row(Ui.muted(fileName), 2);
        body.row(Ui.labeled("Clave maestra", pwd1), 16);
        body.row(meter, 6);
        body.row(Ui.labeled("Repite la clave", pwd2), 12);
        body.row(error, 8);
        body.row(warning, 4);

        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dispose());
        JButton create = new JButton("Crear bóveda");
        create.addActionListener(e -> onCreate());
        getRootPane().setDefaultButton(create);
        body.row(Ui.buttonBar(cancel, create), 18);

        setContentPane(body);
        setMinimumSize(new Dimension(430, 0));
        pack();
        setLocationRelativeTo(parent);
    }


    private void onCreate() {
        char[] a = pwd1.getPassword();
        char[] b = pwd2.getPassword();
        try {
            if (a.length < MIN_LENGTH) {
                error.setText("Mínimo " + MIN_LENGTH + " caracteres.");
                Ui.markError(pwd1);
                return;
            }
            if (!Arrays.equals(a, b)) {
                error.setText("Las claves no coinciden.");
                Ui.markError(pwd2);
                return;
            }
            result = a.clone();
            dispose();
        } finally {
            Arrays.fill(a, (char) 0);
            Arrays.fill(b, (char) 0);
        }
    }

    /** @return la clave elegida (el llamador debe borrarla) o {@code null} si se canceló */
    public char[] showDialog() {
        setVisible(true);
        return result;
    }
}
