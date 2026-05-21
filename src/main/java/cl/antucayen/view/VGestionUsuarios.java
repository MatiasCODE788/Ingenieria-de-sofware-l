package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VGestionUsuarios extends JPanel {

    private JTable tblUsuarios;
    private DefaultTableModel modeloTabla;
    private JButton btnNuevoUsuario;
    private JButton btnEditar;
    private JButton btnDesactivar;

    public VGestionUsuarios() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(240, 244, 248));

        // ── BARRA SUPERIOR ────────────────────────────────────────
        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        panelTop.setBackground(Color.WHITE);
        panelTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 227, 237)));

        btnNuevoUsuario = crearBoton("+ Nuevo usuario", new Color(5, 150, 105));
        btnEditar       = crearBoton("✏️ Editar",        new Color(37, 99, 235));
        btnDesactivar   = crearBoton("🚫 Desactivar",    new Color(220, 38, 38));

        panelTop.add(btnNuevoUsuario);
        panelTop.add(btnEditar);
        panelTop.add(btnDesactivar);

        // ── TABLA ─────────────────────────────────────────────────
        String[] columnas = {"Usuario", "Nombre completo", "Rol", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblUsuarios = new JTable(modeloTabla);
        tblUsuarios.setFont(new Font("Arial", Font.PLAIN, 13));
        tblUsuarios.setRowHeight(36);
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblUsuarios.setGridColor(new Color(220, 227, 237));
        tblUsuarios.setSelectionBackground(new Color(219, 234, 254));
        tblUsuarios.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblUsuarios.getTableHeader().setBackground(new Color(30, 48, 84));
        tblUsuarios.getTableHeader().setForeground(Color.WHITE);
        tblUsuarios.getTableHeader().setPreferredSize(new Dimension(0, 38));

        JScrollPane scroll = new JScrollPane(tblUsuarios);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));

        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        panelTitulo.setBackground(Color.WHITE);
        JLabel lblTitulo = new JLabel("Usuarios del sistema");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        panelTitulo.add(lblTitulo);

        panelTabla.add(panelTitulo, BorderLayout.NORTH);
        panelTabla.add(scroll, BorderLayout.CENTER);

        add(panelTop, BorderLayout.NORTH);
        add(panelTabla, BorderLayout.CENTER);

        // Datos de ejemplo
        modeloTabla.addRow(new Object[]{"admin",   "Juan Pérez",    "Administrador", "Activo"});
        modeloTabla.addRow(new Object[]{"bodega1", "María González","Bodeguero",     "Activo"});
        modeloTabla.addRow(new Object[]{"cajero1", "Carlos López",  "Consulta",      "Inactivo"});
    }

    private JButton crearBoton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 32));
        return btn;
    }

    public JButton getBtnNuevoUsuario() { return btnNuevoUsuario; }
    public JButton getBtnEditar()       { return btnEditar; }
    public JButton getBtnDesactivar()   { return btnDesactivar; }
    public JTable getTblUsuarios()      { return tblUsuarios; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void limpiarTabla() { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }

    public int getFilaSeleccionada() { return tblUsuarios.getSelectedRow(); }
    public String getValorFila(int fila, int columna) {
        return (String) modeloTabla.getValueAt(fila, columna);
    }
}