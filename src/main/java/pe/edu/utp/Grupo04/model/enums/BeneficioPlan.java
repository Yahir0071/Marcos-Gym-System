package pe.edu.utp.Grupo04.model.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogo de beneficios predefinidos para los planes de membresía.
 */
public enum BeneficioPlan {

    MUSCULACION_PESO_LIBRE("Máquinas de musculación y peso libre", "Máquinas de musculación y peso libre"),
    ZONA_CARDIO_HIIT("Zona cardio HIIT y máquinas biomecánicas", "Zona cardio HIIT y máquinas biomecánicas"),
    CARNET_DIGITAL("Carnet digital con código QR", "Carnet digital"),
    EVALUACION_FISICA_MENSUAL("Evaluación física y bioimpedancia mensual", "Evaluación física y bioimpedancia mensual"),
    ZONA_PREMIUM("Acceso a Zona Premium", "Acceso a Zona Premium"),
    PASES_INVITADO_2("2 pases para invitados cada mes", "2 pases de invitado al mes"),
    PASES_INVITADO_4("4 pases para invitados cada mes", "4 pases de invitado al mes"),
    DESCUENTO_SUPLEMENTOS_5("5% de descuento en suplementos", "5% dscto. en suplementos"),
    DESCUENTO_SUPLEMENTOS_10("10% de descuento en suplementos", "10% dscto. en suplementos"),
    PAUSA_VIAJE_SALUD("Pausa por viaje o salud (hasta 15 días)", "Pausa por viaje o salud (hasta 15 días)");

    private final String etiqueta;
    private final String etiquetaComparador;

    BeneficioPlan(String etiqueta, String etiquetaComparador) {
        this.etiqueta = etiqueta;
        this.etiquetaComparador = etiquetaComparador;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getEtiquetaComparador() {
        return etiquetaComparador;
    }

    /**
     * Interpreta los códigos guardados en BD; si el plan aún no tiene beneficios registrados,
     * aplica una configuración por defecto según su nombre para mantener compatibilidad.
     */
    public static List<BeneficioPlan> parsear(String raw, String nombrePlan) {
        if (raw != null && !raw.isBlank()) {
            Set<BeneficioPlan> seleccionados = EnumSet.noneOf(BeneficioPlan.class);
            for (String token : raw.split(",")) {
                String limpio = token.strip().toUpperCase(Locale.ROOT);
                if (limpio.isEmpty()) {
                    continue;
                }
                try {
                    seleccionados.add(valueOf(limpio));
                } catch (IllegalArgumentException ignored) {
                    // Ignora códigos obsoletos sin romper la vista
                }
            }
            return Arrays.stream(values()).filter(seleccionados::contains).toList();
        }
        return porDefectoSegunNombre(nombrePlan);
    }

    public static List<BeneficioPlan> porDefectoSegunNombre(String nombrePlan) {
        List<BeneficioPlan> lista = new ArrayList<>();
        lista.add(MUSCULACION_PESO_LIBRE);
        lista.add(ZONA_CARDIO_HIIT);
        lista.add(CARNET_DIGITAL);
        String upper = nombrePlan == null ? "" : nombrePlan.toUpperCase(Locale.ROOT);
        if (upper.contains("PREMIUM") || upper.contains("VIP")) {
            lista.add(EVALUACION_FISICA_MENSUAL);
            lista.add(ZONA_PREMIUM);
            lista.add(PASES_INVITADO_4);
            lista.add(DESCUENTO_SUPLEMENTOS_10);
        } else if (upper.contains("PLUS")) {
            lista.add(EVALUACION_FISICA_MENSUAL);
            lista.add(ZONA_PREMIUM);
            lista.add(PASES_INVITADO_2);
            lista.add(DESCUENTO_SUPLEMENTOS_5);
        }
        lista.add(PAUSA_VIAJE_SALUD);
        return lista;
    }

    public static String serializar(List<BeneficioPlan> beneficios) {
        if (beneficios == null || beneficios.isEmpty()) {
            return "";
        }
        return beneficios.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
