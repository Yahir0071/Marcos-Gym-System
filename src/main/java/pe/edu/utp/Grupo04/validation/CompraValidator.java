package pe.edu.utp.Grupo04.validation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import pe.edu.utp.Grupo04.dto.CompraItemDTO;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;

import java.util.Set;

/**
 * Validador personalizado de Spring Framework para el proceso de compra y pagos online.
 * Cumple con el estándar de arquitectura indicado en la Figura 5 y sección 3.6.1 del informe del proyecto.
 */
@Component
public class CompraValidator implements Validator {

    private static final Set<String> ESTADOS_PERMITIDOS = Set.of(
            "PAGADO", "EN_PROCESO", "RECHAZADO", "CANCELADO"
    );

    @Override
    public boolean supports(Class<?> clazz) {
        return CompraRequestDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target == null) {
            errors.reject("compra.nula", "La solicitud de compra no puede ser nula");
            return;
        }

        CompraRequestDTO dto = (CompraRequestDTO) target;

        // 1. Validación de documento de identidad
        if ("DNI".equalsIgnoreCase(dto.getBuyerDocType())) {
            if (dto.getBuyerDocNum() != null && !dto.getBuyerDocNum().matches("^\\d{8}$")) {
                errors.rejectValue("buyerDocNum", "documento.dni.invalido",
                        "El DNI debe contener exactamente 8 dígitos numéricos");
            }
        }

        // 2. Validación según el tipo de concepto (PRODUCTO o MEMBRESIA)
        if ("MEMBRESIA".equalsIgnoreCase(dto.getTipoConcepto())) {
            if (dto.getPlanId() == null || dto.getPlanId() <= 0) {
                errors.rejectValue("planId", "plan.requerido",
                        "Debe seleccionar un plan de membresía válido");
            }
        } else {
            // Caso PRODUCTO (por defecto)
            if (dto.getItems() == null || dto.getItems().isEmpty()) {
                errors.rejectValue("items", "items.vacio",
                        "El carrito de compras debe contener al menos un producto");
            } else {
                for (int i = 0; i < dto.getItems().size(); i++) {
                    CompraItemDTO item = dto.getItems().get(i);
                    if (item.getQty() == null || item.getQty() <= 0) {
                        errors.rejectValue("items[" + i + "].qty", "item.cantidad.invalida",
                                "La cantidad del producto '" + item.getName() + "' debe ser mayor a cero");
                    }
                    if (item.getPrice() == null || item.getPrice() < 0) {
                        errors.rejectValue("items[" + i + "].price", "item.precio.invalido",
                                "El precio unitario no puede ser negativo");
                    }
                }
            }
        }

        // 3. Validación de estado permitido
        if (dto.getEstado() != null && !ESTADOS_PERMITIDOS.contains(dto.getEstado().toUpperCase())) {
            errors.rejectValue("estado", "estado.noPermitido",
                    "El estado '" + dto.getEstado() + "' no es un estado de transacción válido");
        }
    }
}
