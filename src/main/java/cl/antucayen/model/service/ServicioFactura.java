package cl.antucayen.model.service;

import cl.antucayen.model.dao.FacturaDAO;
import cl.antucayen.model.entity.Factura;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.List;

public class ServicioFactura {

    private final FacturaDAO facturaDAO = new FacturaDAO();

    public int registrar(Factura f) throws SQLException {
        if (f.getNumeroFactura() == null || f.getNumeroFactura().isEmpty())
            throw new IllegalArgumentException("El número de factura es obligatorio");
        f.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
        return facturaDAO.insertar(f);
    }

    public void actualizarEstado(int idFactura, String estado) throws SQLException {
        facturaDAO.actualizarEstado(idFactura, estado);
    }

    public Factura buscarPorId(int id) throws SQLException {
        return facturaDAO.buscarPorId(id);
    }

    public List<Factura> listarTodas() throws SQLException {
        return facturaDAO.listarTodas();
    }

    public List<Factura> listarPendientes() throws SQLException {
        return facturaDAO.listarPorEstado("Pendiente");
    }

    public List<Factura> listarPorEstado(String estado) throws SQLException {
        return facturaDAO.listarPorEstado(estado);
    }
}