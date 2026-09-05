package cl.antucayen.util;

import java.awt.Color;

/**
 * Los 5 temas visuales disponibles para el sistema.
 * Cada uno define su propia paleta de colores; las vistas (VLogin, VPrincipal, etc.)
 * leen los colores desde GestorTemas.getInstancia().getPaleta() en vez de tenerlos
 * escritos directamente en el código.
 */
public enum Tema {

    AZUL_CORPORATIVO("Azul Corporativo",
            new Color(15, 30, 56), new Color(26, 74, 110),   // gradiente login
            new Color(15, 30, 56), new Color(30, 58, 95),    // primario / hover
            new Color(5, 150, 105),                          // acento (logo)
            new Color(17, 24, 39), new Color(31, 41, 55), new Color(156, 163, 175), // sidebar
            new Color(243, 244, 246)),                       // fondo área principal

    VERDE_ESMERALDA("Verde Esmeralda",
            new Color(6, 78, 59), new Color(16, 122, 91),
            new Color(6, 95, 70), new Color(4, 120, 87),
            new Color(217, 119, 6),
            new Color(6, 41, 33), new Color(6, 60, 48), new Color(167, 202, 190),
            new Color(240, 253, 244)),

    PURPURA_ELEGANTE("Púrpura Elegante",
            new Color(49, 21, 82), new Color(91, 42, 143),
            new Color(76, 29, 149), new Color(107, 51, 178),
            new Color(236, 72, 153),
            new Color(30, 17, 51), new Color(49, 30, 79), new Color(196, 181, 219),
            new Color(245, 243, 255)),

    NARANJA_CALIDO("Naranja Cálido",
            new Color(120, 53, 15), new Color(194, 65, 12),
            new Color(154, 52, 18), new Color(194, 65, 12),
            new Color(21, 128, 61),
            new Color(69, 26, 3), new Color(120, 53, 15), new Color(253, 186, 116),
            new Color(255, 247, 237)),

    OSCURO_NOCTURNO("Oscuro Nocturno",
            new Color(2, 6, 23), new Color(30, 41, 59),
            new Color(8, 145, 178), new Color(14, 165, 233),
            new Color(8, 145, 178),
            new Color(2, 6, 23), new Color(15, 23, 42), new Color(148, 163, 184),
            new Color(15, 23, 42));

    private final String nombre;
    public final Color gradienteInicio, gradienteFin;
    public final Color colorPrimario, colorPrimarioHover;
    public final Color colorAcento;
    public final Color sidebarFondo, sidebarHoverFondo, sidebarTextoInactivo;
    public final Color areaFondo;

    Tema(String nombre,
         Color gradienteInicio, Color gradienteFin,
         Color colorPrimario, Color colorPrimarioHover,
         Color colorAcento,
         Color sidebarFondo, Color sidebarHoverFondo, Color sidebarTextoInactivo,
         Color areaFondo) {
        this.nombre = nombre;
        this.gradienteInicio = gradienteInicio;
        this.gradienteFin = gradienteFin;
        this.colorPrimario = colorPrimario;
        this.colorPrimarioHover = colorPrimarioHover;
        this.colorAcento = colorAcento;
        this.sidebarFondo = sidebarFondo;
        this.sidebarHoverFondo = sidebarHoverFondo;
        this.sidebarTextoInactivo = sidebarTextoInactivo;
        this.areaFondo = areaFondo;
    }

    public String getNombre() { return nombre; }

    @Override
    public String toString() { return nombre; }
}