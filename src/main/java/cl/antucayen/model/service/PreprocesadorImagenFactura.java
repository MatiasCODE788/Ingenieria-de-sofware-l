package cl.antucayen.model.service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/**
 * Prepara una foto o un escaneo de factura para el OCR.
 *
 * <p>Una foto tomada con el teléfono suele venir girada según la etiqueta EXIF,
 * con poca resolución si pasó por una aplicación de mensajería y con luz
 * despareja. Aquí se endereza según esa etiqueta, se pasa a escala de grises,
 * se lleva a un ancho en que la letra de la tabla mida unos 25 píxeles de alto
 * y se empareja el fondo, para que las sombras no se lean como texto.</p>
 */
final class PreprocesadorImagenFactura {

    /** Ancho de trabajo: una factura carta u oficio queda cerca de 280 ppp. */
    static final int ANCHO_OBJETIVO = 2400;

    private PreprocesadorImagenFactura() {
    }

    /** Lee la imagen y la endereza según la orientación que guardó la cámara. */
    static BufferedImage leer(File archivo) throws IOException {
        BufferedImage imagen = ImageIO.read(archivo);
        if (imagen == null) throw new IOException("No se pudo interpretar la imagen seleccionada");
        int orientacion = 1;
        try {
            orientacion = orientacionExif(Files.readAllBytes(archivo.toPath()));
        } catch (IOException | RuntimeException ignorada) {
            // Sin etiqueta legible se usa la imagen tal como viene.
        }
        return enderezar(imagen, orientacion);
    }

    static BufferedImage preparar(BufferedImage original) {
        BufferedImage gris = aGris(original);
        double escala = ANCHO_OBJETIVO / (double) gris.getWidth();
        escala = Math.max(0.5, Math.min(3.0, escala));
        BufferedImage escalada = Math.abs(escala - 1) < 0.05 ? gris : escalar(gris, escala);
        return emparejarFondo(escalada);
    }

