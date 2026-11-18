package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.dto.SocioActualizarDTO;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
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
        socioNuevo.setId(1L);
        socioNuevo.setDni("75849302");
        socioNuevo.setNombres("Valeria");
        socioNuevo.setApellidos("Castro");
        socioNuevo.setTelefono("912345678");

        Usuario u = new Usuario();
        u.setId(5L);
        u.setUsername("valeria@energygym.pe");
        u.setActivo(true);
        socioNuevo.setUsuario(u);
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

    @Test
    @DisplayName("C08 - Debe actualizar los datos del socio y persistirlos en base de datos")
    void testActualizarSocio_PersistenciaExitosa() {
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socioNuevo));
        when(socioRepository.save(any(Socio.class))).thenAnswer(i -> i.getArgument(0));

        SocioActualizarDTO dto = new SocioActualizarDTO("Valeria Nicole", "Castro Peña", "987654321", true);
        Socio actualizado = socioService.actualizarSocio(1L, dto);

        assertEquals("Valeria Nicole", actualizado.getNombres());
        assertEquals("Castro Peña", actualizado.getApellidos());
        assertEquals("987654321", actualizado.getTelefono());
        assertTrue(actualizado.getUsuario().getActivo());
        verify(socioRepository, times(1)).save(socioNuevo);
    }

    @Test
    @DisplayName("C08 - Debe cambiar el estado activo a falso al desactivar un socio")
    void testCambiarEstadoSocio_Desactivacion() {
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socioNuevo));
        when(socioRepository.save(any(Socio.class))).thenAnswer(i -> i.getArgument(0));

        socioService.cambiarEstado(1L, false);

        assertFalse(socioNuevo.getUsuario().getActivo());
        verify(socioRepository, times(1)).save(socioNuevo);
    }

    @Test
    @DisplayName("M04 - Actualizar socio con ID inexistente debe lanzar RecursoNoEncontradoException")
    void testActualizarSocio_Inexistente() {
        when(socioRepository.findById(999L)).thenReturn(Optional.empty());

        SocioActualizarDTO dto = new SocioActualizarDTO("Nombre", "Apellido", "999888777", true);
        assertThrows(RecursoNoEncontradoException.class, () -> {
            socioService.actualizarSocio(999L, dto);
        });
    }
}
