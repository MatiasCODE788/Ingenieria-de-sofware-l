package cl.antucayen.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConexion {

    private static DBConexion instancia;
    private Connection conexion;

    private String host;
    private String port;
    private String nombre;
    private String usuario;
    private String contrasena;

    private DBConexion() {
        cargarPropiedades();
    }

    // Singleton: solo una instancia de la conexión
    public static DBConexion getInstancia() {
        if (instancia == null) {
            instancia = new DBConexion();
        }
        return instancia;
    }

    private void cargarPropiedades() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("No se encontró config.properties");
            }
            Properties props = new Properties();
            props.load(input);
            host      = props.getProperty("db.host");
            port      = props.getProperty("db.port");
            nombre    = props.getProperty("db.nombre");
            usuario   = props.getProperty("db.usuario");
            contrasena = props.getProperty("db.contrasena");
        } catch (IOException e) {
            throw new RuntimeException("Error al leer config.properties: " + e.getMessage());
        }
    }

    public Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            String url = "jdbc:mariadb://" + host + ":" + port + "/" + nombre
                    + "?useUnicode=true&characterEncoding=UTF-8";
            conexion = DriverManager.getConnection(url, usuario, contrasena);
        }
        return conexion;
    }

    public void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                conexion = null;
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }

    // Probar la conexión
    public static void main(String[] args) {
        try {
            Connection con = DBConexion.getInstancia().getConexion();
            if (con != null) {
                System.out.println("✅ Conexión exitosa a la base de datos minimarket");
                DBConexion.getInstancia().cerrarConexion();
            }
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
        }
    }
}