package pe.com.proveperu.sgc.cliente.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
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
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.cliente.application.service.PermisosCliente;
import pe.com.proveperu.sgc.cliente.domain.model.ClientePrecioEspecial;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClientePrecioEspecialRepository;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class ClientesIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClientePrecioEspecialRepository precioEspecialRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    private Producto producto;

    @BeforeEach
    void crearProductoBase() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría clientes " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setCodigo(("CL" + sufijo).toUpperCase());
        unidad.setNombre("Unidad clientes " + sufijo);
        unidad.setPermiteDecimales(true);
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        unidad = unidadMedidaRepository.save(unidad);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidad);
        producto.setCodigoInterno(("CLI-" + sufijo).toUpperCase());
        producto.setNombre("Producto para cliente " + sufijo);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);
    }

    @Test
    void clientesRequierenTokenYPermisoEspecifico() throws Exception {
        mockMvc.perform(get("/api/v1/clientes"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/clientes")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/clientes")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.CLIENTES_VER)))
            .andExpect(status().isOk());
    }

    @Test
    void registraPersonaNaturalYRecuperaExistenteSinDuplicar() throws Exception {
        String dni = nuevoDni();
        String body = personaNatural(dni, "María Elena", "Torres Vega", true);
        MvcResult creacion = crearCliente(body)
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.tipoPersona").value("NATURAL"))
            .andExpect(jsonPath("$.tipoDocumento").value("DNI"))
            .andExpect(jsonPath("$.numeroDocumento").value(dni))
            .andExpect(jsonPath("$.nombreMostrar").value("María Elena Torres Vega"))
            .andExpect(jsonPath("$.permiteCredito").value(true))
            .andReturn();
        Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");

        crearCliente(body)
            .andExpect(status().isOk())
            .andExpect(header().string("X-Recurso-Existente", "true"))
            .andExpect(jsonPath("$.id").value(id.longValue()));

        mockMvc.perform(get("/api/v1/clientes")
                .param("buscar", dni)
                .param("tipoPersona", "NATURAL")
                .param("permiteCredito", "true")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.CLIENTES_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(id.longValue()));
    }

    @Test
    void registraActualizaEInactivaPersonaJuridica() throws Exception {
        String ruc = nuevoRuc();
        MvcResult creacion = crearCliente(personaJuridica(ruc, "Distribuidora Andina SAC"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoPersona").value("JURIDICA"))
            .andExpect(jsonPath("$.razonSocial").value("Distribuidora Andina SAC"))
            .andReturn();
        Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/v1/clientes/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.CLIENTES_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(personaJuridica(ruc, "Distribuidora Andina del Perú SAC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.razonSocial").value("Distribuidora Andina del Perú SAC"))
            .andExpect(jsonPath("$.correo").value("ventas@andina.pe"));

        mockMvc.perform(patch("/api/v1/clientes/{id}/estado", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.CLIENTES_ESTADO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void rechazaDatosQueNoCorrespondenAlTipoDePersona() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.CLIENTES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoPersona": "NATURAL",
                      "tipoDocumento": "RUC",
                      "numeroDocumento": "%s",
                      "nombres": "Persona",
                      "apellidos": "Inválida",
                      "razonSocial": null,
                      "permiteCredito": false
                    }
                    """.formatted(nuevoRuc())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.datosTipoPersonaValidos").exists());
    }

    @Test
    void impideAsignarAUnClienteElDocumentoDeOtro() throws Exception {
        String primerDni = nuevoDni();
        String segundoDni = nuevoDni();
        Number primerId = idDe(crearCliente(personaNatural(
            primerDni,
            "Primer",
            "Cliente",
            false
        )).andExpect(status().isCreated()).andReturn());
        crearCliente(personaNatural(segundoDni, "Segundo", "Cliente", false))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/clientes/{id}", primerId.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.CLIENTES_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(personaNatural(segundoDni, "Primer", "Cliente", false)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Ya existe un cliente con ese número de documento"
            ));
    }

    @Test
    void registraPreciosEspecialesYCierraLaVigenciaAnterior() throws Exception {
        Number idCliente = idDe(crearCliente(personaNatural(
            nuevoDni(),
            "Cliente",
            "Preferencial",
            true
        )).andExpect(status().isCreated()).andReturn());

        crearPrecio(idCliente.longValue(), "100.00", "2026-01-01", null)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.precio").value(100.0))
            .andExpect(jsonPath("$.vigenteHasta").isEmpty());

        crearPrecio(idCliente.longValue(), "90.00", "2026-02-01", null)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.precio").value(90.0));

        mockMvc.perform(get("/api/v1/clientes/{idCliente}/precios-especiales", idCliente)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.PRECIOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].precio").value(90.0))
            .andExpect(jsonPath("$[1].precio").value(100.0))
            .andExpect(jsonPath("$[1].vigenteHasta").value("2026-01-31"));

        List<ClientePrecioEspecial> registrados = precioEspecialRepository
            .findAllByClienteIdOrderByProductoNombreAscVigenteDesdeDesc(idCliente.longValue());
        assertThat(registrados).hasSize(2);
        assertThat(registrados.get(1).getVigenteHasta()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void rechazaPrecioSolapadoEIncluyePreciosEnElHistorial() throws Exception {
        Number idCliente = idDe(crearCliente(personaJuridica(
            nuevoRuc(),
            "Cliente con historial SAC"
        )).andExpect(status().isCreated()).andReturn());

        crearPrecio(idCliente.longValue(), "80.00", "2026-03-01", "2026-03-31")
            .andExpect(status().isCreated());
        crearPrecio(idCliente.longValue(), "70.00", "2026-03-15", "2026-04-15")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "La vigencia del precio especial se superpone con otro precio activo"
            ));

        mockMvc.perform(get("/api/v1/clientes/{id}/historial", idCliente)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.HISTORIAL_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cliente.id").value(idCliente.longValue()))
            .andExpect(jsonPath("$.resumen.totalOperaciones").value(0))
            .andExpect(jsonPath("$.resumen.importeTotal").value(0.0))
            .andExpect(jsonPath("$.resumen.saldoPendiente").value(0.0))
            .andExpect(jsonPath("$.operaciones").isEmpty())
            .andExpect(jsonPath("$.preciosEspeciales.length()").value(1));
    }

    @Test
    void migracionRegistraYAsignaPermisosDeClientesAlAdministrador() {
        Set<String> esperados = Set.of(
            PermisosCliente.CLIENTES_VER,
            PermisosCliente.CLIENTES_CREAR,
            PermisosCliente.CLIENTES_EDITAR,
            PermisosCliente.CLIENTES_ESTADO,
            PermisosCliente.HISTORIAL_VER,
            PermisosCliente.PRECIOS_VER,
            PermisosCliente.PRECIOS_CREAR
        );
        Set<String> registrados = permisoRepository.findAllByModuloOrderByCodigoAsc("Clientes")
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

    private org.springframework.test.web.servlet.ResultActions crearCliente(String body)
        throws Exception {
        return mockMvc.perform(post("/api/v1/clientes")
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.CLIENTES_CREAR))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions crearPrecio(
        long idCliente,
        String precio,
        String vigenteDesde,
        String vigenteHasta
    ) throws Exception {
        String fechaFin = vigenteHasta == null ? "null" : "\"" + vigenteHasta + "\"";
        return mockMvc.perform(post("/api/v1/clientes/{idCliente}/precios-especiales", idCliente)
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCliente.PRECIOS_CREAR))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idProducto": %d,
                  "precio": %s,
                  "vigenteDesde": "%s",
                  "vigenteHasta": %s
                }
                """.formatted(producto.getId(), precio, vigenteDesde, fechaFin)));
    }

    private String personaNatural(
        String dni,
        String nombres,
        String apellidos,
        boolean permiteCredito
    ) {
        return """
            {
              "tipoPersona": "NATURAL",
              "tipoDocumento": "DNI",
              "numeroDocumento": "%s",
              "nombres": "%s",
              "apellidos": "%s",
              "razonSocial": null,
              "nombreComercial": null,
              "direccion": "Av. Principal 123",
              "telefono": "014567890",
              "whatsapp": "999888777",
              "correo": "CLIENTE@EJEMPLO.PE",
              "permiteCredito": %s
            }
            """.formatted(dni, nombres, apellidos, permiteCredito);
    }

    private String personaJuridica(String ruc, String razonSocial) {
        return """
            {
              "tipoPersona": "JURIDICA",
              "tipoDocumento": "RUC",
              "numeroDocumento": "%s",
              "nombres": null,
              "apellidos": null,
              "razonSocial": "%s",
              "nombreComercial": "Andina",
              "direccion": "Jr. Comercio 456",
              "telefono": "016667777",
              "whatsapp": "988777666",
              "correo": "VENTAS@ANDINA.PE",
              "permiteCredito": true
            }
            """.formatted(ruc, razonSocial);
    }

    private Number idDe(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }

    private String nuevoDni() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000));
    }

    private String nuevoRuc() {
        return "20" + ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000);
    }

    private String bearer(String... authorities) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgc-proveperu")
            .subject("administrador-clientes-test")
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(3600))
            .claim("userId", 999_995L)
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
