package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.enums.PeriodoMembresia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

public record TarifaInscripcion(String periodo, int meses, int porcentajePeriodo, BigDecimal subtotal,
                                BigDecimal descuentoCupon, BigDecimal total) {

    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    public static final Set<String> CUPONES_DEMO = Set.of("ENERGY2X1", "APERTURA10", "SHAKERPRO");

    public static TarifaInscripcion calcular(Plan plan, String periodo, String cupon) {
        PeriodoMembresia duracion = PeriodoMembresia.interpretar(periodo);
        int meses = duracion.getMeses();
        int descuentoPeriodo = meses == 12 ? 20 : meses == 3 ? 10 : 0;
        long subtotal = Math.round(plan.getPrecio() * meses);
        long monto = Math.round(duracion.total(plan.getPrecio()));
        long descuentoCupon = CUPONES_DEMO.contains(cupon == null ? "" : cupon.strip().toUpperCase(Locale.ROOT))
                ? Math.min(30, Math.max(0, monto - 1)) : 0;
        return new TarifaInscripcion(periodo, meses, descuentoPeriodo, BigDecimal.valueOf(subtotal),
                BigDecimal.valueOf(descuentoCupon), BigDecimal.valueOf(monto - descuentoCupon));
    }

    public LocalDate fechaFin(LocalDate inicio, int diasPlanMensual) {
        return switch (periodo) {
            case "ANUAL" -> inicio.plusYears(1);
            case "TRIMESTRAL" -> inicio.plusMonths(3);
            default -> inicio.plusDays(diasPlanMensual);
        };
    }
}
