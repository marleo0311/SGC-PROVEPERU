package pe.com.proveperu.sgc.security.application.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Permiso;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class UsuarioUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usuarioLogin) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(usuarioLogin)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña inválidos"));

        Set<GrantedAuthority> autoridades = usuario.getRol().getPermisos().stream()
            .map(Permiso::getCodigo)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        autoridades.add(new SimpleGrantedAuthority(
            "ROLE_" + RolAuthorityMapper.normalizar(usuario.getRol().getNombre())
        ));

        return User.withUsername(usuario.getUsuarioLogin())
            .password(usuario.getPasswordHash())
            .authorities(autoridades)
            .disabled(usuario.getEstado() != EstadoUsuario.ACTIVO)
            .build();
    }
}
