package pe.com.proveperu.sgc.proveedor.api.controller;

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
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.proveedor.application.service.PermisosProveedor;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class ProveedoresIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Test
    void proveedoresRequierenTokenYPermisoEspecifico() throws Exception {
        mockMvc.perform(get("/api/v1/proveedores"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/proveedores")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/proveedores")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_VER)))
            .andExpect(status().isOk());
    }

    @Test
    void registraConsultaYBuscaProveedor() throws Exception {
        String ruc = nuevoRuc();
        MvcResult creacion = crearProveedor(ruc, "Distribuciones del Pacífico SAC")
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.ruc").value(ruc))
            .andExpect(jsonPath("$.razonSocial").value("Distribuciones del Pacífico SAC"))
            .andExpect(jsonPath("$.correo").value("compras@pacifico.pe"))
            .andExpect(jsonPath("$.personaContacto").value("Ana Torres"))
            .andExpect(jsonPath("$.estado").value("ACTIVO"))
            .andReturn();
        Number id = idDe(creacion);

        mockMvc.perform(get("/api/v1/proveedores/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ruc").value(ruc));

        mockMvc.perform(get("/api/v1/proveedores")
                .param("buscar", "Pacífico")
                .param("estado", "ACTIVO")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(id.longValue()));
    }

    @Test
    void rechazaRucDuplicadoAlCrearOActualizar() throws Exception {
        String primerRuc = nuevoRuc();
        String segundoRuc = nuevoRuc();
        Number primerId = idDe(crearProveedor(primerRuc, "Proveedor Uno SAC")
            .andExpect(status().isCreated())
            .andReturn());
        crearProveedor(segundoRuc, "Proveedor Dos SAC")
            .andExpect(status().isCreated());

        crearProveedor(primerRuc, "Proveedor repetido SAC")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe un proveedor con ese RUC"));

        mockMvc.perform(put("/api/v1/proveedores/{id}", primerId.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyProveedor(segundoRuc, "Proveedor Uno modificado SAC")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe un proveedor con ese RUC"));
    }

    @Test
    void actualizaEInactivaProveedor() throws Exception {
        String ruc = nuevoRuc();
        Number id = idDe(crearProveedor(ruc, "Proveedor Original SAC")
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(put("/api/v1/proveedores/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyProveedor(ruc, "Proveedor Actualizado SAC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.razonSocial").value("Proveedor Actualizado SAC"));

        mockMvc.perform(patch("/api/v1/proveedores/{id}/estado", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_ESTADO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void validaRucRazonSocialYCorreo() throws Exception {
        mockMvc.perform(post("/api/v1/proveedores")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "ruc": "1234",
                      "razonSocial": " ",
                      "correo": "correo-invalido"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.ruc").exists())
            .andExpect(jsonPath("$.errores.razonSocial").exists())
            .andExpect(jsonPath("$.errores.correo").exists());
    }

    @Test
    void consultaHistorialPreparadoParaLasCompras() throws Exception {
        Number id = idDe(crearProveedor(nuevoRuc(), "Proveedor Historial SAC")
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(get("/api/v1/proveedores/{id}/compras", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.HISTORIAL_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.proveedor.id").value(id.longValue()))
            .andExpect(jsonPath("$.resumen.totalCompras").value(0))
            .andExpect(jsonPath("$.resumen.importeTotal").value(0.0))
            .andExpect(jsonPath("$.resumen.saldoPendiente").value(0.0))
            .andExpect(jsonPath("$.resumen.ultimaCompra").isEmpty())
            .andExpect(jsonPath("$.compras").isEmpty());
    }

    @Test
    void migracionRegistraYAsignaPermisosDeProveedoresAlAdministrador() {
        Set<String> esperados = Set.of(
            PermisosProveedor.PROVEEDORES_VER,
            PermisosProveedor.PROVEEDORES_CREAR,
            PermisosProveedor.PROVEEDORES_EDITAR,
            PermisosProveedor.PROVEEDORES_ESTADO,
            PermisosProveedor.HISTORIAL_VER
        );
        Set<String> registrados = permisoRepository.findAllByModuloOrderByCodigoAsc("Proveedores")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(java.util.stream.Collectors.toSet());
        assertThat(registrados).containsExactlyInAnyOrderElementsOf(esperados);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        Set<String> asignados = rolRepository.findByIdWithPermisos(administrador.getId()).orElseThrow()
            .getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .filter(esperados::contains)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(asignados).containsExactlyInAnyOrderElementsOf(esperados);
    }

    private org.springframework.test.web.servlet.ResultActions crearProveedor(
        String ruc,
        String razonSocial
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/proveedores")
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosProveedor.PROVEEDORES_CREAR))
            .contentType(MediaType.APPLICATION_JSON)
            .content(bodyProveedor(ruc, razonSocial)));
    }

    private String bodyProveedor(String ruc, String razonSocial) {
        return """
            {
              "ruc": "%s",
              "razonSocial": "%s",
              "nombreComercial": "Pacífico",
              "direccion": "Av. Industrial 456",
              "telefono": "014446666",
              "correo": "COMPRAS@PACIFICO.PE",
              "personaContacto": "Ana Torres"
            }
            """.formatted(ruc, razonSocial);
    }

    private Number idDe(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }

    private String nuevoRuc() {
        return "20" + ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000);
    }

    private String bearer(String... authorities) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgc-proveperu")
            .subject("administrador-proveedores-test")
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(3600))
            .claim("userId", 999_994L)
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
