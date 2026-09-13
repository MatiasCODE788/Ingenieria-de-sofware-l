package cl.antucayen.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConexion {

    @FunctionalInterface
    public interface OperacionSQL<T> {
        T ejecutar() throws SQLException;
    }

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
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) props.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer config.properties: " + e.getMessage(), e);
        }

        host = valorConfiguracion(props, "db.host", "ANTUCAYEN_DB_HOST");
        port = valorConfiguracion(props, "db.port", "ANTUCAYEN_DB_PORT");
        nombre = valorConfiguracion(props, "db.nombre", "ANTUCAYEN_DB_NAME");
        usuario = valorConfiguracion(props, "db.usuario", "ANTUCAYEN_DB_USER");
        contrasena = valorConfiguracion(props, "db.contrasena", "ANTUCAYEN_DB_PASSWORD");

        String ssl = valorOpcional(props, "db.ssl", "ANTUCAYEN_DB_SSL", "false");
        usarSsl = Boolean.parseBoolean(ssl);

        if (host == null || port == null || nombre == null || usuario == null || contrasena == null) {
            throw new IllegalStateException(
                    "Configuración de base de datos incompleta. Copia config.properties.example como "
                            + "config.properties o define las variables ANTUCAYEN_DB_HOST, ANTUCAYEN_DB_PORT, "
                            + "ANTUCAYEN_DB_NAME, ANTUCAYEN_DB_USER y ANTUCAYEN_DB_PASSWORD.");
        }
    }

    private String valorConfiguracion(Properties props, String clave, String variableEntorno) {
        String entorno = System.getenv(variableEntorno);
        if (entorno != null && !entorno.isBlank()) return entorno.trim();
        String valor = props.getProperty(clave);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String valorOpcional(Properties props, String clave, String variableEntorno, String defecto) {
        String entorno = System.getenv(variableEntorno);
        if (entorno != null && !entorno.isBlank()) return entorno.trim();
        return props.getProperty(clave, defecto).trim();
    }

    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            String url = "jdbc:mariadb://" + host + ":" + port + "/" + nombre
                    + "?useUnicode=true&characterEncoding=UTF-8"
                    + "&allowPublicKeyRetrieval=true&useSsl=" + usarSsl;
            conexion = DriverManager.getConnection(url, usuario, contrasena);
        }
        return conexion;
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
                // Si no se puede restaurar la conexión, se descarta para que
                // la siguiente operación abra una nueva conexión limpia.
                try {
                    conn.close();
                } catch (SQLException ignored) {
                    // no hay acción adicional segura que realizar aquí
                }
                conexion = null;
            }
        }
    }
}
