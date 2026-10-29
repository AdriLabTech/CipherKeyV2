package dev.view;

import dev.security.PasswordGenerator;
import dev.security.SecureClipboard;

import javax.swing.*;
import java.awt.*;

public class PasswordGeneratorDialog extends JDialog {
    private PasswordGenerator generator;
    private JCheckBox upperChk, lowerChk, digitsChk, symbolsChk;
    private JSpinner lengthSpinner;
    private JTextField resultField;
    private JButton generateBtn, copyBtn;

    private String lastGenerated = "";
    private boolean accepted = false;

    public PasswordGeneratorDialog(Frame parent) {
        super(parent, "Generar contraseña", true);
        generator = new PasswordGenerator();

        upperChk = new JCheckBox("Mayúsculas", true);
        lowerChk = new JCheckBox("Minúsculas", true);
        digitsChk = new JCheckBox("Números", true);
        symbolsChk = new JCheckBox("Símbolos", true);
        lengthSpinner = new JSpinner(new SpinnerNumberModel(16, 4, 64, 1));
        resultField = new JTextField(20);
        resultField.setEditable(false);
        generateBtn = new JButton("Generar");
        copyBtn = new JButton("Copiar");

        generateBtn.addActionListener(e -> generate());
        copyBtn.addActionListener(e -> copy());

        JButton acceptBtn = new JButton("Aceptar");
        acceptBtn.addActionListener(e -> {
            accepted = true;
            dispose();
        });

        JPanel optionsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        optionsPanel.add(upperChk);
        optionsPanel.add(lowerChk);
        optionsPanel.add(digitsChk);
        optionsPanel.add(symbolsChk);
        optionsPanel.add(new JLabel("Longitud:"));
        optionsPanel.add(lengthSpinner);

        JPanel resultPanel = new JPanel(new FlowLayout());
        resultPanel.add(new JLabel("Contraseña:"));
        resultPanel.add(resultField);
        resultPanel.add(copyBtn);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(generateBtn);
        buttonPanel.add(acceptBtn);
        JButton closeBtn = new JButton("Cerrar");
        closeBtn.addActionListener(e -> dispose());
        buttonPanel.add(closeBtn);

        setLayout(new BorderLayout());
        add(optionsPanel, BorderLayout.NORTH);
        add(resultPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private void generate() {
        generator.setIncludeUppercase(upperChk.isSelected());
        generator.setIncludeLowercase(lowerChk.isSelected());
        generator.setIncludeDigits(digitsChk.isSelected());
        generator.setIncludeSymbols(symbolsChk.isSelected());
        generator.setLength((Integer) lengthSpinner.getValue());
        try {
            String pwd = generator.generate();
            resultField.setText(pwd);
            lastGenerated = pwd;
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void copy() {
        String pwd = resultField.getText();
        if (!pwd.isEmpty()) {
            SecureClipboard.copyToClipboard(pwd);
            JOptionPane.showMessageDialog(this, "Contraseña copiada (se borrará en 30s)");
        }
    }

    public boolean isAccepted(){
        return accepted;
    }

    public String getGeneratedPassword(){
        return lastGenerated;
    }
}
