package pe.edu.utp.Grupo04.model.enums;

import java.util.Locale;

public enum PeriodoMembresia {
    MENSUAL(1, 1.0, "Mensual"),
    TRIMESTRAL(3, 0.90, "Trimestral"),
    ANUAL(12, 0.80, "Anual");

    private final int meses;
    private final double factor;
    private final String etiqueta;

    PeriodoMembresia(int meses, double factor, String etiqueta) {
        this.meses = meses;
        this.factor = factor;
        this.etiqueta = etiqueta;
    }

    public int getMeses() { return meses; }

    public String getEtiqueta() { return etiqueta; }

    public double total(double precioMensual) {
        return Math.round(precioMensual * meses * factor * 100.0) / 100.0;
    }

    public static PeriodoMembresia interpretar(String valor) {
        if (valor == null || valor.isBlank()) {
            return MENSUAL;
        }
        try {
            return valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Elige un periodo mensual, trimestral o anual.");
        }
    }
}
