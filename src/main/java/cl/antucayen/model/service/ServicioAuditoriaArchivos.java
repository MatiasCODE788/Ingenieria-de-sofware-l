package cl.antucayen.model.service;

import cl.antucayen.model.dao.LogArchivoDAO;
import cl.antucayen.model.entity.LogArchivo;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.security.SesionActual;

import java.sql.SQLException;
import java.util.List;

/** RF-37: bitácora persistente de importaciones y exportaciones. */
public class ServicioAuditoriaArchivos {

    private final LogArchivoDAO logArchivoDAO = new LogArchivoDAO();

    public void registrar(String nombreArchivo,
                          String tipoOperacion,
                          String formato,
                          String resultado,
                          String detalle) throws SQLException {
        Autorizacion.verificarSesionActiva();
        Usuario usuario = SesionActual.getUsuario();
        if (usuario == null) throw new SecurityException("No existe una sesión autenticada");

        LogArchivo log = new LogArchivo();
        log.setIdUsuario(usuario.getIdUsuario());
        String nombre = usuario.getNombreCompleto();
        log.setNombreUsuario(nombre == null || nombre.isBlank() ? usuario.getUsername() : nombre.trim());
        log.setNombreArchivo(limpiar(nombreArchivo));
        log.setTipoOperacion(validarOperacion(tipoOperacion));
        log.setFormato(limpiar(formato).toUpperCase());
        log.setResultado(validarResultado(resultado));
        log.setDetalle(detalle == null ? null : detalle.trim());
        logArchivoDAO.insertar(log);
    }

    public List<LogArchivo> listarRecientes(int limite) throws SQLException {
        Autorizacion.verificarAdministrador(
                "Solo el Administrador puede consultar la bitácora de archivos");
        return logArchivoDAO.listarRecientes(limite);
    }

    private String validarOperacion(String operacion) {
        String valor = limpiar(operacion).toUpperCase();
        if (!"IMPORTACION".equals(valor) && !"EXPORTACION".equals(valor)
                && !"PLANTILLA".equals(valor)) {
            throw new IllegalArgumentException("Tipo de operación de archivo no válido");
        }
        return valor;
    }

    private String validarResultado(String resultado) {
        String valor = limpiar(resultado).toUpperCase();
        if (!"EXITOSO".equals(valor) && !"ERROR".equals(valor)) {
            throw new IllegalArgumentException("Resultado de auditoría no válido");
        }
        return valor;
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
