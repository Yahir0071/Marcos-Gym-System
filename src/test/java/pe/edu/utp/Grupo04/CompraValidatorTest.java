package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import pe.edu.utp.Grupo04.dto.CompraItemDTO;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.validation.CompraValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CompraValidatorTest {

    private CompraValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CompraValidator();
    }

    @Test
    @DisplayName("Validador - Debe aceptar compra válida de productos")
    void testValidate_CompraValida() {
        CompraRequestDTO dto = new CompraRequestDTO();
        dto.setTipoConcepto("PRODUCTO");
        dto.setBuyerName("Carlos Mendoza");
        dto.setBuyerDocType("DNI");
        dto.setBuyerDocNum("72563123");
        dto.setBuyerEmail("carlos@ejemplo.com");
        dto.setEstado("PAGADO");
        dto.setItems(List.of(new CompraItemDTO(1L, "Creatina", "Suplementos", "/img/creatina.jpg", 95.00, 1)));

        Errors errors = new BeanPropertyBindingResult(dto, "compraRequestDTO");
        validator.validate(dto, errors);

        assertFalse(errors.hasErrors(), "No debería tener errores de validación");
    }

    @Test
    @DisplayName("Validador - Debe rechazar DNI con caracteres no numéricos o longitud distinta a 8")
    void testValidate_DniInvalido() {
        CompraRequestDTO dto = new CompraRequestDTO();
        dto.setBuyerDocType("DNI");
        dto.setBuyerDocNum("7256ABC"); // Menos de 8 y con letras
        dto.setItems(List.of(new CompraItemDTO(1L, "Shaker", "Botellas", "/img/shaker.jpg", 45.00, 1)));

        Errors errors = new BeanPropertyBindingResult(dto, "compraRequestDTO");
        validator.validate(dto, errors);

        assertTrue(errors.hasFieldErrors("buyerDocNum"));
    }

    @Test
    @DisplayName("Validador - Debe rechazar compra de productos con carrito vacío")
    void testValidate_CarritoVacio() {
        CompraRequestDTO dto = new CompraRequestDTO();
        dto.setTipoConcepto("PRODUCTO");
        dto.setBuyerDocType("DNI");
        dto.setBuyerDocNum("72563123");
        dto.setItems(List.of()); // Carrito vacío

        Errors errors = new BeanPropertyBindingResult(dto, "compraRequestDTO");
        validator.validate(dto, errors);

        assertTrue(errors.hasFieldErrors("items"));
    }

    @Test
    @DisplayName("Validador - Debe rechazar compra de membresía si no se especifica planId")
    void testValidate_MembresiaSinPlan() {
        CompraRequestDTO dto = new CompraRequestDTO();
        dto.setTipoConcepto("MEMBRESIA");
        dto.setPlanId(null);
        dto.setBuyerDocType("DNI");
        dto.setBuyerDocNum("72563123");

        Errors errors = new BeanPropertyBindingResult(dto, "compraRequestDTO");
        validator.validate(dto, errors);

        assertTrue(errors.hasFieldErrors("planId"));
    }
}
