package pe.com.proveperu.sgc.catalogo.api.controller;

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
import java.util.UUID;
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
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.MarcaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class CatalogosBasicosIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Test
    void endpointsRequierenTokenYPermisoEspecifico() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_VER)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_VER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Sin permiso de creación\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void creaActualizaInactivaYBuscaCategoria() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        MvcResult creacion = mockMvc.perform(post("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "  Espumas %s  ",
                      "descripcion": "  Productos de espuma  "
                    }
                    """.formatted(sufijo)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.nombre").value("Espumas " + sufijo))
            .andExpect(jsonPath("$.descripcion").value("Productos de espuma"))
            .andExpect(jsonPath("$.estado").value("ACTIVO"))
            .andReturn();

        Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(put("/api/v1/categorias/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "Espumas actualizadas %s",
                      "descripcion": "Descripción actualizada"
                    }
                    """.formatted(sufijo)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Espumas actualizadas " + sufijo));

        mockMvc.perform(patch("/api/v1/categorias/{id}/estado", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_ESTADO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVO"));

        mockMvc.perform(get("/api/v1/categorias")
                .param("buscar", sufijo)
                .param("estado", "INACTIVO")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id.longValue()));

        assertThat(categoriaRepository.findById(id.longValue()).orElseThrow().getEstado())
            .isEqualTo(EstadoCatalogo.INACTIVO);
    }

    @Test
    void rechazaCategoriaDuplicadaSinImportarMayusculas() throws Exception {
        String nombre = "Categoría duplicada " + UUID.randomUUID();
        String body = "{\"nombre\":\"" + nombre + "\"}";

        mockMvc.perform(post("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CATEGORIAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre.toUpperCase() + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe una categoría con ese nombre"));
    }

    @Test
    void creaYActualizaMarca() throws Exception {
        String nombre = "Marca " + UUID.randomUUID();
        MvcResult creacion = mockMvc.perform(post("/api/v1/marcas")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.MARCAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("ACTIVO"))
            .andReturn();

        Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(put("/api/v1/marcas/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.MARCAS_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "%s actualizada",
                      "estado": "INACTIVO"
                    }
                    """.formatted(nombre)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value(nombre + " actualizada"))
            .andExpect(jsonPath("$.estado").value("INACTIVO"));

        mockMvc.perform(get("/api/v1/marcas")
                .param("buscar", nombre)
                .param("estado", "INACTIVO")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.MARCAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id.longValue()));

        assertThat(marcaRepository.findById(id.longValue())).isPresent();
    }

    @Test
    void creaYActualizaUnidadNormalizandoElCodigo() throws Exception {
        String codigo = "u" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult creacion = mockMvc.perform(post("/api/v1/unidades-medida")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.UNIDADES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "codigo": "%s",
                      "nombre": "Unidad de prueba",
                      "permiteDecimales": true
                    }
                    """.formatted(codigo.toLowerCase())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value(codigo.toUpperCase()))
            .andExpect(jsonPath("$.permiteDecimales").value(true))
            .andReturn();

        Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");
        String nuevoCodigo = "n" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(put("/api/v1/unidades-medida/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.UNIDADES_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "codigo": "%s",
                      "nombre": "Unidad actualizada",
                      "permiteDecimales": false,
                      "estado": "INACTIVO"
                    }
                    """.formatted(nuevoCodigo.toLowerCase())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value(nuevoCodigo.toUpperCase()))
            .andExpect(jsonPath("$.permiteDecimales").value(false))
            .andExpect(jsonPath("$.estado").value("INACTIVO"));

        mockMvc.perform(get("/api/v1/unidades-medida")
                .param("buscar", nuevoCodigo)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.UNIDADES_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id.longValue()));

        assertThat(unidadMedidaRepository.findById(id.longValue()).orElseThrow().getCodigo())
            .isEqualTo(nuevoCodigo.toUpperCase());
    }

    @Test
    void validaCamposObligatoriosDeUnidad() throws Exception {
        mockMvc.perform(post("/api/v1/unidades-medida")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.UNIDADES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"UND\",\"nombre\":\"Unidad\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.permiteDecimales").exists());
    }

    @Test
    void migracionAsignaLosPermisosDeCatalogoAlAdministrador() {
        assertThat(permisoRepository.findAllByModuloOrderByCodigoAsc("Catálogo"))
            .hasSize(10);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        Rol rolConPermisos = rolRepository.findByIdWithPermisos(administrador.getId()).orElseThrow();
        Set<String> codigos = rolConPermisos.getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .filter(codigo -> codigo.startsWith("CAT_"))
            .collect(java.util.stream.Collectors.toSet());

        assertThat(codigos).containsExactlyInAnyOrder(
            PermisosCatalogo.CATEGORIAS_VER,
            PermisosCatalogo.CATEGORIAS_CREAR,
            PermisosCatalogo.CATEGORIAS_EDITAR,
            PermisosCatalogo.CATEGORIAS_ESTADO,
            PermisosCatalogo.MARCAS_VER,
            PermisosCatalogo.MARCAS_CREAR,
            PermisosCatalogo.MARCAS_EDITAR,
            PermisosCatalogo.UNIDADES_VER,
            PermisosCatalogo.UNIDADES_CREAR,
            PermisosCatalogo.UNIDADES_EDITAR
        );
    }

    private String bearer(String... authorities) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgc-proveperu")
            .subject("administrador-catalogo-test")
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(3600))
            .claim("userId", 999_998L)
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
