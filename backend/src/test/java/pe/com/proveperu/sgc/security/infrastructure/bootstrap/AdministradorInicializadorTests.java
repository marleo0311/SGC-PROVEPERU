package pe.com.proveperu.sgc.security.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdministradorInicializadorTests {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdministradorInicialProperties properties;
    private AdministradorInicializador inicializador;

    @BeforeEach
    void setUp() {
        properties = new AdministradorInicialProperties();
        properties.setEnabled(true);
        properties.setLogin(" Admin ");
        properties.setPassword("clave-segura-de-prueba");
        properties.setNombreCompleto("Administrador de prueba");

        inicializador = new AdministradorInicializador(
            properties,
            rolRepository,
            usuarioRepository,
            passwordEncoder
        );
    }

    @Test
    void creaRolYUsuarioConContrasenaCifrada() {
        when(usuarioRepository.existsByUsuarioLoginIgnoreCase("admin")).thenReturn(false);
        when(rolRepository.findByNombreIgnoreCase("Administrador")).thenReturn(Optional.empty());
        when(rolRepository.save(any(Rol.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("clave-segura-de-prueba")).thenReturn("hash-bcrypt");

        inicializador.run(new DefaultApplicationArguments());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario usuarioGuardado = captor.getValue();

        assertThat(usuarioGuardado.getUsuarioLogin()).isEqualTo("admin");
        assertThat(usuarioGuardado.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(usuarioGuardado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuarioGuardado.getRol().getNombre()).isEqualTo("Administrador");
    }

    @Test
    void noReemplazaUnAdministradorExistente() {
        when(usuarioRepository.existsByUsuarioLoginIgnoreCase("admin")).thenReturn(true);

        inicializador.run(new DefaultApplicationArguments());

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
        verify(rolRepository, never()).save(any());
    }

    @Test
    void rechazaUnaContrasenaCorta() {
        properties.setPassword("corta");
        when(usuarioRepository.existsByUsuarioLoginIgnoreCase("admin")).thenReturn(false);

        assertThatThrownBy(() -> inicializador.run(new DefaultApplicationArguments()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("ADMIN_INITIAL_PASSWORD debe tener al menos 12 caracteres");

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }
}
