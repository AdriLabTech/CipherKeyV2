package dev.view;

import dev.security.PasswordAudit;
import dev.security.PasswordAudit.Finding;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/** Resultado de la auditoría de seguridad, agrupado por tipo de problema. */
public class AuditDialog extends JDialog {
    private final Consumer<Finding> onOpen;

    public AuditDialog(Frame parent, PasswordAudit.Report report, Consumer<Finding> onOpen) {
        super(parent, "Auditoría de seguridad", true);
        Ui.configureDialog(this);
        this.onOpen = onOpen;

        VBox body = new VBox();
        body.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        body.row(Ui.title("Auditoría de seguridad"), 0);

        if (report.isClean()) {
            JLabel ok = new JLabel("Todo en orden: no hay contraseñas débiles, repetidas ni antiguas.",
                    Icons.colored("check", 20, () -> UIManager.getColor("App.success")), SwingConstants.LEADING);
            ok.setIconTextGap(10);
            body.row(ok, 18);
        } else {
            body.row(Ui.muted(report.total() + (report.total() == 1 ? " aviso" : " avisos")
                    + " · doble clic para ir a la entrada"), 2);
            JTabbedPane tabs = new JTabbedPane();
            addTab(tabs, "Débiles", report.weak());
            addTab(tabs, "Repetidas", report.reused());
            addTab(tabs, "Antiguas", report.old());
            addTab(tabs, "Sin contraseña", report.empty());
            tabs.setPreferredSize(new Dimension(520, 300));
            body.row(tabs, 14);
        }

        JButton close = new JButton("Cerrar");
        close.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(close);
        body.row(Ui.buttonBar(close), 16);

        setContentPane(body);
        pack();
        setLocationRelativeTo(parent);
    }

    private void addTab(JTabbedPane tabs, String name, List<Finding> findings) {
        if (findings.isEmpty()) return;
        DefaultListModel<Finding> model = new DefaultListModel<>();
        findings.forEach(model::addElement);
        JList<Finding> list = new JList<>(model);
        list.setFixedCellHeight(54);
        list.setCellRenderer((l, f, i, selected, focus) -> {
            JPanel p = new JPanel(new BorderLayout(12, 0));
            p.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            p.setBackground(selected ? l.getSelectionBackground() : l.getBackground());
            JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
            text.setOpaque(false);
            JLabel title = new JLabel(f.entry().getTitle());
            title.setForeground(selected ? l.getSelectionForeground() : l.getForeground());
            JLabel detail = Ui.muted(f.detail());
            text.add(title);
            text.add(detail);
            JPanel textWrap = new JPanel(new GridBagLayout());
            textWrap.setOpaque(false);
            textWrap.add(text, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            p.add(new JLabel(new Icons.Avatar(f.entry().getTitle(), 30)), BorderLayout.WEST);
            p.add(textWrap, BorderLayout.CENTER);
            return p;
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) open(list.getSelectedValue());
            }
        });
        list.registerKeyboardAction(e -> {
            if (list.getSelectedValue() != null) open(list.getSelectedValue());
        }, KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        tabs.addTab(name + " (" + findings.size() + ")", scroll);
    }

    private void open(Finding f) {
        dispose();
        onOpen.accept(f);
    }
}
