package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Promocion;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.PromocionRepository;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificacionesIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PromocionRepository promociones;

    @Test
    void conservaLecturaYArchivoDeUnMismoAvisoPorCuenta() throws Exception {
        String primeraCuenta = crearCliente();
        String segundaCuenta = crearCliente();
        Promocion promocion = new Promocion();
        promocion.setTitulo("Promoción de integración");
        promocion.setDescripcion("Aviso válido para ambas cuentas");
        String clave = "promocion-" + promociones.save(promocion).getId();
        String avisoNoLeido = "data-clave=\"" + clave + "\" data-leida=\"false\"";
        String avisoLeido = "data-clave=\"" + clave + "\" data-leida=\"true\"";

        mvc.perform(get("/cliente/notificaciones").with(user(primeraCuenta).roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(avisoNoLeido)))
                .andExpect(content().string(containsString("Carnet digital")));

        mvc.perform(post("/cliente/api/notificaciones/leer").with(user(primeraCuenta).roles("CLIENTE")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/cliente/notificaciones").with(user(primeraCuenta).roles("CLIENTE")))
                .andExpect(content().string(containsString(avisoLeido)));

        mvc.perform(post("/cliente/api/notificaciones/archivar").param("clave", clave)
                        .with(user(primeraCuenta).roles("CLIENTE")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/cliente/notificaciones").with(user(primeraCuenta).roles("CLIENTE")))
                .andExpect(content().string(not(containsString(clave))));
        mvc.perform(get("/cliente/notificaciones").with(user(segundaCuenta).roles("CLIENTE")))
                .andExpect(content().string(containsString(avisoNoLeido)));
    }

    private String crearCliente() {
        Usuario usuario = new Usuario();
        usuario.setUsername(UUID.randomUUID() + "@example.test");
        usuario.setPassword(UUID.randomUUID().toString());
        usuario.setRol(Rol.CLIENTE);
        return usuarios.save(usuario).getUsername();
    }
}
