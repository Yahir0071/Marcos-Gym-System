package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.UsuarioService;
import pe.edu.utp.Grupo04.service.impl.SocioServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SocioServiceTest {

    @Mock
    private SocioRepository socioRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private SocioServiceImpl socioService;

    private Socio socioNuevo;

    @BeforeEach
    void setUp() {
        socioNuevo = new Socio();
        socioNuevo.setDni("75849302");
        socioNuevo.setNombres("Valeria");
        socioNuevo.setApellidos("Castro");
        socioNuevo.setTelefono("912345678");
    }

    @Test
    @DisplayName("RF02 - Debe registrar un nuevo socio y su usuario de acceso correspondiente")
    void testRegistrarSocio_Exitoso() {
        when(socioRepository.findByDni("75849302")).thenReturn(Optional.empty());

        Usuario usuarioCreado = new Usuario();
        usuarioCreado.setId(10L);
        usuarioCreado.setUsername("valeria@energygym.pe");
        usuarioCreado.setRol(Rol.CLIENTE);
        usuarioCreado.setActivo(true);

        when(usuarioService.registrarUsuario(eq("valeria@energygym.pe"), eq("pass123"), eq(Rol.CLIENTE)))
                .thenReturn(usuarioCreado);

        when(socioRepository.save(any(Socio.class))).thenAnswer(i -> {
            Socio s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        Socio guardado = socioService.registrarSocio(socioNuevo, "valeria@energygym.pe", "pass123");

        assertNotNull(guardado);
        assertEquals(1L, guardado.getId());
        assertEquals("75849302", guardado.getDni());
        assertNotNull(guardado.getUsuario());
        assertEquals("valeria@energygym.pe", guardado.getUsuario().getUsername());
        verify(socioRepository, times(1)).save(socioNuevo);
    }

    @Test
    @DisplayName("RF02 - Debe rechazar el registro y lanzar excepción si el DNI ya está registrado")
    void testRegistrarSocio_DniDuplicado_LanzaExcepcion() {
        when(socioRepository.findByDni("75849302")).thenReturn(Optional.of(socioNuevo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            socioService.registrarSocio(socioNuevo, "valeria@energygym.pe", "pass123");
        });

        assertTrue(ex.getMessage().contains("Ya existe un socio registrado"));
        verify(usuarioService, never()).registrarUsuario(any(), any(), any());
        verify(socioRepository, never()).save(any());
    }
}
