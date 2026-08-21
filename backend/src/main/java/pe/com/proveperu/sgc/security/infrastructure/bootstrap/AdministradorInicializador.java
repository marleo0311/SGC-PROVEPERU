package pe.com.proveperu.sgc.security.infrastructure.bootstrap;

import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pe.com.proveperu.sgc.security.domain.model.EstadoRegistro;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true")
public class AdministradorInicializador implements ApplicationRunner {

    private static final String NOMBRE_ROL_ADMINISTRADOR = "Administrador";
    private static final int LONGITUD_MINIMA_CONTRASENA = 12;

    private final AdministradorInicialProperties properties;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarDatosNoSensibles();

        String loginNormalizado = properties.getLogin().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> usuarioExistente =
            usuarioRepository.findByUsuarioLoginIgnoreCase(loginNormalizado);

        if (usuarioExistente.isPresent() && !properties.isResetPassword()) {
            log.info("El usuario administrador inicial ya existe; no se realizaron cambios");
            return;
        }

        validarContrasena();

        if (usuarioExistente.isPresent()) {
            Usuario administrador = usuarioExistente.get();
            administrador.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
            usuarioRepository.save(administrador);
            log.info("Contraseña del administrador restablecida correctamente");
            return;
        }

        Rol rolAdministrador = rolRepository
            .findByNombreIgnoreCase(NOMBRE_ROL_ADMINISTRADOR)
            .orElseGet(this::crearRolAdministrador);

        if (rolAdministrador.getEstado() != EstadoRegistro.ACTIVO) {
            rolAdministrador.setEstado(EstadoRegistro.ACTIVO);
            rolAdministrador = rolRepository.save(rolAdministrador);
        }

        Usuario administrador = new Usuario();
        administrador.setRol(rolAdministrador);
        administrador.setNombreCompleto(properties.getNombreCompleto().trim());
        administrador.setUsuarioLogin(loginNormalizado);
        administrador.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        administrador.setEstado(EstadoUsuario.ACTIVO);

        usuarioRepository.save(administrador);
        log.info("Usuario administrador inicial creado correctamente");
    }

    private void validarDatosNoSensibles() {
        if (!StringUtils.hasText(properties.getLogin())) {
            throw new IllegalStateException("ADMIN_INITIAL_LOGIN es obligatorio");
        }
        if (!StringUtils.hasText(properties.getNombreCompleto())) {
            throw new IllegalStateException("ADMIN_INITIAL_NAME es obligatorio");
        }
    }

    private void validarContrasena() {
        if (!StringUtils.hasText(properties.getPassword())
            || properties.getPassword().length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new IllegalStateException(
                "ADMIN_INITIAL_PASSWORD debe tener al menos 12 caracteres"
            );
        }
    }

    private Rol crearRolAdministrador() {
        Rol rol = new Rol();
        rol.setNombre(NOMBRE_ROL_ADMINISTRADOR);
        rol.setDescripcion("Acceso administrativo general al sistema");
        rol.setEstado(EstadoRegistro.ACTIVO);
        return rolRepository.save(rol);
    }
}
