package cl.antucayen.view.components;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/** Logo oficial embebido en el JAR para funcionar en Linux y Windows sin rutas externas. */
public final class LogoAntucayen {
    private static final BufferedImage ORIGINAL = cargar();

    private LogoAntucayen() { }

    private static BufferedImage cargar() {
        URL archivo = LogoAntucayen.class.getResource("/branding/logo-antucayen.png");
        if (archivo != null) {
            try { return ImageIO.read(archivo); }
            catch (IOException ignored) { /* Recurso dañado: se usa reserva visual. */ }
        }
        BufferedImage reserva = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = reserva.createGraphics();
        try {
            g.setColor(new Color(156, 194, 31));
            g.fillOval(2, 2, 60, 60);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Dialog", Font.BOLD, 38));
            g.drawString("A", 18, 46);
        } finally { g.dispose(); }
        return reserva;
    }

    /** Máscara circular solo sobre las esquinas de la imagen, conservando su diseño interno. */
    public static Image imagen(int tamano) {
        if (tamano <= 0) throw new IllegalArgumentException("Tamaño de logo no válido");
        BufferedImage icono = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icono.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setClip(new Ellipse2D.Double(0, 0, tamano, tamano));
            g.drawImage(ORIGINAL, 0, 0, tamano, tamano, null);
        } finally { g.dispose(); }
        return icono;
    }

    public static ImageIcon icono(int tamano) { return new ImageIcon(imagen(tamano)); }
}
