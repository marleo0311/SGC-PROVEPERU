package pe.com.proveperu.sgc.security.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.security.domain.model.EstadoRegistro;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTests {

    private static final String LOGIN = "jwt-test@proveperu.test";
    private static final String PASSWORD = "Contrasena-segura-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void crearUsuarioActivo() {
        Rol rol = new Rol();
        rol.setNombre("Rol prueba JWT");
        rol.setDescripcion("Rol temporal para pruebas de autenticación");
        rol.setEstado(EstadoRegistro.ACTIVO);
        rol = rolRepository.save(rol);

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombreCompleto("Usuario de prueba JWT");
        usuario.setUsuarioLogin(LOGIN);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);
    }

    @Test
    void loginCorrectoDevuelveJwtSinDatosSensibles() throws Exception {
        MvcResult result = realizarLogin(PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.tipo").value("Bearer"))
            .andExpect(jsonPath("$.usuario.usuarioLogin").value(LOGIN))
            .andExpect(jsonPath("$.usuario.nombreCompleto").value("Usuario de prueba JWT"))
            .andExpect(jsonPath("$.usuario.rol").value("ROL_PRUEBA_JWT"))
            .andExpect(jsonPath("$.usuario.password").doesNotExist())
            .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).doesNotContain(PASSWORD).doesNotContain("$2");
    }

    @Test
    void credencialesIncorrectasDevuelvenUnauthorized() throws Exception {
        MvcResult result = realizarLogin("contrasena-incorrecta")
            .andExpect(status().isUnauthorized())
            .andReturn();

        assertThat(result.getResponse().getContentAsString())
            .doesNotContain(PASSWORD)
            .doesNotContain("passwordHash")
            .doesNotContain("$2");
    }

    @Test
    void solicitudDeLoginIncompletaDevuelveBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void endpointMeRequiereTokenValido() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());

        MvcResult login = realizarLogin(PASSWORD)
            .andExpect(status().isOk())
            .andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuarioLogin").value(LOGIN))
            .andExpect(jsonPath("$.rol").value("ROL_PRUEBA_JWT"));
    }

    @Test
    void tokenVencidoEsRechazado() throws Exception {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgc-proveperu")
            .subject(LOGIN)
            .issuedAt(ahora.minusSeconds(7200))
            .expiresAt(ahora.minusSeconds(3600))
            .claim("userId", 1L)
            .claim("role", "ROL_PRUEBA_JWT")
            .claim("authorities", List.of("ROLE_ROL_PRUEBA_JWT"))
            .build();
        String tokenVencido = jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
            claims
        )).getTokenValue();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVencido))
            .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions realizarLogin(String password)
        throws Exception {
        String request = """
            {
              "usuarioLogin": "%s",
              "password": "%s"
            }
            """.formatted(LOGIN, password);

        return mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request));
    }
}
