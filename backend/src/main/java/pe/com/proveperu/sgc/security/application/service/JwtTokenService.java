package pe.com.proveperu.sgc.security.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.config.JwtProperties;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock jwtClock;

    public String generar(Authentication authentication, Usuario usuario) {
        Instant emitidoEn = jwtClock.instant();
        Instant expiraEn = emitidoEn.plus(properties.getExpiration());
        List<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .sorted()
            .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .issuedAt(emitidoEn)
            .expiresAt(expiraEn)
            .subject(usuario.getUsuarioLogin())
            .claim("userId", usuario.getId())
            .claim("role", RolAuthorityMapper.normalizar(usuario.getRol().getNombre()))
            .claim("authorities", authorities)
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
            .type("JWT")
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
