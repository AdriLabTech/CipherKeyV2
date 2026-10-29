package dev.view;

import com.formdev.flatlaf.FlatClientProperties;
import dev.model.PasswordEntry;
import dev.security.Totp;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Panel derecho con los datos de la entrada seleccionada y sus acciones. */
public class DetailPanel extends JPanel {
    /** Acciones que el panel delega en la ventana principal. */
    public interface Actions {
        void edit();
        void delete();
        void toggleFavorite();
        void copyUsername();
        void copyPassword();
        void openUrl();
        void copyTotp();
        void showHistory();
    }

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es"));
    private static final String MASK = "••••••••••••";

    private final Actions actions;
    private final CardLayout cards = new CardLayout();

    private final JLabel avatar = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel category = Ui.muted(" ");
    private final JButton favorite = Ui.iconButton(Icons.get("star", 18), "Marcar como favorita", e -> {});
    private final JLabel username = new JLabel();
    private final JLabel password = new JLabel();
    private final JButton reveal = Ui.iconButton(Icons.get("eye"), "Mostrar contraseña", e -> {});
    private final StrengthMeter meter = new StrengthMeter();
    private final JButton urlLink = new JButton();
    private final JTextArea notes = new JTextArea();
    private final JLabel dates = Ui.muted(" ");

    private final JLabel totpCode = new JLabel(" ");
    private final JProgressBar totpBar = new JProgressBar(0, 100);
    private final JPanel totpRow;
    private final JButton historyBtn = new JButton("Historial", Icons.get("clock"));
    private final Timer totpTimer = new Timer(1000, e -> tickTotp());
    private Totp totp;

    private boolean revealed;
    private PasswordEntry current;

    public DetailPanel(Actions actions) {
        super(new BorderLayout());
        this.actions = actions;
        setLayout(cards);
        setPreferredSize(new Dimension(340, 0));

        totpCode.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        totpBar.setPreferredSize(new Dimension(60, 5));
        totpBar.setBorderPainted(false);
        JPanel codeWithBar = new JPanel(new BorderLayout(0, 4));
        codeWithBar.setOpaque(false);
        codeWithBar.add(totpCode, BorderLayout.CENTER);
        codeWithBar.add(totpBar, BorderLayout.SOUTH);
        totpRow = field("Código 2FA", codeWithBar, Ui.iconButton(Icons.get("copy"), "Copiar código", e -> actions.copyTotp()));
        historyBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        historyBtn.putClientProperty(FlatClientProperties.STYLE, "margin: 0,0,0,0");
        historyBtn.setHorizontalAlignment(SwingConstants.LEFT);
        historyBtn.setFocusable(false);
        historyBtn.addActionListener(e -> actions.showHistory());

        add(buildEmpty(), "empty");
        add(buildContent(), "content");
        cards.show(this, "empty");
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(UIManager.getColor("App.card.background"));
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        g.setColor(UIManager.getColor("App.card.border"));
        g.fillRect(0, 0, 1, getHeight()); // filete que separa de la tabla
    }

