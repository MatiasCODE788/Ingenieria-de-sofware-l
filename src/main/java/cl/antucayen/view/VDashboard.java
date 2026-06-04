package cl.antucayen.view;

import javax.swing.*;
import java.awt.*;

public class VDashboard extends JPanel {

    private JLabel lblProductosActivos;
    private JLabel lblFacturasPendientes;
    private JLabel lblMovimientosHoy;

    public VDashboard() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 20));
        setBackground(new Color(243, 244, 246));

        // Título
        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelTitulo.setBackground(new Color(243, 244, 246));
        JLabel lblTitulo = new JLabel("Panel principal");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitulo.setForeground(new Color(17, 24, 39));
        JLabel lblSub = new JLabel("  Resumen operativo del minimarket");
        lblSub.setFont(new Font("Arial", Font.PLAIN, 13));
        lblSub.setForeground(new Color(107, 114, 128));
        panelTitulo.add(lblTitulo);
        panelTitulo.add(lblSub);

        // Tarjetas de estadísticas
        JPanel panelTarjetas = new JPanel(new GridLayout(1, 3, 16, 0));
        panelTarjetas.setBackground(new Color(243, 244, 246));

        lblProductosActivos  = new JLabel("—");
        lblFacturasPendientes = new JLabel("—");
        lblMovimientosHoy    = new JLabel("—");

        panelTarjetas.add(crearTarjeta("PRODUCTOS ACTIVOS",  lblProductosActivos,
                new Color(37, 99, 235),  "📦"));
        panelTarjetas.add(crearTarjeta("FACTURAS PENDIENTES", lblFacturasPendientes,
                new Color(217, 119, 6),  "📄"));
        panelTarjetas.add(crearTarjeta("MOVIMIENTOS HOY",    lblMovimientosHoy,
                new Color(5, 150, 105),  "📋"));

        add(panelTitulo,    BorderLayout.NORTH);
        add(panelTarjetas,  BorderLayout.CENTER);
    }

    private JPanel crearTarjeta(String titulo, JLabel lblValor, Color color, String icono) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 11));
        lblTitulo.setForeground(new Color(107, 114, 128));

        lblValor.setFont(new Font("Arial", Font.BOLD, 32));
        lblValor.setForeground(color);

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Arial", Font.PLAIN, 28));
        lblIcono.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(lblTitulo, BorderLayout.WEST);
        top.add(lblIcono,  BorderLayout.EAST);

        card.add(top,     BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    public void setProductosActivos(int n)   { lblProductosActivos.setText(String.valueOf(n)); }
    public void setFacturasPendientes(int n)  { lblFacturasPendientes.setText(String.valueOf(n)); }
    public void setMovimientosHoy(int n)      { lblMovimientosHoy.setText(String.valueOf(n)); }
}