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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Marca;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.MarcaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class ProductosIntegrationTests {

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
    private ProductoRepository productoRepository;

    @Autowired
    private PrecioProductoRepository precioRepository;

    @Autowired
    private ProductoUnidadConversionRepository conversionRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    private Categoria categoria;
    private Marca marca;
    private UnidadMedida unidad;
    private UnidadMedida unidadAlterna;

    @BeforeEach
    void crearCatalogosBase() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        categoria = new Categoria();
        categoria.setNombre("Categoría producto " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        marca = new Marca();
        marca.setNombre("Marca producto " + sufijo);
        marca.setEstado(EstadoCatalogo.ACTIVO);
        marca = marcaRepository.save(marca);

        unidad = crearUnidad("U" + sufijo, "Unidad base " + sufijo, true);
        unidadAlterna = crearUnidad("A" + sufijo, "Unidad alterna " + sufijo, false);
    }

    @Test
    void productosRequierenTokenYPermisoEspecifico() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/productos")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/productos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_VER)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/productos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_CREAR)))
            .andExpect(status().isOk());
    }

    @Test
    void creaProductoConPreciosInicialesYPermiteBuscarlo() throws Exception {
        String codigo = "PROD-" + UUID.randomUUID().toString().substring(0, 8);
        String codigoNormalizado = codigo.toUpperCase();
        MvcResult creacion = mockMvc.perform(post("/api/v1/productos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "codigoInterno": "%s",
                      "codigoBarras": "BAR-%s",
                      "nombre": "Espuma de prueba",
                      "descripcion": "Producto creado desde la API",
                      "idCategoria": %d,
                      "idMarca": %d,
                      "idUnidadBase": %d,
                      "stockMinimo": 5.500,
                      "precioMinorista": 120.00,
                      "precioMayorista": 110.00
                    }
                    """.formatted(
                        codigo.toLowerCase(),
                        codigo,
                        categoria.getId(),
                        marca.getId(),
                        unidad.getId()
                    )))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.codigoInterno").value(codigoNormalizado))
            .andExpect(jsonPath("$.nombre").value("Espuma de prueba"))
            .andExpect(jsonPath("$.categoria.id").value(categoria.getId()))
            .andExpect(jsonPath("$.marca.id").value(marca.getId()))
            .andExpect(jsonPath("$.unidadBase.id").value(unidad.getId()))
            .andExpect(jsonPath("$.estado").value("ACTIVO"))
            .andReturn();

        Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");
        assertThat(precioRepository.findAllByProductoIdOrderByTipoPrecioAscVigenteDesdeDesc(id.longValue()))
            .extracting(precio -> precio.getTipoPrecio())
            .containsExactly("MAYORISTA", "MINORISTA");

        mockMvc.perform(get("/api/v1/productos")
                .param("buscar", codigoNormalizado)
                .param("estado", "ACTIVO")
                .param("idCategoria", categoria.getId().toString())
                .param("page", "0")
                .param("size", "10")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(id.longValue()));

        mockMvc.perform(get("/api/v1/productos")
                .param("buscar", codigoNormalizado)
                .param("idCategoria", "999999999")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(0));

        mockMvc.perform(get("/api/v1/productos/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigoInterno").value(codigoNormalizado));
    }

    @Test
    void actualizaInactivaYRechazaCodigoDuplicado() throws Exception {
        Producto producto = crearProducto("P-A-" + UUID.randomUUID().toString().substring(0, 8));
        Producto otro = crearProducto("P-B-" + UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(put("/api/v1/productos/{id}", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyActualizacion(otro.getCodigoInterno(), "Producto duplicado")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe un producto con ese código interno"));

        String nuevoCodigo = "NUEVO-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(put("/api/v1/productos/{id}", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyActualizacion(nuevoCodigo.toLowerCase(), "Producto actualizado")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigoInterno").value(nuevoCodigo.toUpperCase()))
            .andExpect(jsonPath("$.nombre").value("Producto actualizado"));

        mockMvc.perform(patch("/api/v1/productos/{id}/estado", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_ESTADO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void creaConversionYRechazaDuplicadoInverso() throws Exception {
        Producto producto = crearProducto("CONV-" + UUID.randomUUID().toString().substring(0, 8));
        String body = """
            {
              "idUnidadOrigen": %d,
              "idUnidadDestino": %d,
              "factorConversion": 1000.000000
            }
            """.formatted(unidad.getId(), unidadAlterna.getId());

        mockMvc.perform(post("/api/v1/productos/{id}/conversiones", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CONVERSIONES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.unidadOrigen.id").value(unidad.getId()))
            .andExpect(jsonPath("$.unidadDestino.id").value(unidadAlterna.getId()))
            .andExpect(jsonPath("$.factorConversion").value(1000.0));

        mockMvc.perform(get("/api/v1/productos/{id}/conversiones", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CONVERSIONES_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].idProducto").value(producto.getId()));

        String inversa = """
            {
              "idUnidadOrigen": %d,
              "idUnidadDestino": %d,
              "factorConversion": 0.001000
            }
            """.formatted(unidadAlterna.getId(), unidad.getId());
        mockMvc.perform(post("/api/v1/productos/{id}/conversiones", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CONVERSIONES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inversa))
            .andExpect(status().isConflict());

        assertThat(conversionRepository
            .findAllByProductoIdOrderByUnidadOrigenNombreAscUnidadDestinoNombreAsc(producto.getId()))
            .hasSize(1);
    }

    @Test
    void registraHistorialDePreciosCerrandoLaVigenciaAnterior() throws Exception {
        Producto producto = crearProducto("PREC-" + UUID.randomUUID().toString().substring(0, 8));
        registrarPrecio(producto.getId(), "minorista", "100.00", "2026-01-01")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoPrecio").value("MINORISTA"))
            .andExpect(jsonPath("$.vigenteHasta").isEmpty());

        registrarPrecio(producto.getId(), "MINORISTA", "120.00", "2026-02-01")
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/productos/{id}/precios", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRECIOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].monto").value(120.0))
            .andExpect(jsonPath("$[0].vigenteHasta").isEmpty())
            .andExpect(jsonPath("$[1].monto").value(100.0))
            .andExpect(jsonPath("$[1].vigenteHasta").value("2026-01-31"));

        mockMvc.perform(get("/api/v1/productos/{id}/precios", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_CREAR)))
            .andExpect(status().isOk());

        assertThat(precioRepository
            .findAllByProductoIdOrderByTipoPrecioAscVigenteDesdeDesc(producto.getId()))
            .hasSize(2)
            .filteredOn(precio -> precio.getMonto().compareTo(new BigDecimal("100.00")) == 0)
            .singleElement()
            .satisfies(precio ->
                assertThat(precio.getVigenteHasta()).isEqualTo(LocalDate.of(2026, 1, 31))
            );
    }

    @Test
    void validaStockFechasYUnidades() throws Exception {
        String bodyProducto = """
            {
              "codigoInterno": "INVALIDO-%s",
              "nombre": "Producto inválido",
              "idCategoria": %d,
              "idUnidadBase": %d,
              "stockMinimo": -1
            }
            """.formatted(UUID.randomUUID(), categoria.getId(), unidad.getId());
        mockMvc.perform(post("/api/v1/productos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRODUCTOS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyProducto))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.stockMinimo").exists());

        Producto producto = crearProducto("VAL-" + UUID.randomUUID().toString().substring(0, 8));
        mockMvc.perform(post("/api/v1/productos/{id}/conversiones", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.CONVERSIONES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idUnidadOrigen": %d,
                      "idUnidadDestino": %d,
                      "factorConversion": 1
                    }
                    """.formatted(unidad.getId(), unidad.getId())))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/productos/{id}/precios", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRECIOS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoPrecio": "MINORISTA",
                      "monto": 10.00,
                      "vigenteDesde": "2026-03-10",
                      "vigenteHasta": "2026-03-01"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void migracionAsignaPermisosDeProductosAlAdministrador() {
        Set<String> esperados = Set.of(
            PermisosCatalogo.PRODUCTOS_VER,
            PermisosCatalogo.PRODUCTOS_CREAR,
            PermisosCatalogo.PRODUCTOS_EDITAR,
            PermisosCatalogo.PRODUCTOS_ESTADO,
            PermisosCatalogo.CONVERSIONES_VER,
            PermisosCatalogo.CONVERSIONES_CREAR,
            PermisosCatalogo.PRECIOS_VER,
            PermisosCatalogo.PRECIOS_CREAR
        );
        Set<String> registrados = permisoRepository.findAllByModuloOrderByCodigoAsc("Catálogo")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .filter(esperados::contains)
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

    private UnidadMedida crearUnidad(String codigo, String nombre, boolean permiteDecimales) {
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setCodigo(codigo.toUpperCase());
        unidadMedida.setNombre(nombre);
        unidadMedida.setPermiteDecimales(permiteDecimales);
        unidadMedida.setEstado(EstadoCatalogo.ACTIVO);
        return unidadMedidaRepository.save(unidadMedida);
    }

    private Producto crearProducto(String codigo) {
        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setMarca(marca);
        producto.setUnidadBase(unidad);
        producto.setCodigoInterno(codigo.toUpperCase());
        producto.setNombre("Producto de prueba");
        producto.setStockMinimo(new BigDecimal("1.000"));
        producto.setEstado(EstadoCatalogo.ACTIVO);
        return productoRepository.save(producto);
    }

    private String bodyActualizacion(String codigo, String nombre) {
        return """
            {
              "codigoInterno": "%s",
              "codigoBarras": null,
              "nombre": "%s",
              "descripcion": null,
              "idCategoria": %d,
              "idMarca": %d,
              "idUnidadBase": %d,
              "stockMinimo": 2.000
            }
            """.formatted(codigo, nombre, categoria.getId(), marca.getId(), unidad.getId());
    }

    private org.springframework.test.web.servlet.ResultActions registrarPrecio(
        Long idProducto,
        String tipo,
        String monto,
        String vigenteDesde
    ) throws Exception {
        String body = """
            {
              "tipoPrecio": "%s",
              "monto": %s,
              "vigenteDesde": "%s",
              "vigenteHasta": null
            }
            """.formatted(tipo, monto, vigenteDesde);
        return mockMvc.perform(post("/api/v1/productos/{id}/precios", idProducto)
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCatalogo.PRECIOS_CREAR))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private String bearer(String... authorities) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgc-proveperu")
            .subject("administrador-productos-test")
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(3600))
            .claim("userId", 999_997L)
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
