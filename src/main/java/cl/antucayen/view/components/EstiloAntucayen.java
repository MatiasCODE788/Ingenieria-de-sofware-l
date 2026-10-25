package cl.antucayen.view.components;

import cl.antucayen.util.GestorTemas;
import cl.antucayen.util.Tema;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Sistema de diseño compartido por las ventanas Swing de Antucayen.
 * No crea ni reemplaza controles de negocio: solo modifica sus propiedades visuales.
 */
public final class EstiloAntucayen {
    public static final Color NAVY = new Color(7, 39, 93);
    public static final Color BLUE = new Color(20, 86, 212);
    public static final Color HOVER = new Color(12, 69, 169);
    public static final Color TEXT = new Color(18, 39, 77);
    public static final Color MUTED = new Color(106, 124, 153);
    public static final Color BACKGROUND = new Color(246, 248, 252);
    public static final Color BORDER = new Color(226, 233, 244);
    public static final Color PALE = new Color(236, 243, 255);

    private EstiloAntucayen() {}

    public static Color primario() {
        Tema tema = GestorTemas.getInstancia().getTema();
        return tema == Tema.AZUL_CORPORATIVO ? BLUE : tema.colorPrimario;
    }

    public static Color cabecera() {
        Tema tema = GestorTemas.getInstancia().getTema();
        return tema == Tema.AZUL_CORPORATIVO ? NAVY : tema.gradienteInicio;
    }

    /** Se invoca antes de construir las ventanas, después de instalar FlatLaf. */
    public static void iniciar() {
        UIManager.put("defaultFont", new Font("Dialog", Font.PLAIN, 12));
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Button.margin", new Insets(8, 14, 8, 14));
        UIManager.put("TextField.margin", new Insets(7, 10, 7, 10));
        UIManager.put("PasswordField.margin", new Insets(7, 10, 7, 10));
        UIManager.put("ComboBox.padding", new Insets(6, 10, 6, 10));
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.showVerticalLines", Boolean.FALSE);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.rowHeight", 35);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TableHeader.background", new Color(248, 250, 254));
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("Table.selectionBackground", PALE);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TabbedPane.selectedBackground", PALE);
        UIManager.put("TabbedPane.underlineColor", primario());
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.focusColor", primario());
    }

    /** Estiliza controles existentes, conservando modelos, listeners y editores de negocio. */
    public static void aplicar(Component componente) {
        if (componente instanceof JPanel panel) {
            if (BACKGROUND_OLD.equals(panel.getBackground())) panel.setBackground(BACKGROUND);
            else if (new Color(17, 24, 39).equals(panel.getBackground()))
                panel.setBackground(cabecera());
        }
        if (componente instanceof JTable tabla) {
            tabla.setFont(new Font("Dialog", Font.PLAIN, 12));
            tabla.setRowHeight(Math.max(34, tabla.getRowHeight()));
            tabla.setGridColor(BORDER);
            tabla.setSelectionBackground(PALE);
            tabla.setSelectionForeground(TEXT);
            tabla.setShowVerticalLines(false);
            tabla.setFillsViewportHeight(true);
            JTableHeader cab = tabla.getTableHeader();
            if (cab != null) {
                cab.setBackground(new Color(247, 249, 253));
                cab.setForeground(TEXT);
                cab.setFont(new Font("Dialog", Font.BOLD, 12));
                cab.setPreferredSize(new Dimension(0, 36));
            }
        } else if (componente instanceof JScrollPane scroll) {
            if (scroll.getViewport().getView() instanceof JTable) {
                scroll.setBorder(BorderFactory.createLineBorder(BORDER));
                scroll.getViewport().setBackground(Color.WHITE);
            }
        } else if (componente instanceof JButton boton) {
            Color fondo = boton.getBackground();
            if (fondo != null && (fondo.equals(new Color(5, 150, 105)) ||
                    fondo.equals(new Color(37, 99, 235)) ||
                    fondo.equals(new Color(2, 132, 199)) ||
                    fondo.equals(new Color(124, 58, 237)))) {
                boton.setBackground(primario());
                boton.setForeground(Color.WHITE);
            }
            boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boton.putClientProperty("JButton.buttonType", "roundRect");
        } else if (componente instanceof JTextField campo) {
            campo.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        } else if (componente instanceof JComboBox<?> combo) {
            combo.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        } else if (componente instanceof JLabel etiqueta) {
            if (new Color(17, 24, 39).equals(etiqueta.getForeground())) etiqueta.setForeground(TEXT);
            if (new Color(107, 114, 128).equals(etiqueta.getForeground())) etiqueta.setForeground(MUTED);
        }
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) aplicar(hijo);
        }
    }

    private static final Color BACKGROUND_OLD = new Color(243, 244, 246);
}