    private JComponent buildEmpty() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel(Icons.get("key", 40));
        JLabel text = Ui.muted("Selecciona una entrada para ver sus datos");
        icon.setAlignmentX(CENTER_ALIGNMENT);
        text.setAlignmentX(CENTER_ALIGNMENT);
        box.add(icon);
        box.add(Box.createVerticalStrut(10));
        box.add(text);
        p.add(box);
        return p;
    }

    private JComponent buildContent() {
        VBox.Scrolling col = new VBox.Scrolling();
        col.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        favorite.addActionListener(e -> actions.toggleFavorite());

        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(category);
        JPanel titlesWrap = new JPanel(new GridBagLayout());
        titlesWrap.setOpaque(false);
        titlesWrap.add(titles, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        header.add(avatar, BorderLayout.WEST);
        header.add(titlesWrap, BorderLayout.CENTER);
        header.add(favorite, BorderLayout.EAST);

        reveal.addActionListener(e -> {
            revealed = !revealed;
            renderPassword();
        });
        password.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        urlLink.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        urlLink.putClientProperty(FlatClientProperties.STYLE, "foreground: $Component.accentColor; margin: 0,0,0,0");
        urlLink.setHorizontalAlignment(SwingConstants.LEFT);
        urlLink.setFocusable(false);
        urlLink.addActionListener(e -> actions.openUrl());

        notes.setEditable(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setOpaque(false);
        notes.setBorder(null);

        JButton edit = new JButton("Editar", Icons.get("edit"));
        edit.addActionListener(e -> actions.edit());
        JButton delete = Ui.danger("Eliminar", Icons.colored("trash", 16, () -> UIManager.getColor("App.danger")), e -> actions.delete());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.add(edit);
        buttons.add(delete);

        col.row(header, 0);
        col.row(field("Usuario", username, Ui.iconButton(Icons.get("copy"), "Copiar usuario (Ctrl+U)", e -> actions.copyUsername())), 26);
        col.row(field("Contraseña", password, reveal,
                Ui.iconButton(Icons.get("copy"), "Copiar contraseña (Ctrl+B)", e -> actions.copyPassword())), 16);
        col.row(meter, 2);
        col.row(historyBtn, 4);
        col.row(totpRow, 16);
        col.row(field("Sitio web", urlLink, Ui.iconButton(Icons.get("external-link"), "Abrir en el navegador", e -> actions.openUrl())), 16);
        col.row(field("Notas", notes), 16);
        col.glue();
        col.row(dates, 16);
        col.row(buttons, 14);

        JScrollPane scroll = new JScrollPane(col, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        return scroll;
    }

    /** Fila: etiqueta pequeña arriba, valor a la izquierda y botones de acción a la derecha. */
    private static JPanel field(String label, JComponent value, JComponent... actionButtons) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.add(Ui.muted(label), BorderLayout.NORTH);
        JPanel line = new JPanel(new BorderLayout(4, 0));
        line.setOpaque(false);
        line.add(value, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        btns.setOpaque(false);
        for (JComponent b : actionButtons) btns.add(b);
        line.add(btns, BorderLayout.EAST);
        row.add(line, BorderLayout.CENTER);
        return row;
    }

    public void show(PasswordEntry entry) {
        current = entry;
        revealed = false;
        totpTimer.stop();
        totp = null;
        if (entry == null) {
            cards.show(this, "empty");
            return;
        }
        avatar.setIcon(new Icons.Avatar(entry.getTitle(), 44));
        title.setText(entry.getTitle());
        String cat = entry.getCategory();
        category.setText(cat == null || cat.isBlank() ? "Sin categoría" : cat);
        favorite.setIcon(entry.isFavorite()
                ? Icons.colored("star-filled", 18, () -> UIManager.getColor("App.warning"))
                : Icons.get("star", 18));
        favorite.setToolTipText(entry.isFavorite() ? "Quitar de favoritas" : "Marcar como favorita");
        username.setText(orDash(entry.getUsername()));
        meter.update(entry.getPassword());

        boolean hasUrl = entry.getUrl() != null && !entry.getUrl().isBlank();
        urlLink.setText(hasUrl ? entry.getUrl() : "—");
        urlLink.setEnabled(hasUrl);
        boolean hasNotes = entry.getNotes() != null && !entry.getNotes().isBlank();
        notes.setText(hasNotes ? entry.getNotes() : "—");

        dates.setText("<html>Creada " + DATE_TIME.format(entry.getCreated()) + "<br>Modificada " + DATE_TIME.format(entry.getModified()) + "</html>");
        int historySize = entry.getHistory().size();
        historyBtn.setVisible(historySize > 0);
        historyBtn.setText("Historial de contraseñas (" + historySize + ")");

        boolean hasTotp = entry.getTotpSecret() != null && !entry.getTotpSecret().isBlank();
        totpRow.setVisible(hasTotp);
        if (hasTotp) {
            try {
                totp = Totp.parse(entry.getTotpSecret());
                tickTotp();
                totpTimer.start();
            } catch (IllegalArgumentException ex) {
                totpCode.setText("Clave no válida");
                totpBar.setValue(0);
            }
        }
        renderPassword();
        cards.show(this, "content");
        revalidate();
        repaint();
    }

    private void tickTotp() {
        if (totp == null) return;
        Instant now = Instant.now();
        String code = totp.code(now);
        int mid = code.length() / 2;
        totpCode.setText(code.substring(0, mid) + " " + code.substring(mid));
        int remaining = totp.secondsRemaining(now);
        totpBar.setValue(remaining * 100 / totp.period());
        totpBar.setForeground(UIManager.getColor(remaining <= 5 ? "App.danger" : "Component.accentColor"));
    }

    @Override
    public void removeNotify() {
        totpTimer.stop();
        super.removeNotify();
    }

    private void renderPassword() {
        boolean has = current != null && current.getPassword() != null && !current.getPassword().isEmpty();
        password.setText(!has ? "—" : revealed ? current.getPassword() : MASK);
        reveal.setIcon(Icons.get(revealed ? "eye-off" : "eye"));
        reveal.setToolTipText(revealed ? "Ocultar contraseña" : "Mostrar contraseña");
    }

    private static String orDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
