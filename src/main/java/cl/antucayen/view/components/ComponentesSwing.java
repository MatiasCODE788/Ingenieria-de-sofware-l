package cl.antucayen.view.components;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/** Componentes visuales reutilizables, independientes de cualquier vista de negocio. */
public final class ComponentesSwing {

    private ComponentesSwing() {}

    public static JTable crearTabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font("Arial", Font.PLAIN, 13));
        tabla.setRowHeight(36);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(EstiloAntucayen.BORDER);
        tabla.setSelectionBackground(EstiloAntucayen.PALE);
        tabla.setSelectionForeground(EstiloAntucayen.TEXT);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(EstiloAntucayen.TEXT);
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));
        return tabla;
    }

    public static JButton crearBoton(String texto, Color color) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("Arial", Font.BOLD, 12));
        boton.setBackground(color.equals(new Color(37, 99, 235)) ||
                color.equals(new Color(5, 150, 105)) || color.equals(new Color(2, 132, 199)) ||
                color.equals(new Color(124, 58, 237)) || color.equals(new Color(14, 116, 144))
                ? EstiloAntucayen.primario() : color);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.putClientProperty("JButton.buttonType", "roundRect");
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(160, 32));
        return boton;
    }
}
