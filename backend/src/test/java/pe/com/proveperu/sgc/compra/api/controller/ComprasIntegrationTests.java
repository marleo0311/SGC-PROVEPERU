package pe.com.proveperu.sgc.compra.api.controller;

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
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.DetalleCompraRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
import pe.com.proveperu.sgc.proveedor.application.service.PermisosProveedor;
import pe.com.proveperu.sgc.proveedor.domain.model.Proveedor;
import pe.com.proveperu.sgc.proveedor.infrastructure.persistence.ProveedorRepository;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.transporte.application.service.PermisosTransporte;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class ComprasIntegrationTests {

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
    private ProveedorRepository proveedorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private DetalleCompraRepository detalleCompraRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    private Usuario usuario;
    private Proveedor proveedor;
    private Producto producto;
    private UnidadMedida unidad;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría compra " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        unidad = new UnidadMedida();
        unidad.setCodigo("UC" + sufijo);
        unidad.setNombre("Unidad compra " + sufijo);
        unidad.setPermiteDecimales(true);
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        unidad = unidadMedidaRepository.save(unidad);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidad);
        producto.setCodigoInterno("PC-" + sufijo);
        producto.setNombre("Producto compra " + sufijo);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        proveedor = new Proveedor();
        proveedor.setRuc(nuevoRuc());
        proveedor.setRazonSocial("Proveedor compra " + sufijo);
        proveedor.setEstado(EstadoCatalogo.ACTIVO);
        proveedor = proveedorRepository.save(proveedor);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario compra " + sufijo);
        usuario.setUsuarioLogin("compra-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);
    }

    @Test
    void registraCompraCalculaTotalesYNoMueveInventario() throws Exception {
        long movimientosAntes = movimientoInventarioRepository.count();
        long detallesAntes = detalleCompraRepository.count();

        MvcResult resultado = crearCompra("F001-100", "CREDITO", "4.50")
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.idProveedor").value(proveedor.getId()))
            .andExpect(jsonPath("$.usuarioLogin").value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.estado").value("REGISTRADA"))
            .andExpect(jsonPath("$.subtotal").value(25.0))
            .andExpect(jsonPath("$.igv").value(4.5))
            .andExpect(jsonPath("$.gastosAdicionales").value(0.0))
            .andExpect(jsonPath("$.total").value(29.5))
            .andExpect(jsonPath("$.detalles[0].cantidad").value(2.5))
            .andExpect(jsonPath("$.detalles[0].precioCompra").value(10.0))
            .andReturn();
        Number idCompra = idDe(resultado);

        assertThat(detalleCompraRepository.count()).isEqualTo(detallesAntes + 1);
        assertThat(movimientoInventarioRepository.count()).isEqualTo(movimientosAntes);

        mockMvc.perform(get("/api/v1/compras")
                .param("idProveedor", proveedor.getId().toString())
                .param("estado", "REGISTRADA")
                .param("desde", "2026-08-01")
                .param("hasta", "2026-08-31")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(idCompra.longValue()));

        mockMvc.perform(get("/api/v1/compras/{id}", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.detalles[0].codigoProducto")
                .value(producto.getCodigoInterno()));
    }

    @Test
    void agregaGastoYRecalculaTotalDeCompra() throws Exception {
        Number idCompra = idDe(crearCompra("F001-101", "CONTADO", "4.50")
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(post("/api/v1/compras/{id}/gastos", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.GASTOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoGasto": "CARGA",
                      "descripcion": "Carga de mercadería",
                      "importe": 5.25,
                      "fecha": "2026-08-22",
                      "numeroComprobante": "G001-10"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.idCompra").value(idCompra.longValue()))
            .andExpect(jsonPath("$.importe").value(5.25));

        mockMvc.perform(get("/api/v1/compras/{id}", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gastosAdicionales").value(5.25))
            .andExpect(jsonPath("$.total").value(34.75));

        mockMvc.perform(get("/api/v1/compras/{id}/gastos", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipoGasto").value("CARGA"));
    }

    @Test
    void editaCompraRegistradaYLuegoImpideCambiosTrasAnularla() throws Exception {
        Number idCompra = idDe(crearCompra("F001-102", "CONTADO", "4.50")
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(put("/api/v1/compras/{id}", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyCompra("F001-102", "CONTADO", "0.00", "3.000", "12.00")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotal").value(36.0))
            .andExpect(jsonPath("$.total").value(36.0))
            .andExpect(jsonPath("$.detalles.length()").value(1));

        mockMvc.perform(patch("/api/v1/compras/{id}/estado", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_ANULAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ANULADA\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ANULADA"));

        mockMvc.perform(put("/api/v1/compras/{id}", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_EDITAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyCompra("F001-102", "CONTADO", "0.00", "1.000", "10.00")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Solo se puede modificar o anular una compra REGISTRADA"
            ));
    }

    @Test
    void rechazaComprobanteDuplicadoYDetallesRepetidos() throws Exception {
        crearCompra("F001-103", "CONTADO", "0.00")
            .andExpect(status().isCreated());
        crearCompra("F001-103", "CONTADO", "0.00")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Ya existe una compra de ese proveedor con el mismo comprobante"
            ));

        mockMvc.perform(post("/api/v1/compras")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConDetallesRepetidos()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(
                "No se puede repetir el mismo producto y unidad en la compra"
            ));
    }

    @Test
    void exigeUnidadValidaParaElProductoYAutoridadCorrecta() throws Exception {
        UnidadMedida otraUnidad = new UnidadMedida();
        otraUnidad.setCodigo("OT" + UUID.randomUUID().toString().substring(0, 8));
        otraUnidad.setNombre("Otra unidad");
        otraUnidad.setPermiteDecimales(true);
        otraUnidad.setEstado(EstadoCatalogo.ACTIVO);
        otraUnidad = unidadMedidaRepository.save(otraUnidad);

        mockMvc.perform(post("/api/v1/compras")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyCompraConUnidad(otraUnidad.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "no es la unidad base ni tiene una conversión activa"
            )));

        mockMvc.perform(get("/api/v1/compras")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCatalogo.PRODUCTOS_VER
                )))
            .andExpect(status().isForbidden());
    }

    @Test
    void historialDelProveedorIncluyeLasComprasRegistradas() throws Exception {
        Number idCompra = idDe(crearCompra("F001-104", "CREDITO", "4.50")
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(get("/api/v1/proveedores/{id}/compras", proveedor.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosProveedor.HISTORIAL_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumen.totalCompras").value(1))
            .andExpect(jsonPath("$.resumen.importeTotal").value(29.5))
            .andExpect(jsonPath("$.resumen.saldoPendiente").value(29.5))
            .andExpect(jsonPath("$.compras[0].idCompra").value(idCompra.longValue()));
    }

    @Test
    void migracionRegistraYAsignaPermisosDeComprasAlAdministrador() {
        Set<String> esperados = Set.of(
            PermisosCompra.COMPRAS_VER,
            PermisosCompra.COMPRAS_CREAR,
            PermisosCompra.COMPRAS_EDITAR,
            PermisosCompra.COMPRAS_ANULAR
        );
        Set<String> registrados = permisoRepository.findAllByModuloOrderByCodigoAsc("Compras")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .filter(esperados::contains)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(registrados).containsExactlyInAnyOrderElementsOf(esperados);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        Set<String> asignados = rolRepository.findByIdWithPermisos(administrador.getId())
            .orElseThrow()
            .getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .filter(esperados::contains)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(asignados).containsExactlyInAnyOrderElementsOf(esperados);
    }

    private org.springframework.test.web.servlet.ResultActions crearCompra(
        String comprobante,
        String condicion,
        String igv
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/compras")
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_CREAR))
            .contentType(MediaType.APPLICATION_JSON)
            .content(bodyCompra(comprobante, condicion, igv, "2.500", "10.00")));
    }

    private String bodyCompra(
        String comprobante,
        String condicion,
        String igv,
        String cantidad,
        String precio
    ) {
        return """
            {
              "idProveedor": %d,
              "fecha": "2026-08-22",
              "tipoComprobante": "FACTURA",
              "numeroComprobante": "%s",
              "condicionPago": "%s",
              "igv": %s,
              "detalles": [
                {
                  "idProducto": %d,
                  "idUnidadMedida": %d,
                  "cantidad": %s,
                  "precioCompra": %s
                }
              ]
            }
            """.formatted(
                proveedor.getId(),
                comprobante,
                condicion,
                igv,
                producto.getId(),
                unidad.getId(),
                cantidad,
                precio
            );
    }

    private String bodyConDetallesRepetidos() {
        return """
            {
              "idProveedor": %d,
              "fecha": "2026-08-22",
              "condicionPago": "CONTADO",
              "igv": 0.00,
              "detalles": [
                {"idProducto": %d, "idUnidadMedida": %d, "cantidad": 1, "precioCompra": 10},
                {"idProducto": %d, "idUnidadMedida": %d, "cantidad": 2, "precioCompra": 10}
              ]
            }
            """.formatted(
                proveedor.getId(),
                producto.getId(),
                unidad.getId(),
                producto.getId(),
                unidad.getId()
            );
    }

    private String bodyCompraConUnidad(Long idUnidad) {
        return """
            {
              "idProveedor": %d,
              "fecha": "2026-08-22",
              "condicionPago": "CONTADO",
              "igv": 0.00,
              "detalles": [
                {"idProducto": %d, "idUnidadMedida": %d, "cantidad": 1, "precioCompra": 10}
              ]
            }
            """.formatted(proveedor.getId(), producto.getId(), idUnidad);
    }

    private Number idDe(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }

    private String nuevoRuc() {
        return Long.toString(ThreadLocalRandom.current().nextLong(
            10_000_000_000L,
            100_000_000_000L
        ));
    }

    private String bearer(String... authorities) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgc-proveperu")
            .subject(usuario.getUsuarioLogin())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(3600))
            .claim("userId", usuario.getId())
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
