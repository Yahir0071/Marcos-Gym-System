package pe.edu.utp.Grupo04.model.enums;

public enum Rol {
    CLIENTE("Cliente"),
    RECEPCION("Recepción"),
    ADMINISTRACION("Administración"),
    GESTOR_TECNICO("Gestor técnico");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
