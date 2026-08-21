package pe.com.proveperu.sgc.security.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import pe.com.proveperu.sgc.security.application.service.PermisosSeguridad;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Permiso;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class AdministracionSeguridadIntegrationTests {

    private static final String PASSWORD_INICIAL = "Contrasena-Inicial-123";
    private static final String PASSWORD_NUEVA = "Contrasena-Nueva-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Rol rolAdministrador;

    @BeforeEach
    void cargarDatosBase() {
        rolAdministrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
    }

    @Test
    void endpointsRequierenTokenYPermisoEspecifico() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/usuarios")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/usuarios")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.USUARIOS_VER)))
            .andExpect(status().isOk());
    }

    @Test
    void crearYBuscarUsuarioNoExponeDatosSensibles() throws Exception {
        String login = "usuario-" + UUID.randomUUID() + "@proveperu.test";
        String request = """
            {
              "nombreCompleto": "Usuario creado por API",
              "usuarioLogin": "%s",
              "password": "%s",
              "idRol": %d
            }
            """.formatted(login.toUpperCase(), PASSWORD_INICIAL, rolAdministrador.getId());

        MvcResult creacion = mockMvc.perform(post("/api/v1/usuarios")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.USUARIOS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.usuarioLogin").value(login))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();

        String contenido = creacion.getResponse().getContentAsString();
        Number id = JsonPath.read(contenido, "$.id");
        assertThat(contenido).doesNotContain(PASSWORD_INICIAL).doesNotContain("$2");

        Usuario guardado = usuarioRepository.findById(id.longValue()).orElseThrow();
        assertThat(passwordEncoder.matches(PASSWORD_INICIAL, guardado.getPasswordHash())).isTrue();

        mockMvc.perform(get("/api/v1/usuarios")
                .param("buscar", login)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.USUARIOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(id.longValue()))
            .andExpect(jsonPath("$.contenido[0].passwordHash").doesNotExist());
    }

    @Test
    void actualizaDatosEstadoYPasswordDelUsuario() throws Exception {
        Usuario usuario = crearUsuario("objetivo-" + UUID.randomUUID() + "@proveperu.test");
        String nuevoLogin = "actualizado-" + UUID.randomUUID() + "@proveperu.test";

        mockMvc.perform(put("/api/v1/usuarios/{id}", usuario.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.USUARIOS_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombreCompleto": "Nombre actualizado",
                      "usuarioLogin": "%s",
                      "idRol": %d
                    }
                    """.formatted(nuevoLogin.toUpperCase(), rolAdministrador.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreCompleto").value("Nombre actualizado"))
            .andExpect(jsonPath("$.usuarioLogin").value(nuevoLogin));

        mockMvc.perform(patch("/api/v1/usuarios/{id}/estado", usuario.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.USUARIOS_ESTADO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"SUSPENDIDO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));

        mockMvc.perform(patch("/api/v1/usuarios/{id}/password", usuario.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.USUARIOS_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"" + PASSWORD_NUEVA + "\"}"))
            .andExpect(status().isNoContent());

        usuarioRepository.flush();
        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        assertThat(passwordEncoder.matches(PASSWORD_NUEVA, actualizado.getPasswordHash())).isTrue();
    }

    @Test
    void passwordCortoEsRechazado() throws Exception {
        Usuario usuario = crearUsuario("validacion-" + UUID.randomUUID() + "@proveperu.test");

        mockMvc.perform(patch("/api/v1/usuarios/{id}/password", usuario.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.USUARIOS_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"corta\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void creaRolYActualizaSusPermisos() throws Exception {
        Permiso permisoVerUsuarios = permisoRepository.findByCodigo(PermisosSeguridad.USUARIOS_VER)
            .orElseThrow();
        Permiso permisoVerRoles = permisoRepository.findByCodigo(PermisosSeguridad.ROLES_VER)
            .orElseThrow();
        String nombreRol = "Rol API " + UUID.randomUUID();

        MvcResult creacion = mockMvc.perform(post("/api/v1/roles")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.ROLES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "%s",
                      "descripcion": "Creado desde la API",
                      "idsPermisos": [%d]
                    }
                    """.formatted(nombreRol, permisoVerUsuarios.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value(nombreRol))
            .andExpect(jsonPath("$.permisos.length()").value(1))
            .andReturn();

        Number idRol = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch("/api/v1/roles/{id}/permisos", idRol.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.ROLES_PERMISOS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idsPermisos\":[" + permisoVerUsuarios.getId() + "," +
                    permisoVerRoles.getId() + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permisos.length()").value(2));

        mockMvc.perform(get("/api/v1/roles/{id}", idRol.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.ROLES_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permisos[*].codigo").value(
                org.hamcrest.Matchers.hasItems(PermisosSeguridad.USUARIOS_VER, PermisosSeguridad.ROLES_VER)
            ));
    }

    @Test
    void listaPermisosYProtegeLosObligatoriosDelAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/permisos")
                .param("modulo", "seguridad")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.PERMISOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.codigo == 'SEG_USUARIOS_VER')]").exists());

        mockMvc.perform(patch("/api/v1/roles/{id}/permisos", rolAdministrador.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosSeguridad.ROLES_PERMISOS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idsPermisos\":[]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "El rol Administrador debe conservar todos los permisos de seguridad obligatorios"
            ));
    }

    private Usuario crearUsuario(String login) {
        Usuario usuario = new Usuario();
        usuario.setRol(rolAdministrador);
        usuario.setNombreCompleto("Usuario objetivo");
        usuario.setUsuarioLogin(login);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD_INICIAL));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        return usuarioRepository.save(usuario);
    }

    private String bearer(String... authorities) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgc-proveperu")
            .subject("administrador-api-test")
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(3600))
            .claim("userId", 999_999L)
            .claim("role", "ADMINISTRADOR")
            .claim("authorities", List.of(authorities))
            .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
            claims
        )).getTokenValue();
        return "Bearer " + token;
    }
}
