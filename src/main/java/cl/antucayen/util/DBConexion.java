package cl.antucayen.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DBConexion {

    @FunctionalInterface
    public interface OperacionSQL<T> {
        T ejecutar() throws SQLException;
    }

    private static final String ARCHIVO_CONFIG = "config.properties";
    private static final String PROPIEDAD_RUTA_CONFIG = "antucayen.config";

    private static DBConexion instancia;
    private Connection conexion;

    private String host;
    private String port;
    private String nombre;
    private String usuario;
    private String contrasena;
    private boolean usarSsl;

    private DBConexion() {
        cargarConfiguracion();
    }

    public static synchronized DBConexion getInstancia() {
        if (instancia == null) instancia = new DBConexion();
        return instancia;
    }

    private void cargarConfiguracion() {
        Properties props = new Properties();
        List<String> ubicacionesIntentadas = new ArrayList<>();

        try {
            cargarArchivoExplicito(props, ubicacionesIntentadas);
            if (props.isEmpty()) cargarDesdeClasspath(props, ubicacionesIntentadas);
            if (props.isEmpty()) cargarDesdeSistemaDeArchivos(props, ubicacionesIntentadas);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error al leer la configuración de base de datos: " + e.getMessage(), e);
        }

        host = valorConfiguracion(props, "db.host", "ANTUCAYEN_DB_HOST");
        port = valorConfiguracion(props, "db.port", "ANTUCAYEN_DB_PORT");
        nombre = valorConfiguracion(props, "db.nombre", "ANTUCAYEN_DB_NAME");
        usuario = valorConfiguracion(props, "db.usuario", "ANTUCAYEN_DB_USER");
        contrasena = valorConfiguracion(props, "db.contrasena", "ANTUCAYEN_DB_PASSWORD");
        usarSsl = Boolean.parseBoolean(
                valorOpcional(props, "db.ssl", "ANTUCAYEN_DB_SSL", "false"));

        if (host == null || port == null || nombre == null || usuario == null || contrasena == null) {
            String directorioTrabajo = System.getProperty("user.dir", "<desconocido>");
            String intentos = ubicacionesIntentadas.isEmpty()
                    ? "ninguna ubicación de archivo disponible"
                    : String.join("; ", ubicacionesIntentadas);
            throw new IllegalStateException(
                    "Configuración de base de datos incompleta. Directorio de trabajo: "
                            + directorioTrabajo + ". Ubicaciones revisadas: " + intentos
                            + ". Asegúrate de que exista src/main/resources/config.properties, "
                            + "o ejecuta con -Dantucayen.config=RUTA_AL_ARCHIVO, o define "
                            + "ANTUCAYEN_DB_HOST, ANTUCAYEN_DB_PORT, ANTUCAYEN_DB_NAME, "
                            + "ANTUCAYEN_DB_USER y ANTUCAYEN_DB_PASSWORD.");
        }
    }

    private void cargarArchivoExplicito(Properties props,
                                        List<String> ubicacionesIntentadas) throws IOException {
        String rutaExplicita = System.getProperty(PROPIEDAD_RUTA_CONFIG);
        if (rutaExplicita == null || rutaExplicita.isBlank()) return;

        Path archivo = Paths.get(rutaExplicita.trim()).toAbsolutePath().normalize();
        ubicacionesIntentadas.add(archivo.toString());
        cargarSiExiste(props, archivo);
    }

    private void cargarDesdeClasspath(Properties props,
                                      List<String> ubicacionesIntentadas) throws IOException {
        ubicacionesIntentadas.add("classpath:/" + ARCHIVO_CONFIG);

        ClassLoader contexto = Thread.currentThread().getContextClassLoader();
        if (contexto != null) {
            try (InputStream input = contexto.getResourceAsStream(ARCHIVO_CONFIG)) {
                if (input != null) {
                    props.load(input);
                    return;
                }
            }
        }

        try (InputStream input = DBConexion.class.getResourceAsStream("/" + ARCHIVO_CONFIG)) {
            if (input != null) props.load(input);
        }
    }

    private void cargarDesdeSistemaDeArchivos(Properties props,
                                              List<String> ubicacionesIntentadas) throws IOException {
        Path trabajo = Paths.get(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();

        Path configLocal = trabajo.resolve(ARCHIVO_CONFIG);
        ubicacionesIntentadas.add(configLocal.toString());
        if (cargarSiExiste(props, configLocal)) return;

        Path actual = trabajo;
        for (int nivel = 0; nivel < 6 && actual != null; nivel++) {
            Path candidato = actual.resolve("src")
                    .resolve("main")
                    .resolve("resources")
                    .resolve(ARCHIVO_CONFIG)
                    .normalize();
            ubicacionesIntentadas.add(candidato.toString());
            if (cargarSiExiste(props, candidato)) return;
            actual = actual.getParent();
        }
    }

    private boolean cargarSiExiste(Properties props, Path archivo) throws IOException {
        if (!Files.isRegularFile(archivo)) return false;
        try (InputStream input = Files.newInputStream(archivo)) {
            props.load(input);
            return true;
        }
    }

    private String valorConfiguracion(Properties props, String clave, String variableEntorno) {
        String entorno = System.getenv(variableEntorno);
        if (entorno != null && !entorno.isBlank()) return entorno.trim();
        String valor = props.getProperty(clave);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String valorOpcional(Properties props, String clave,
                                 String variableEntorno, String defecto) {
        String entorno = System.getenv(variableEntorno);
        if (entorno != null && !entorno.isBlank()) return entorno.trim();
        return props.getProperty(clave, defecto).trim();
    }

    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = crearConexion();
        }
        return conexion;
    }

    /**
     * Abre una conexión independiente de la conexión transaccional compartida.
     *
     * <p>Se usa exclusivamente en lecturas ejecutadas fuera del EDT (por
     * ejemplo, autocompletado del POS). El llamador debe cerrarla mediante
     * try-with-resources. De esta forma una consulta en segundo plano no
     * comparte simultáneamente el mismo objeto JDBC Connection con una venta o
     * ajuste ejecutado en el hilo principal.</p>
     */
    public Connection abrirConexionIndependiente() throws SQLException {
        return crearConexion();
    }

    private Connection crearConexion() throws SQLException {
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + nombre
                + "?useUnicode=true&characterEncoding=UTF-8"
                + "&allowPublicKeyRetrieval=true&useSsl=" + usarSsl;
        return DriverManager.getConnection(url, usuario, contrasena);
    }

    /**
     * Ejecuta una operación de negocio de forma atómica usando la misma
     * conexión compartida por los DAO actuales. Si ya existe una transacción
     * activa, se integra en ella sin confirmar ni revertir por separado.
     */
    public synchronized <T> T ejecutarEnTransaccion(OperacionSQL<T> operacion) throws SQLException {
        Connection conn = getConexion();
        boolean gestionarTransaccion = conn.getAutoCommit();
        if (!gestionarTransaccion) return operacion.ejecutar();

        conn.setAutoCommit(false);
        try {
            T resultado = operacion.ejecutar();
            conn.commit();
            return resultado;
        } catch (SQLException | RuntimeException ex) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                    // Sin acción adicional segura.
                }
                conexion = null;
            }
        }
    }
}
