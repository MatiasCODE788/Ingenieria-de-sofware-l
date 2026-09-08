package cl.antucayen.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/**
 * Permite usar una pistola lectora de código de barras (USB, modo teclado)
 * para ingresar datos en el sistema.
 *
 * Un escáner "escribe" el código muy rápido (unos pocos milisegundos entre
 * caracteres) y termina enviando Enter, como si alguien tipeara larguísimo
 * y presionara Enter al final.
 *
 * Casos que cubre:
 *  1) El cajero escanea directo sobre un campo de texto con foco (el buscador,
 *     el campo "código de barras" al crear un producto, etc.): funciona igual
 *     que si lo tipeara a mano, sin cambios — Swing ya lo maneja bien.
 *  2) El cajero escanea sin haber hecho clic en ningún campo (por ejemplo,
 *     recién hizo clic en la tabla del carrito, o en un botón): en ese caso
 *     esta clase captura las teclas a nivel global de la aplicación y las
 *     redirige igual al campo configurado, simulando que se escribió el
 *     código ahí y se presionó Enter.
 */
public final class EscanerGlobal {

    private static final long INTERVALO_MAX_MS = 60; // separación típica entre caracteres de un escáner
    private static final int  LARGO_MINIMO      = 3;  // ignora rachas de teclas sueltas / accidentales

    private static JTextField        destinoActual;
    private static Supplier<Boolean> activoActual;
    private static boolean           dispatcherInstalado = false;

    private EscanerGlobal() {}

    /**
     * Activa (o reemplaza) el campo donde se vuelcan los escaneos capturados
     * globalmente. Se puede llamar varias veces (por ejemplo, cada vez que se
     * recrea la pantalla de ventas) sin duplicar el listener.
     *
     * @param destino         campo de texto que recibirá el código escaneado
     * @param activoSiVisible debe devolver true solo cuando esa pantalla está
     *                        visible, para no interferir con otras pantallas
     */
    public static synchronized void activar(JTextField destino, Supplier<Boolean> activoSiVisible) {
        destinoActual = destino;
        activoActual  = activoSiVisible;
        if (!dispatcherInstalado) {
            instalarDispatcher();
            dispatcherInstalado = true;
        }
    }

    private static void instalarDispatcher() {
        StringBuilder buffer = new StringBuilder();
        long[] ultimoTecleo = {0L};

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(evento -> {
            if (activoActual == null || destinoActual == null || !Boolean.TRUE.equals(activoActual.get())) {
                return false;
            }

            Component foco = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            // Si ya hay un campo de texto con el foco (incluido el propio destino),
            // dejamos que Swing lo maneje normal: escanear ahí ya funciona solo.
            if (foco instanceof JTextComponent) return false;

            long ahora = System.currentTimeMillis();

            if (evento.getID() == KeyEvent.KEY_TYPED) {
                char c = evento.getKeyChar();
                if (ahora - ultimoTecleo[0] > INTERVALO_MAX_MS) buffer.setLength(0);
                ultimoTecleo[0] = ahora;
                if (c != '\n' && c != '\r' && c != KeyEvent.CHAR_UNDEFINED) buffer.append(c);
                return true; // no había ningún campo enfocado, así que estas teclas no iban a ningún lado
            }

            if (evento.getID() == KeyEvent.KEY_PRESSED && evento.getKeyCode() == KeyEvent.VK_ENTER) {
                boolean huboCodigo = buffer.length() >= LARGO_MINIMO;
                String codigo = buffer.toString();
                buffer.setLength(0);
                if (huboCodigo) {
                    JTextField destino = destinoActual;
                    SwingUtilities.invokeLater(() -> {
                        destino.setText(codigo);
                        destino.postActionEvent(); // dispara el mismo listener que usa el Enter manual
                    });
                    return true;
                }
            }
            return false;
        });
    }
}