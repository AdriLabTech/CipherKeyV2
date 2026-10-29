package dev.view;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

/**
 * Gestiona los temas claro y oscuro de la aplicación para Nimbus L&F.
 */
public class ThemeManager {
    private static boolean darkTheme = false;

    public static void toggleTheme() {
        darkTheme = !darkTheme;
        if (darkTheme) {
            setDarkTheme();
        } else {
            setLightTheme();
        }
        // Forzar la recarga del Look & Feel para que los cambios tengan efecto
        try {
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        updateAllWindows();
    }

    public static void setLightTheme() {
        // Colores base para tema claro (similar al Nimbus por defecto)
        UIManager.put("control", new ColorUIResource(240, 240, 240));          // Fondo de paneles, botones
        UIManager.put("text", new ColorUIResource(Color.BLACK));               // Texto normal
        UIManager.put("nimbusBase", new ColorUIResource(214, 217, 223));       // Base para gradientes
        UIManager.put("nimbusBlueGrey", new ColorUIResource(184, 207, 229));   // Bordes y detalles
        UIManager.put("nimbusSelectionBackground", new ColorUIResource(57, 105, 138)); // Selección
        UIManager.put("nimbusSelectedText", new ColorUIResource(Color.WHITE)); // Texto seleccionado
        UIManager.put("nimbusLightBackground", new ColorUIResource(Color.WHITE)); // Fondo de campos de texto
        UIManager.put("menu", new ColorUIResource(240, 240, 240));             // Fondo de menús
        UIManager.put("menuText", new ColorUIResource(Color.BLACK));           // Texto de menús

        // Algunas propiedades adicionales para tablas, etc.
        UIManager.put("Table.background", new ColorUIResource(Color.WHITE));
        UIManager.put("Table.foreground", new ColorUIResource(Color.BLACK));
        UIManager.put("Table.selectionBackground", new ColorUIResource(184, 207, 229));
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.BLACK));
        UIManager.put("TableHeader.background", new ColorUIResource(230, 230, 230));
        UIManager.put("TableHeader.foreground", new ColorUIResource(Color.BLACK));

        darkTheme = false;
    }

    public static void setDarkTheme() {
        // Colores para tema oscuro
        UIManager.put("control", new ColorUIResource(60, 63, 65));            // Fondo oscuro
        UIManager.put("text", new ColorUIResource(187, 187, 187));            // Texto gris claro
        UIManager.put("nimbusBase", new ColorUIResource(75, 75, 75));         // Base oscura
        UIManager.put("nimbusBlueGrey", new ColorUIResource(45, 45, 45));     // Bordes oscuros
        UIManager.put("nimbusSelectionBackground", new ColorUIResource(75, 110, 175)); // Azul selección
        UIManager.put("nimbusSelectedText", new ColorUIResource(Color.WHITE));
        UIManager.put("nimbusLightBackground", new ColorUIResource(69, 73, 74)); // Fondo campos texto
        UIManager.put("menu", new ColorUIResource(60, 63, 65));
        UIManager.put("menuText", new ColorUIResource(187, 187, 187));

        // Tablas
        UIManager.put("Table.background", new ColorUIResource(60, 63, 65));
        UIManager.put("Table.foreground", new ColorUIResource(187, 187, 187));
        UIManager.put("Table.selectionBackground", new ColorUIResource(75, 110, 175));
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("TableHeader.background", new ColorUIResource(45, 45, 45));
        UIManager.put("TableHeader.foreground", new ColorUIResource(187, 187, 187));

        darkTheme = true;
    }

    private static void updateAllWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    public static boolean isDarkTheme() {
        return darkTheme;
    }
}