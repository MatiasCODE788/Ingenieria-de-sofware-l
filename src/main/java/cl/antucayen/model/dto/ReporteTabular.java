package cl.antucayen.model.dto;

import java.util.List;

/** Resultado tabular inmutable para reportes RF-57 a RF-61. */
public record ReporteTabular(String titulo, List<String> columnas, List<List<Object>> filas) {
    public ReporteTabular {
        columnas = columnas == null ? List.of() : List.copyOf(columnas);
        filas = filas == null ? List.of() : filas.stream().map(List::copyOf).toList();
    }

    public boolean estaVacio() {
        return filas.isEmpty();
    }
}
