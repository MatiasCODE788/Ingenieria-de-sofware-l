package cl.antucayen.model.dao;

import cl.antucayen.model.entity.LogArchivo;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogArchivoDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(LogArchivo log) throws SQLException {
        String sql = """
                INSERT INTO log_archivo
                (id_usuario, nombre_usuario, nombre_archivo, tipo_operacion, formato, resultado, detalle)
                VALUES (?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, log.getIdUsuario());
            ps.setString(2, log.getNombreUsuario());
            ps.setString(3, log.getNombreArchivo());
            ps.setString(4, log.getTipoOperacion());
            ps.setString(5, log.getFormato());
            ps.setString(6, log.getResultado());
            ps.setString(7, log.getDetalle());
            ps.executeUpdate();
        }
    }

    public List<LogArchivo> listarRecientes(int limite) throws SQLException {
        int limiteSeguro = Math.max(1, Math.min(limite, 500));
        String sql = """
                SELECT id_log, fecha_hora, id_usuario, nombre_usuario, nombre_archivo,
                       tipo_operacion, formato, resultado, detalle
                FROM log_archivo
                ORDER BY fecha_hora DESC, id_log DESC
                LIMIT ?
                """;
        List<LogArchivo> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, limiteSeguro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private LogArchivo mapear(ResultSet rs) throws SQLException {
        LogArchivo log = new LogArchivo();
        log.setIdLog(rs.getInt("id_log"));
        Timestamp ts = rs.getTimestamp("fecha_hora");
        log.setFechaHora(ts == null ? null : ts.toLocalDateTime());
        log.setIdUsuario(rs.getInt("id_usuario"));
        log.setNombreUsuario(rs.getString("nombre_usuario"));
        log.setNombreArchivo(rs.getString("nombre_archivo"));
        log.setTipoOperacion(rs.getString("tipo_operacion"));
        log.setFormato(rs.getString("formato"));
        log.setResultado(rs.getString("resultado"));
        log.setDetalle(rs.getString("detalle"));
        return log;
    }
}
