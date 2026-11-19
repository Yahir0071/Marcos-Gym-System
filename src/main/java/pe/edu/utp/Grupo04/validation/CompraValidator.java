package pe.edu.utp.Grupo04.validation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import pe.edu.utp.Grupo04.dto.CompraItemDTO;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;

import java.util.Set;

/**
 * Validador personalizado de Spring para el proceso de compra (simulada).
 * Solo valida la FORMA de la solicitud: los precios, el stock y el propietario
 * se verifican en OrdenCompraService, con datos de la base de datos.
 */
@Component
public class CompraValidator implements Validator {

    private static final Set<String> ESTADOS_PERMITIDOS = Set.of(
            "PAGADO", "EN_PROCESO", "RECHAZADO", "CANCELADO"
    );
    private static final Set<String> CONCEPTOS_PERMITIDOS = Set.of("PRODUCTO", "MEMBRESIA");

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

        // 1. Documento de identidad (solo si se envía)
        if ("DNI".equalsIgnoreCase(dto.getBuyerDocType())) {
            if (dto.getBuyerDocNum() != null && !dto.getBuyerDocNum().matches("^\\d{8}$")) {
                errors.rejectValue("buyerDocNum", "documento.dni.invalido",
                        "El DNI debe contener exactamente 8 dígitos numéricos");
            }
        }

        // 2. Tipo de concepto
        if (dto.getTipoConcepto() != null
                && !CONCEPTOS_PERMITIDOS.contains(dto.getTipoConcepto().toUpperCase())) {
            errors.rejectValue("tipoConcepto", "concepto.invalido",
                    "El tipo de compra debe ser PRODUCTO o MEMBRESIA");
        }

        // 3. Validación según el concepto
        if ("MEMBRESIA".equalsIgnoreCase(dto.getTipoConcepto())) {
            if (dto.getPlanId() == null || dto.getPlanId() <= 0) {
                errors.rejectValue("planId", "plan.requerido",
                        "Debe seleccionar un plan de membresía válido");
            }
        } else {
            if (dto.getItems() == null || dto.getItems().isEmpty()) {
                errors.rejectValue("items", "items.vacio",
                        "El carrito de compras debe contener al menos un producto");
            } else {
                for (CompraItemDTO item : dto.getItems()) {
                    if (item == null) {   // items:[null] ya no provoca HTTP 500
                        errors.rejectValue("items", "item.nulo",
                                "El carrito contiene un producto inválido");
                        continue;
                    }
                    if (item.getId() == null) {
                        errors.rejectValue("items", "item.sinId",
                                "Falta el identificador de un producto");
                    }
                    if (item.getQty() == null || item.getQty() <= 0) {
                        errors.rejectValue("items", "item.cantidad.invalida",
                                "La cantidad de cada producto debe ser mayor a cero");
                    }
                }
            }
        }

        // 4. Estado de la simulación de pago
        if (dto.getEstado() != null && !ESTADOS_PERMITIDOS.contains(dto.getEstado().toUpperCase())) {
            errors.rejectValue("estado", "estado.noPermitido",
                    "El estado '" + dto.getEstado() + "' no es un estado de transacción válido");
        }
    }
}