package com.example.reconsenastransito.model;

public class detecSenaTransito {
    private String id;
    private String fechaDect;
    private String hora;
    private  String senaTransito;
    private  float predicSena;

    private String idConductor;



    public detecSenaTransito() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFechaDect() {
        return fechaDect;
    }

    public void setFechaDect(String fechaDect) {
        this.fechaDect = fechaDect;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getSenaTransito() {
        return senaTransito;
    }

    public void setSenaTransito(String senaTransito) {
        this.senaTransito = senaTransito;
    }

    public float getPredicSena() {
        return predicSena;
    }

    public void setPredicSena(float predicSena) {
        this.predicSena = predicSena;
    }

    public String getIdConductor() {
        return idConductor;
    }

    public void setIdConductor(String idConductor) {
        this.idConductor = idConductor;
    }

}

