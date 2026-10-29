package dev.view;

import javax.swing.*;
import java.awt.*;

/**
 * Columna vertical: cada fila ocupa todo el ancho (o se centra) con un hueco superior propio.
 * Sustituye a BoxLayout, cuyos separadores centrados descuadran el resto de componentes.
 */
public class VBox extends JPanel {
    private int row;

    public VBox() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    /** Fila a todo el ancho. */
    public VBox row(JComponent c, int gapTop) {
        return place(c, gapTop, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH, 0);
    }

    /** Fila con el componente centrado y a su tamaño natural. */
    public VBox centered(JComponent c, int gapTop) {
        return place(c, gapTop, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0);
    }

    /** Hueco elástico: empuja las filas siguientes hacia abajo si sobra altura. */
    public VBox glue() {
        return place(new JPanel() {{ setOpaque(false); }}, 0, GridBagConstraints.BOTH, GridBagConstraints.NORTH, 1);
    }

    private VBox place(JComponent c, int gapTop, int fill, int anchor, double weightY) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row++;
        g.weightx = 1;
        g.weighty = weightY;
        g.fill = fill;
        g.anchor = anchor;
        g.insets = new Insets(gapTop, 0, 0, 0);
        add(c, g);
        return this;
    }

    /** VBox para JScrollPane: sigue el ancho del visor y, si sobra alto, ocupa todo el visor. */
    public static class Scrolling extends VBox implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && vp.getHeight() > getPreferredSize().height;
        }
    }
}
