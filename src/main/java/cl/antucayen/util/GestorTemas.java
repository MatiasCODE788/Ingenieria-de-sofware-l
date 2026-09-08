package cl.antucayen.util;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Guarda qué tema está activo y lo recuerda entre sesiones.
 * Se guarda en el HOME del usuario (no en config.properties, que es solo
 * para la conexión a la BD), así que la preferencia de tema es por
 * dispositivo/usuario del sistema operativo, igual que cualquier ajuste
 * de una app de escritorio normal.
 */
public class GestorTemas {

    private static GestorTemas instancia;

    private static final Path ARCHIVO_PREFERENCIA =
            Paths.get(System.getProperty("user.home"), ".antucayen", "tema.properties");

    private Tema temaActual;

    private GestorTemas() {
        temaActual = cargar();
    }

    public static GestorTemas getInstancia() {
        if (instancia == null) instancia = new GestorTemas();
        return instancia;
    }

    public Tema getTema() { return temaActual; }

    public void setTema(Tema tema) {
        this.temaActual = tema;
        guardar(tema);
    }

    private Tema cargar() {
        try (InputStream in = Files.newInputStream(ARCHIVO_PREFERENCIA)) {
            Properties p = new Properties();
            p.load(in);
            return Tema.valueOf(p.getProperty("tema", Tema.AZUL_CORPORATIVO.name()));
        } catch (IOException | IllegalArgumentException e) {
            return Tema.AZUL_CORPORATIVO; // primera vez o archivo corrupto: tema por defecto
        }
    }

    private void guardar(Tema tema) {
        try {
            Files.createDirectories(ARCHIVO_PREFERENCIA.getParent());
            Properties p = new Properties();
            p.setProperty("tema", tema.name());
            try (OutputStream out = Files.newOutputStream(ARCHIVO_PREFERENCIA)) {
                p.store(out, "Preferencia de tema — Minimarket Antucayen");
            }
        } catch (IOException e) {
            System.err.println("No se pudo guardar la preferencia de tema: " + e.getMessage());
        }
    }
}