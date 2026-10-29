package dev.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.function.Supplier;

/** Iconos SVG (carpeta resources/icons) recoloreados según el tema activo. */
public final class Icons {
    private static final int DEFAULT_SIZE = 16;

    private Icons() {}

    /** Icono gris neutro (sigue al tema). */
    public static Icon get(String name) {
        return get(name, DEFAULT_SIZE);
    }

    public static Icon get(String name, int size) {
        return colored(name, size, () -> UIManager.getColor("App.icon.color"));
    }

    public static Icon accent(String name, int size) {
        return colored(name, size, () -> UIManager.getColor("Component.accentColor"));
    }

    public static Icon white(String name, int size) {
        return colored(name, size, () -> Color.WHITE);
    }

    public static Icon colored(String name, int size, Supplier<Color> color) {
        // El mapper se evalúa al pintar, así que el icono cambia solo al alternar el tema.
        return new FlatSVGIcon("icons/" + name + ".svg", size, size, Icons.class.getClassLoader())
                .setColorFilter(new FlatSVGIcon.ColorFilter(c -> color.get()));
    }

    /** Imagen de la marca (escudo) para iconos de ventana. */
    public static Image appIcon() {
        return new ImageIcon(Icons.class.getResource("/img/icon.png")).getImage();
    }

    public static ImageIcon logo(int width) {
        ImageIcon raw = new ImageIcon(Icons.class.getResource("/img/logo.png"));
        int height = raw.getIconHeight() * width / raw.getIconWidth();
        return new ImageIcon(raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    /** Círculo de color con la inicial, para identificar cada entrada. */
    public static final class Avatar implements Icon {
        private static final Color[] PALETTE = {
                new Color(0x1B8AC9), new Color(0x7C5CFC), new Color(0xE0557A), new Color(0xE58A1F),
                new Color(0x2FA37C), new Color(0x4F6BED), new Color(0xC9573B), new Color(0x8E5BA8)};

        private final String letter;
        private final Color color;
        private final int size;

        public Avatar(String title, int size) {
            String t = title == null ? "" : title.trim();
            this.letter = t.isEmpty() ? "?" : t.substring(0, t.offsetByCodePoints(0, 1)).toUpperCase();
            this.color = PALETTE[Math.floorMod(t.toLowerCase().hashCode(), PALETTE.length)];
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fill(new Ellipse2D.Float(x, y, size, size));
            g2.setColor(Color.WHITE);
            g2.setFont(c.getFont().deriveFont(Font.BOLD, size * 0.45f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(letter, x + (size - fm.stringWidth(letter)) / 2f, y + (size + fm.getAscent() - fm.getDescent()) / 2f);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
