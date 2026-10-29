package dev.view;

import dev.security.PasswordGenerator;
import dev.security.SecureClipboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class PasswordGeneratorDialog extends JDialog {
    private final PasswordGenerator generator = new PasswordGenerator();
    private final JCheckBox upperChk = new JCheckBox("Mayúsculas (A-Z)", true);
    private final JCheckBox lowerChk = new JCheckBox("Minúsculas (a-z)", true);
    private final JCheckBox digitsChk = new JCheckBox("Números (0-9)", true);
    private final JCheckBox symbolsChk = new JCheckBox("Símbolos (!@#…)", true);
    private final JCheckBox ambiguousChk = new JCheckBox("Evitar caracteres ambiguos (I l 1 O 0)");
    private final JSlider lengthSlider = new JSlider(8, 64, 20);
    private final JLabel lengthLabel = new JLabel();
    private final JTextField resultField = new JTextField();
    private final StrengthMeter meter = new StrengthMeter();

    private String lastGenerated = "";
    private boolean accepted;

    /** @param selectMode true si se abre desde un formulario y debe devolver la contraseña ("Usar"); false para copiar. */
    public PasswordGeneratorDialog(Window parent, boolean selectMode) {
        super(parent, "Generador de contraseñas", ModalityType.APPLICATION_MODAL);
        Ui.configureDialog(this);

        resultField.setEditable(false);
        resultField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        resultField.putClientProperty("FlatLaf.style", "margin: 8,12,8,4");
        JToolBar tools = new JToolBar();
        tools.setFloatable(false);
        tools.setBorder(null);
        tools.add(Ui.iconButton(Icons.get("refresh"), "Generar otra", e -> generate()));
        tools.add(Ui.iconButton(Icons.get("copy"), "Copiar", e -> copy()));
        resultField.putClientProperty("JTextField.trailingComponent", tools);

        lengthSlider.addChangeListener(e -> {
            lengthLabel.setText(lengthSlider.getValue() + " caracteres");
            if (!lengthSlider.getValueIsAdjusting()) generate();
        });
        for (JCheckBox c : new JCheckBox[]{upperChk, lowerChk, digitsChk, symbolsChk, ambiguousChk}) {
            c.addItemListener(this::onOptionChanged);
        }

        JPanel lengthRow = new JPanel(new BorderLayout(12, 0));
        lengthRow.setOpaque(false);
        lengthRow.add(lengthSlider, BorderLayout.CENTER);
        lengthRow.add(lengthLabel, BorderLayout.EAST);
        lengthLabel.setPreferredSize(new Dimension(96, 20));

        JPanel classes = new JPanel(new GridLayout(2, 2, 12, 6));
        classes.setOpaque(false);
        classes.add(upperChk);
        classes.add(lowerChk);
        classes.add(digitsChk);
        classes.add(symbolsChk);

        VBox body = new VBox();
        body.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        body.row(Ui.title("Generador de contraseñas"), 0);
        body.row(resultField, 16);
        body.row(meter, 10);
        body.row(Ui.muted("Longitud"), 18);
        body.row(lengthRow, 4);
        body.row(Ui.muted("Caracteres"), 14);
        body.row(classes, 6);
        body.row(ambiguousChk, 8);

        JButton close = new JButton(selectMode ? "Cancelar" : "Cerrar");
        close.addActionListener(e -> dispose());
        JButton action;
        if (selectMode) {
            action = new JButton("Usar contraseña");
            action.addActionListener(e -> {
                accepted = true;
                dispose();
            });
        } else {
            action = new JButton("Copiar", Icons.white("copy", 16));
            action.addActionListener(e -> copy());
        }
        getRootPane().setDefaultButton(action);
        body.row(Ui.buttonBar(close, action), 20);

        setContentPane(body);
        lengthLabel.setText(lengthSlider.getValue() + " caracteres");
        generate();
        setMinimumSize(new Dimension(440, 0));
        pack();
        setLocationRelativeTo(parent);
    }


    private void onOptionChanged(ItemEvent e) {
        // Siempre debe quedar al menos un tipo de carácter seleccionado.
        if (!upperChk.isSelected() && !lowerChk.isSelected() && !digitsChk.isSelected() && !symbolsChk.isSelected()) {
            ((JCheckBox) e.getSource()).setSelected(true);
            return;
        }
        generate();
    }

    private void generate() {
        generator.setIncludeUppercase(upperChk.isSelected());
        generator.setIncludeLowercase(lowerChk.isSelected());
        generator.setIncludeDigits(digitsChk.isSelected());
        generator.setIncludeSymbols(symbolsChk.isSelected());
        generator.setExcludeAmbiguous(ambiguousChk.isSelected());
        generator.setLength(lengthSlider.getValue());
        lastGenerated = generator.generate();
        resultField.setText(lastGenerated);
        resultField.setCaretPosition(0);
        meter.update(lastGenerated);
    }

    private void copy() {
        if (!lastGenerated.isEmpty()) {
            SecureClipboard.copyToClipboard(lastGenerated);
            Toast.show(this, "Copiada · se borrará en 30 s");
        }
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getGeneratedPassword() {
        return lastGenerated;
    }
}
