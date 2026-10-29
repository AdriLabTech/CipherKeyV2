package dev.view;

import com.formdev.flatlaf.FlatClientProperties;
import dev.persistence.AppPrefs;
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

/** Pantalla de acceso: elegir (o crear) una bóveda y escribir la clave maestra. */
public class LoginDialog extends JDialog {
    private final JComboBox<String> pathBox = new JComboBox<>();
    private final JPasswordField passwordField = Ui.passwordField(20);
    private final JLabel errorLabel = new JLabel(" ");
    private boolean succeeded;
    private Path selectedFile;

    /**
     * @param preselected bóveda a mostrar de entrada (p. ej. la que falló); si es null se usa la más reciente
     * @param error       mensaje inicial a mostrar bajo la contraseña, o null
     */
    public LoginDialog(Frame parent, Path preselected, String error) {
        super(parent, "CipherKey", true);
        Ui.configureDialog(this);

        for (Path p : AppPrefs.recentVaults()) {
            if (Files.exists(p)) pathBox.addItem(p.toString());
        }
        pathBox.setEditable(true);
        pathBox.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Selecciona tu bóveda (.ckey)");
        if (preselected != null) {
            pathBox.setSelectedItem(preselected.toString());
        } else if (pathBox.getItemCount() > 0) {
            pathBox.setSelectedIndex(0);
        } else {
            pathBox.setSelectedItem("");
        }

        JButton browse = new JButton("Examinar…");
        browse.addActionListener(this::onBrowse);
        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.add(pathBox, BorderLayout.CENTER);
        pathRow.add(browse, BorderLayout.EAST);

        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tu clave maestra");
        errorLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: $App.danger");
        errorLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        if (error != null) errorLabel.setText(error);

        JButton unlock = Ui.primary("Desbloquear", Icons.white("lock", 16), this::onOk);
        unlock.putClientProperty(FlatClientProperties.STYLE_CLASS, "large");
        getRootPane().setDefaultButton(unlock);

        JButton create = new JButton("Crear una bóveda nueva");
        create.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        create.putClientProperty(FlatClientProperties.STYLE, "foreground: $Component.accentColor");
        create.addActionListener(this::onNewVault);

        JLabel logo = new JLabel(Icons.logo(250));
        JLabel tagline = Ui.muted("Tu bóveda cifrada · 100 % offline");

        JButton theme = Ui.iconButton(Icons.get(ThemeManager.isDark() ? "sun" : "moon", 18), "Cambiar tema", e -> {
            ThemeManager.toggle();
            ((JButton) e.getSource()).setIcon(Icons.get(ThemeManager.isDark() ? "sun" : "moon", 18));
        });
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setOpaque(false);
        topBar.add(theme);

        VBox body = new VBox();
        body.setBorder(BorderFactory.createEmptyBorder(10, 34, 26, 34));
        body.row(topBar, 0);
        body.centered(logo, 0);
        body.centered(tagline, 2);
        body.row(Ui.labeled("Bóveda", pathRow), 26);
        body.row(Ui.labeled("Contraseña maestra", passwordField), 14);
        body.row(errorLabel, 6);
        body.row(unlock, 10);
        body.centered(create, 8);

        setContentPane(body);
        setPreferredSize(new Dimension(460, getPreferredSize().height));
        pack();
        setLocationRelativeTo(parent);

        boolean hasPath = !pathText().isEmpty();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                (hasPath ? passwordField : pathBox).requestFocusInWindow();
            }
        });
    }



    private String pathText() {
        Object item = pathBox.getEditor().getItem();
        return item == null ? "" : item.toString().trim();
    }

    private void onBrowse(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Bóvedas CipherKey (*.ckey)", "ckey"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathBox.setSelectedItem(fileChooser.getSelectedFile().getAbsolutePath());
            passwordField.requestFocusInWindow();
        }
    }

    private void onNewVault(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Dónde guardar la nueva bóveda");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Bóvedas CipherKey (*.ckey)", "ckey"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".ckey")) {
            file = new File(file.getAbsolutePath() + ".ckey");
        }
        if (file.exists()) {
            errorLabel.setText("Ya existe un archivo con ese nombre.");
            return;
        }

        CreateVaultDialog dlg = new CreateVaultDialog(this, file.getName());
        char[] password = dlg.showDialog();
        if (password == null) return;
        try {
            Vault.create(file.toPath(), password).close();
            pathBox.setSelectedItem(file.getAbsolutePath());
            errorLabel.setText(" ");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            Toast.show(this, "Bóveda creada · introduce tu clave para entrar");
        } catch (VaultException ex) {
            errorLabel.setText("No se pudo crear la bóveda: " + ex.getMessage());
        } finally {
            Arrays.fill(password, (char) 0);
        }
    }

    private void onOk(ActionEvent e) {
        String path = pathText();
        if (path.isEmpty()) {
            errorLabel.setText("Selecciona un archivo de bóveda.");
            return;
        }
        Path file = Paths.get(path);
        if (!Files.isRegularFile(file)) {
            errorLabel.setText("El archivo no existe.");
            return;
        }
        if (passwordField.getPassword().length == 0) {
            errorLabel.setText("Escribe tu contraseña maestra.");
            passwordField.requestFocusInWindow();
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
