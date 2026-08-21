package pe.com.proveperu.sgc.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Permiso;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioUserDetailsServiceTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioUserDetailsService(usuarioRepository);
    }

    @Test
    void cargaUsuarioActivoConRolYPermisos() {
        Usuario usuario = crearUsuario(EstadoUsuario.ACTIVO);
        when(usuarioRepository.findByUsuarioLoginIgnoreCase("admin"))
            .thenReturn(Optional.of(usuario));

        UserDetails resultado = service.loadUserByUsername("admin");

        assertThat(resultado.getUsername()).isEqualTo("admin");
        assertThat(resultado.getPassword()).isEqualTo("hash-bcrypt-de-prueba");
        assertThat(resultado.isEnabled()).isTrue();
        assertThat(resultado.getAuthorities())
            .extracting("authority")
            .containsExactlyInAnyOrder("ROLE_ADMINISTRADOR", "USUARIO_LEER");
    }

    @Test
    void deshabilitaUsuarioSuspendido() {
        Usuario usuario = crearUsuario(EstadoUsuario.SUSPENDIDO);
        when(usuarioRepository.findByUsuarioLoginIgnoreCase("admin"))
            .thenReturn(Optional.of(usuario));

        UserDetails resultado = service.loadUserByUsername("admin");

        assertThat(resultado.isEnabled()).isFalse();
    }

    @Test
    void rechazaUsuarioInexistenteSinRevelarDetalles() {
        when(usuarioRepository.findByUsuarioLoginIgnoreCase("desconocido"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("desconocido"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Usuario o contraseña inválidos");
    }

    private Usuario crearUsuario(EstadoUsuario estado) {
        Permiso permiso = new Permiso();
        permiso.setCodigo("USUARIO_LEER");
        permiso.setNombre("Consultar usuarios");
        permiso.setModulo("SEGURIDAD");

        Rol rol = new Rol();
        rol.setNombre("Administrador");
        rol.setPermisos(Set.of(permiso));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombreCompleto("Administrador de prueba");
        usuario.setUsuarioLogin("admin");
        usuario.setPasswordHash("hash-bcrypt-de-prueba");
        usuario.setEstado(estado);
        return usuario;
    }
}
