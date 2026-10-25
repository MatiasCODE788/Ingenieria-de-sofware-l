package cl.antucayen.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Iconos vectoriales sin dependencia de fuentes especiales (funcionan en Windows y Linux). */
public final class IconoNavegacion implements Icon {
    private final String tipo;
    private final Color color;
    private final int tamano;

    public IconoNavegacion(String tipo, Color color, int tamano) {
        this.tipo = tipo;
        this.color = color;
        this.tamano = tamano;
    }

    @Override public int getIconWidth() { return tamano; }
    @Override public int getIconHeight() { return tamano; }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D p = (Graphics2D) g.create();
        try {
            p.translate(x, y);
            p.scale(tamano / 24.0, tamano / 24.0);
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(color);
            p.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (tipo) {
                case "cart" -> { p.draw(new Line2D.Double(3, 5, 6, 5)); p.draw(new Line2D.Double(6, 5, 8, 16));
                    p.draw(new Line2D.Double(8, 16, 19, 16)); p.draw(new Line2D.Double(9, 7, 21, 7));
                    p.draw(new Line2D.Double(21, 7, 19, 13)); p.draw(new Line2D.Double(19, 13, 8, 13));
                    p.fill(new Ellipse2D.Double(8, 19, 2.8, 2.8)); p.fill(new Ellipse2D.Double(17, 19, 2.8, 2.8)); }
                case "dashboard" -> { p.drawRoundRect(3, 3, 8, 8, 2, 2); p.drawRoundRect(13, 3, 8, 5, 2, 2);
                    p.drawRoundRect(3, 13, 8, 8, 2, 2); p.drawRoundRect(13, 10, 8, 11, 2, 2); }
                case "products" -> { p.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 4, 4));
                    p.draw(new Line2D.Double(4, 10, 20, 10)); p.draw(new Line2D.Double(12, 10, 12, 20)); }
                case "suppliers" -> { p.drawRoundRect(3, 9, 18, 12, 2, 2); p.draw(new Line2D.Double(7, 9, 7, 4));
                    p.draw(new Line2D.Double(7, 4, 17, 4)); p.draw(new Line2D.Double(17, 4, 17, 9));
                    p.draw(new Line2D.Double(8, 14, 10, 14)); p.draw(new Line2D.Double(14, 14, 16, 14)); }
                case "facturas" -> { p.drawRoundRect(5, 3, 14, 18, 2, 2); p.draw(new Line2D.Double(9, 8, 15, 8));
                    p.draw(new Line2D.Double(9, 12, 16, 12)); p.draw(new Line2D.Double(9, 16, 14, 16)); }
                case "upload" -> { p.draw(new Line2D.Double(12, 3, 12, 15)); p.draw(new Line2D.Double(8, 7, 12, 3));
                    p.draw(new Line2D.Double(16, 7, 12, 3)); p.draw(new Arc2D.Double(3, 12, 18, 10, 180, 180, Arc2D.OPEN)); }
                case "settings" -> { p.draw(new Ellipse2D.Double(5, 5, 14, 14)); p.draw(new Ellipse2D.Double(10, 10, 4, 4));
                    p.draw(new Line2D.Double(12, 1, 12, 5)); p.draw(new Line2D.Double(12, 19, 12, 23));
                    p.draw(new Line2D.Double(1, 12, 5, 12)); p.draw(new Line2D.Double(19, 12, 23, 12)); }
                case "history" -> { p.draw(new Arc2D.Double(3, 3, 18, 18, 55, 290, Arc2D.OPEN));
                    p.draw(new Line2D.Double(4, 4, 4, 10)); p.draw(new Line2D.Double(4, 10, 10, 10));
                    p.draw(new Line2D.Double(12, 7, 12, 12)); p.draw(new Line2D.Double(12, 12, 16, 14)); }
                case "reports" -> { p.draw(new Line2D.Double(3, 21, 21, 21)); p.drawRoundRect(5, 12, 3, 9, 1, 1);
                    p.drawRoundRect(11, 8, 3, 13, 1, 1); p.drawRoundRect(17, 4, 3, 17, 1, 1); }
                case "users" -> { p.draw(new Ellipse2D.Double(8, 3, 8, 8)); p.draw(new Arc2D.Double(3, 12, 18, 16, 0, 180, Arc2D.OPEN)); }
                case "sale" -> { p.drawRoundRect(4, 3, 16, 18, 2, 2); p.draw(new Line2D.Double(8, 8, 16, 8));
                    p.draw(new Line2D.Double(8, 13, 16, 13)); p.draw(new Line2D.Double(8, 17, 12, 17)); }
                case "search" -> { p.draw(new Ellipse2D.Double(4, 4, 12, 12)); p.draw(new Line2D.Double(15, 15, 21, 21)); }
                default -> { p.drawRoundRect(4, 4, 16, 16, 3, 3); p.draw(new Line2D.Double(8, 12, 16, 12)); }
            }
        } finally { p.dispose(); }
    }
}
