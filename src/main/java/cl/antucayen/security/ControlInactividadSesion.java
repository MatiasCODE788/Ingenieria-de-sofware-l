package cl.antucayen.security;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.time.Duration;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Vigila inactividad mientras hay una sesión abierta en las ventanas Swing
 * de este proceso, incluidos diálogos independientes. No registra actividad
 * producida en otras aplicaciones.
 * El listener y el temporizador se eliminan al detener el controlador.
 */
public final class ControlInactividadSesion {
    public static final int MINUTOS_PREDETERMINADOS = 30;
    private static final int MINUTOS_MAXIMOS = 1440;
    private static final String PROPIEDAD = "antucayen.sesion.inactividad.minutos";

    private final Window ventanaPrincipal;
    private final Runnable alExpirar;
    private final long limiteNanos;
    private final Timer temporizador;
    private final AWTEventListener oyente;
    private long ultimaActividadNanos;
    private boolean activo;

    public static int minutosConfigurados() {
        String valor = System.getProperty(PROPIEDAD, "").trim();
        try {
            int minutos = Integer.parseInt(valor);
            return minutos >= 1 && minutos <= MINUTOS_MAXIMOS
                    ? minutos : MINUTOS_PREDETERMINADOS;
        } catch (NumberFormatException ex) {
            return MINUTOS_PREDETERMINADOS;
        }
    }

    public ControlInactividadSesion(Window ventanaPrincipal, Runnable alExpirar) {
        this(ventanaPrincipal, alExpirar, Duration.ofMinutes(minutosConfigurados()));
    }

    // Constructor también útil para verificar límites de tiempo sin esperar 30 min.
    public ControlInactividadSesion(Window ventanaPrincipal, Runnable alExpirar, Duration limite) {
        if (ventanaPrincipal == null || alExpirar == null || limite == null
                || limite.isZero() || limite.isNegative()) {
            throw new IllegalArgumentException("Parámetros de inactividad inválidos");
        }
        this.ventanaPrincipal = ventanaPrincipal;
        this.alExpirar = alExpirar;
        this.limiteNanos = limite.toNanos();
        this.temporizador = new Timer(1000, e -> verificarVencimiento());
        this.oyente = this::registrarActividad;
    }

    /** Inicio/detención exclusivamente desde el hilo de eventos de Swing. */
    public void iniciar() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Debe iniciarse desde el hilo de eventos Swing");
        }
        if (activo) return;
        activo = true;
        ultimaActividadNanos = System.nanoTime();
        Toolkit.getDefaultToolkit().addAWTEventListener(oyente,
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
                        | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        temporizador.start();
    }

    public void detener() {
        if (!activo) return;
        activo = false;
        temporizador.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(oyente);
    }

    private void registrarActividad(AWTEvent evento) {
        if (!activo || !(evento instanceof InputEvent)
                || !(evento.getSource() instanceof Component componente)
                || !perteneceAlSistema(componente)) {
            return;
        }
        long ahora = System.nanoTime();
        if (vencio(ultimaActividadNanos, ahora, limiteNanos)) {
            expirar();
        } else {
            ultimaActividadNanos = ahora;
        }
    }

    private boolean perteneceAlSistema(Component componente) {
        // Varias vistas abren diálogos sin owner (JOptionPane, facturas):
        // pertenecen al MISMO JVM, así que también cuentan como actividad.
        // Los eventos de aplicaciones ajenas nunca llegan a este listener.
        Window ventana = SwingUtilities.getWindowAncestor(componente);
        return ventana != null && ventana.isShowing() && ventanaPrincipal.isDisplayable();
    }

    private void verificarVencimiento() {
        if (activo && vencio(ultimaActividadNanos, System.nanoTime(), limiteNanos)) {
            expirar();
        }
    }

    // Package-private para prueba de frontera exacta y ausencia de overflow temporal.
    static boolean vencio(long ultimaActividad, long ahora, long limite) {
        return ahora - ultimaActividad >= limite;
    }

    private void expirar() {
        detener();
        alExpirar.run();
    }
}
