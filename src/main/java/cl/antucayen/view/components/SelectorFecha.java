package cl.antucayen.view.components;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Selector de fecha Swing liviano, sin dependencias externas.
 * Permite mantener un campo editable manualmente y seleccionar la fecha
 * desde un calendario mensual pequeño.
 */
public final class SelectorFecha {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CL");
    private static final DateTimeFormatter TITULO_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_ES);

    private SelectorFecha() {
    }

    public static void mostrar(Component padre, JTextField campo,
                               DateTimeFormatter formatoCampo) {
        LocalDate inicial = LocalDate.now();
        String texto = campo.getText().trim();
        if (!texto.isEmpty()) {
            try {
                inicial = LocalDate.parse(texto, formatoCampo);
            } catch (DateTimeParseException ignored) {
                // Si el usuario dejó un texto inválido, el calendario parte hoy.
            }
        }

        Window owner = SwingUtilities.getWindowAncestor(padre);
        CalendarioDialogo dialogo = new CalendarioDialogo(owner, inicial, fecha ->
                campo.setText(formatoCampo.format(fecha)));
        dialogo.setVisible(true);
    }

    private interface SeleccionFecha {
        void aceptar(LocalDate fecha);
    }

    private static final class CalendarioDialogo extends JDialog {

        private YearMonth mesActual;
        private LocalDate fechaSeleccionada;
        private final SeleccionFecha callback;
        private final JLabel lblMes = new JLabel("", SwingConstants.CENTER);
        private final JPanel panelDias = new JPanel(new GridLayout(0, 7, 3, 3));

        CalendarioDialogo(Window owner, LocalDate inicial, SeleccionFecha callback) {
            super(owner, "Seleccionar fecha", ModalityType.APPLICATION_MODAL);
            this.mesActual = YearMonth.from(inicial);
            this.fechaSeleccionada = inicial;
            this.callback = callback;
            construirInterfaz();
            refrescarCalendario();
        }

        private void construirInterfaz() {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(false);
            setSize(320, 300);
            setLocationRelativeTo(getOwner());
            setLayout(new BorderLayout(8, 8));

            JPanel cabecera = new JPanel(new BorderLayout(6, 0));
            cabecera.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

            JButton btnAnterior = new JButton("<");
            JButton btnSiguiente = new JButton(">");
            btnAnterior.setToolTipText("Mes anterior");
            btnSiguiente.setToolTipText("Mes siguiente");
            lblMes.setFont(new Font("Arial", Font.BOLD, 14));

            btnAnterior.addActionListener(e -> {
                mesActual = mesActual.minusMonths(1);
                refrescarCalendario();
            });
            btnSiguiente.addActionListener(e -> {
                mesActual = mesActual.plusMonths(1);
                refrescarCalendario();
            });

            cabecera.add(btnAnterior, BorderLayout.WEST);
            cabecera.add(lblMes, BorderLayout.CENTER);
            cabecera.add(btnSiguiente, BorderLayout.EAST);

            panelDias.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            JButton btnHoy = new JButton("Hoy");
            btnHoy.addActionListener(e -> seleccionar(LocalDate.now()));
            JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
            pie.add(btnHoy);

            add(cabecera, BorderLayout.NORTH);
            add(panelDias, BorderLayout.CENTER);
            add(pie, BorderLayout.SOUTH);
        }

        private void refrescarCalendario() {
            panelDias.removeAll();
            lblMes.setText(capitalizar(mesActual.atDay(1).format(TITULO_MES)));

            String[] encabezados = {"Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do"};
            for (String encabezado : encabezados) {
                JLabel lbl = new JLabel(encabezado, SwingConstants.CENTER);
                lbl.setFont(new Font("Arial", Font.BOLD, 11));
                panelDias.add(lbl);
            }

            int espacios = mesActual.atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < espacios; i++) {
                panelDias.add(new JLabel(""));
            }

            for (int dia = 1; dia <= mesActual.lengthOfMonth(); dia++) {
                LocalDate fecha = mesActual.atDay(dia);
                JButton btnDia = new JButton(Integer.toString(dia));
                btnDia.setMargin(new Insets(2, 2, 2, 2));
                btnDia.setFocusPainted(false);
                if (fecha.equals(fechaSeleccionada)) {
                    btnDia.setFont(btnDia.getFont().deriveFont(Font.BOLD));
                }
                btnDia.addActionListener(e -> seleccionar(fecha));
                panelDias.add(btnDia);
            }

            panelDias.revalidate();
            panelDias.repaint();
        }

        private void seleccionar(LocalDate fecha) {
            fechaSeleccionada = fecha;
            callback.aceptar(fecha);
            dispose();
        }

        private static String capitalizar(String texto) {
            if (texto == null || texto.isEmpty()) return texto;
            return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
        }
    }
}
