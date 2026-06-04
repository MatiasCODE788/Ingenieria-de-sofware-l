package cl.antucayen.model.entity;

import java.time.LocalDateTime;

public class AjusteInventario {
    private int           idAjuste;
    private LocalDateTime fechaHora;
    private String        modalidadAjuste;
    private String        estadoAjuste;
    private int           idUsuario;

    public AjusteInventario() {}

    public AjusteInventario(int idAjuste, LocalDateTime fechaHora,
                            String modalidadAjuste, String estadoAjuste,
                            int idUsuario) {
        this.idAjuste        = idAjuste;
        this.fechaHora       = fechaHora;
        this.modalidadAjuste = modalidadAjuste;
        this.estadoAjuste    = estadoAjuste;
        this.idUsuario       = idUsuario;
    }

    public int           getIdAjuste()        { return idAjuste; }
    public LocalDateTime getFechaHora()        { return fechaHora; }
    public String        getModalidadAjuste()  { return modalidadAjuste; }
    public String        getEstadoAjuste()     { return estadoAjuste; }
    public int           getIdUsuario()        { return idUsuario; }

    public void setIdAjuste(int idAjuste)               { this.idAjuste = idAjuste; }
    public void setFechaHora(LocalDateTime fechaHora)   { this.fechaHora = fechaHora; }
    public void setModalidadAjuste(String modalidad)    { this.modalidadAjuste = modalidad; }
    public void setEstadoAjuste(String estadoAjuste)    { this.estadoAjuste = estadoAjuste; }
    public void setIdUsuario(int idUsuario)             { this.idUsuario = idUsuario; }
}