    private static BufferedImage aGris(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_BYTE_GRAY) return original;
        BufferedImage gris = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gris.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, gris.getWidth(), gris.getHeight());
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return gris;
    }

    private static BufferedImage escalar(BufferedImage gris, double escala) {
        int ancho = Math.max(1, (int) Math.round(gris.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(gris.getHeight() * escala));
        BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = salida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(gris, 0, 0, ancho, alto, null);
        g.dispose();
        return salida;
    }

    /**
     * Divide cada píxel por el brillo del papel a su alrededor. El papel se
     * estima con el máximo de bloques de 16 píxeles, así una sombra o un borde
     * más oscuro de la foto queda blanco y el texto conserva su contraste.
     */
    static BufferedImage emparejarFondo(BufferedImage gris) {
        int ancho = gris.getWidth();
        int alto = gris.getHeight();
        byte[] px = new byte[ancho * alto];
        gris.getRaster().getDataElements(0, 0, ancho, alto, px);

        int bloque = 16;
        int bw = (ancho + bloque - 1) / bloque;
        int bh = (alto + bloque - 1) / bloque;
        float[] promedio = new float[bw * bh];
        for (int by = 0; by < bh; by++) {
            for (int bx = 0; bx < bw; bx++) {
                long suma = 0;
                int cuenta = 0;
                for (int y = by * bloque; y < Math.min(alto, (by + 1) * bloque); y++) {
                    int fila = y * ancho;
                    for (int x = bx * bloque; x < Math.min(ancho, (bx + 1) * bloque); x++) {
                        suma += px[fila + x] & 0xFF;
                        cuenta++;
                    }
                }
                promedio[by * bw + bx] = cuenta == 0 ? 255 : (float) suma / cuenta;
            }
        }
        // Máximo de 3x3 bloques: el papel es lo más claro del vecindario.
        float[] fondo = new float[bw * bh];
        for (int by = 0; by < bh; by++) {
            for (int bx = 0; bx < bw; bx++) {
                float maximo = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int yy = by + dy;
                        int xx = bx + dx;
                        if (yy < 0 || yy >= bh || xx < 0 || xx >= bw) continue;
                        maximo = Math.max(maximo, promedio[yy * bw + xx]);
                    }
                }
                fondo[by * bw + bx] = Math.max(1, maximo);
            }
        }

        byte[] salida = new byte[ancho * alto];
        for (int y = 0; y < alto; y++) {
            double fy = Math.max(0, Math.min(bh - 1.0, (y + 0.5) / bloque - 0.5));
            int y0 = (int) fy;
            int y1 = Math.min(bh - 1, y0 + 1);
            double ty = fy - y0;
            for (int x = 0; x < ancho; x++) {
                double fx = Math.max(0, Math.min(bw - 1.0, (x + 0.5) / bloque - 0.5));
                int x0 = (int) fx;
                int x1 = Math.min(bw - 1, x0 + 1);
                double tx = fx - x0;
                double f = (fondo[y0 * bw + x0] * (1 - tx) + fondo[y0 * bw + x1] * tx) * (1 - ty)
                        + (fondo[y1 * bw + x0] * (1 - tx) + fondo[y1 * bw + x1] * tx) * ty;
                int v = (int) Math.round((px[y * ancho + x] & 0xFF) * 255.0 / f);
                salida[y * ancho + x] = (byte) Math.min(255, v);
            }
        }
        BufferedImage resultado = new BufferedImage(ancho, alto, BufferedImage.TYPE_BYTE_GRAY);
        resultado.getRaster().setDataElements(0, 0, ancho, alto, salida);
        return resultado;
    }

    /** Gira la imagen según la etiqueta EXIF: 3 = 180°, 6 = 90° horario, 8 = 90° antihorario. */
    static BufferedImage enderezar(BufferedImage imagen, int orientacion) {
        int grados = switch (orientacion) {
            case 3 -> 180;
            case 6 -> 90;
            case 8 -> 270;
            default -> 0;
        };
        if (grados == 0) return imagen;
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        boolean gira = grados != 180;
        BufferedImage salida = new BufferedImage(gira ? alto : ancho, gira ? ancho : alto, BufferedImage.TYPE_INT_RGB);
        AffineTransform t = new AffineTransform();
        if (grados == 90) {
            t.translate(alto, 0);
        } else if (grados == 180) {
            t.translate(ancho, alto);
        } else {
            t.translate(0, ancho);
        }
        t.rotate(Math.toRadians(grados));
        Graphics2D g = salida.createGraphics();
        g.drawImage(imagen, t, null);
        g.dispose();
        return salida;
    }

    /** Orientación EXIF de un JPEG, o 1 si no tiene. Solo lee la etiqueta 0x0112 del primer IFD. */
    static int orientacionExif(byte[] datos) {
        if (datos.length < 4 || (datos[0] & 0xFF) != 0xFF || (datos[1] & 0xFF) != 0xD8) return 1;
        int i = 2;
        while (i + 4 <= datos.length) {
            if ((datos[i] & 0xFF) != 0xFF) return 1;
            int marcador = datos[i + 1] & 0xFF;
            int largo = ((datos[i + 2] & 0xFF) << 8) | (datos[i + 3] & 0xFF);
            if (marcador == 0xDA || largo < 2) return 1;
            if (marcador == 0xE1 && i + 10 <= datos.length
                    && datos[i + 4] == 'E' && datos[i + 5] == 'x' && datos[i + 6] == 'i' && datos[i + 7] == 'f') {
                int tiff = i + 10;
                if (tiff + 8 > datos.length) return 1;
                ByteOrder orden = datos[tiff] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
                ByteBuffer b = ByteBuffer.wrap(datos, 0, Math.min(datos.length, tiff + largo)).order(orden);
                int ifd = tiff + b.getInt(tiff + 4);
                if (ifd + 2 > b.limit()) return 1;
                int entradas = b.getShort(ifd) & 0xFFFF;
                for (int e = 0; e < entradas; e++) {
                    int pos = ifd + 2 + e * 12;
                    if (pos + 12 > b.limit()) return 1;
                    if ((b.getShort(pos) & 0xFFFF) == 0x0112) {
                        int valor = b.getShort(pos + 8) & 0xFFFF;
                        return valor >= 1 && valor <= 8 ? valor : 1;
                    }
                }
                return 1;
            }
            i += 2 + largo;
        }
        return 1;
    }
}
