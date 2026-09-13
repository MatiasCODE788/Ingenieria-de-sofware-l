package cl.antucayen.model.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Gestiona de forma centralizada los archivos digitales asociados a facturas. */
public class ServicioArchivoFactura {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("pdf", "jpg", "jpeg", "png");

    public void validar(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo de factura válido");
        }
        if (archivo.length() <= 0) {
            throw new IllegalArgumentException("El archivo de factura está vacío");
        }
        if (archivo.length() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo de factura no puede superar 10 MB");
        }
        String extension = extension(archivo.getName());
        if (!EXTENSIONES.contains(extension)) {
            throw new IllegalArgumentException("Formato no admitido. Usa PDF, JPG, JPEG o PNG");
        }
    }

    /**
     * Copia el adjunto a una ruta administrada por la aplicación. La BD almacena
     * esa copia y no una referencia temporal al archivo elegido por el usuario.
     */
    public String guardarCopia(File origen) throws IOException {
        validar(origen);
        Path directorio = Path.of("archivos", "facturas").toAbsolutePath().normalize();
        Files.createDirectories(directorio);

        String extension = extension(origen.getName());
        String base = nombreSeguro(origen.getName().replaceFirst("(?i)\\.[^.]+$", ""));
        if (base.isBlank()) base = "factura";
        String nombre = base + "_" + UUID.randomUUID() + "." + extension;
        Path destino = directorio.resolve(nombre).normalize();
        if (!destino.startsWith(directorio)) {
            throw new IOException("Ruta de archivo de factura no válida");
        }
        Files.copy(origen.toPath(), destino);
        return destino.toString();
    }

    public void eliminarCopiaSilenciosamente(String ruta) {
        if (ruta == null || ruta.isBlank()) return;
        try {
            Path directorio = Path.of("archivos", "facturas").toAbsolutePath().normalize();
            Path archivo = Path.of(ruta).toAbsolutePath().normalize();
            if (archivo.startsWith(directorio)) Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // La falla al limpiar un adjunto que nunca quedó registrado no debe
            // ocultar la excepción original de la operación de negocio.
        }
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private String nombreSeguro(String nombre) {
        return nombre.replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_");
    }
}
