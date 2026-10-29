package dev.view;

import dev.model.PasswordEntry;
import dev.model.PasswordHistoryItem;
import dev.security.SecureClipboard;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Contraseñas anteriores de una entrada, con mostrar y copiar. */
public class HistoryDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es"));
    private static final String MASK = "••••••••••••";

    public HistoryDialog(Frame parent, PasswordEntry entry) {
        super(parent, "Historial de contraseñas", true);
        Ui.configureDialog(this);

        VBox list = new VBox();
        list.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        int gap = 0;
        for (PasswordHistoryItem item : entry.getHistory()) {
            list.row(buildRow(item), gap);
            gap = 10;
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(420, Math.min(280, 28 + entry.getHistory().size() * 62)));

        JButton close = new JButton("Cerrar");
        close.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(close);

        VBox body = new VBox();
        body.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        body.row(Ui.title(entry.getTitle()), 0);
        body.row(Ui.muted("Se guardan las últimas 10 contraseñas que dejaste de usar."), 2);
        body.row(scroll, 14);
        body.row(Ui.buttonBar(close), 16);
        setContentPane(body);
        pack();
        setLocationRelativeTo(parent);
    }

    private JComponent buildRow(PasswordHistoryItem item) {
        JLabel value = new JLabel(MASK);
        value.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JButton reveal = Ui.iconButton(Icons.get("eye"), "Mostrar", e -> {});
        reveal.addActionListener(e -> {
            boolean show = MASK.equals(value.getText());
            value.setText(show ? item.getPassword() : MASK);
            reveal.setIcon(Icons.get(show ? "eye-off" : "eye"));
        });
        JButton copy = Ui.iconButton(Icons.get("copy"), "Copiar", e -> {
            SecureClipboard.copyToClipboard(item.getPassword());
            Toast.show(this, "Copiada · se borrará en 30 s");
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttons.setOpaque(false);
        buttons.add(reveal);
        buttons.add(copy);
        JPanel line = new JPanel(new BorderLayout(4, 0));
        line.setOpaque(false);
        line.add(value, BorderLayout.CENTER);
        line.add(buttons, BorderLayout.EAST);

        VBox row = new VBox();
        row.row(Ui.muted("Dejó de usarse el " + DATE.format(item.getReplacedAt())), 0);
        row.row(line, 2);
        return row;
    }
}
