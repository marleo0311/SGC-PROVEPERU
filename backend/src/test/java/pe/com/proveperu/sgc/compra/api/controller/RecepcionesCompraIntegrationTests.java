package pe.com.proveperu.sgc.compra.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.DetalleRecepcionCompraRepository;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.RecepcionCompraRepository;
import pe.com.proveperu.sgc.inventario.application.service.PermisosInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.proveedor.domain.model.Proveedor;
import pe.com.proveperu.sgc.proveedor.infrastructure.persistence.ProveedorRepository;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class RecepcionesCompraIntegrationTests {

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
    private ProductoUnidadConversionRepository conversionRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private SedeRepository sedeRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private RecepcionCompraRepository recepcionRepository;

    @Autowired
    private DetalleRecepcionCompraRepository detalleRecepcionRepository;

    private Usuario usuario;
    private Proveedor proveedor;
    private Producto producto;
    private UnidadMedida unidadBase;
    private Sede sede;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría recepción " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        unidadBase = nuevaUnidad("UR" + sufijo, "Unidad recepción " + sufijo, true);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidadBase);
        producto.setCodigoInterno("PR-" + sufijo);
        producto.setNombre("Producto recepción " + sufijo);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        proveedor = new Proveedor();
        proveedor.setRuc(nuevoRuc());
        proveedor.setRazonSocial("Proveedor recepción " + sufijo);
        proveedor.setEstado(EstadoCatalogo.ACTIVO);
        proveedor = proveedorRepository.save(proveedor);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario recepción " + sufijo);
        usuario.setUsuarioLogin("recepcion-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
    }

    @Test
    void confirmaRecepcionTotalActualizaStockCompraYKardex() throws Exception {
        Number idCompra = crearCompra(producto, unidadBase, "5.000");

        MvcResult resultado = recibir(idCompra, producto.getId(), "5.000", true, null)
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
            .andExpect(jsonPath("$.idSede").value(sede.getId()))
            .andExpect(jsonPath("$.usuarioLogin").value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.items[0].cantidadEsperada").value(5.0))
            .andExpect(jsonPath("$.items[0].cantidadRecibida").value(5.0))
            .andExpect(jsonPath("$.items[0].cantidadAcumulada").value(5.0))
            .andExpect(jsonPath("$.items[0].cantidadPendiente").value(0.0))
            .andReturn();
        Number idRecepcion = idDe(resultado);

        Inventario inventario = inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElseThrow();
        assertThat(inventario.getStockFisico()).isEqualByComparingTo("5.000");
        MovimientoInventario movimiento = movimientoDeProducto(producto.getId());
        assertThat(movimiento.getTipoMovimiento()).isEqualTo(TipoMovimientoInventario.COMPRA);
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("5.000");
        assertThat(movimiento.getCantidadBase()).isEqualByComparingTo("5.000");
        assertThat(movimiento.getDocumentoOrigen()).isEqualTo("RECEPCION_COMPRA");
        assertThat(movimiento.getIdOrigen()).isEqualTo(idRecepcion.longValue());

        mockMvc.perform(get("/api/v1/compras/{id}", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECIBIDA"))
            .andExpect(jsonPath("$.detalles[0].cantidadRecibida").value(5.0))
            .andExpect(jsonPath("$.detalles[0].cantidadPendiente").value(0.0));

        mockMvc.perform(get("/api/v1/kardex/{idProducto}", producto.getId())
                .param("idSede", sede.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.KARDEX_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].tipoMovimiento").value("COMPRA"))
            .andExpect(jsonPath("$.contenido[0].idOrigen").value(idRecepcion.longValue()));
    }

    @Test
    void permiteRecepcionesParcialesEImpideSuperarElPendiente() throws Exception {
        Number idCompra = crearCompra(producto, unidadBase, "10.000");

        recibir(idCompra, producto.getId(), "4.000", true, null)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].cantidadPendiente").value(6.0));

        mockMvc.perform(get("/api/v1/compras/{id}", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PARCIALMENTE_RECIBIDA"));

        long recepcionesAntes = recepcionRepository.count();
        long movimientosAntes = movimientoRepository.count();
        recibir(idCompra, producto.getId(), "7.000", true, null)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "supera la cantidad pendiente: 6.000"
            )));
        assertThat(recepcionRepository.count()).isEqualTo(recepcionesAntes);
        assertThat(movimientoRepository.count()).isEqualTo(movimientosAntes);

        recibir(idCompra, producto.getId(), "6.000", true, null)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].cantidadAcumulada").value(10.0))
            .andExpect(jsonPath("$.items[0].cantidadPendiente").value(0.0));

        Inventario inventario = inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElseThrow();
        assertThat(inventario.getStockFisico()).isEqualByComparingTo("10.000");

        mockMvc.perform(get("/api/v1/compras/{id}/recepciones", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.RECEPCIONES_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void registraIncidenciaYExigeObservacionParaNoConforme() throws Exception {
        Number idCompra = crearCompra(producto, unidadBase, "8.000");

        recibir(idCompra, producto.getId(), "3.000", false, null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(
                "Una cantidad no conforme requiere una observación"
            ));
        assertThat(recepcionRepository.count()).isZero();

        recibir(idCompra, producto.getId(), "3.000", false, "Dos unidades dañadas")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("CON_INCIDENCIA"))
            .andExpect(jsonPath("$.items[0].conforme").value(false))
            .andExpect(jsonPath("$.items[0].observacion").value(
                "Dos unidades dañadas"
            ));
    }

    @Test
    void convierteLaUnidadCompradaAntesDeIngresarAlStockBase() throws Exception {
        UnidadMedida caja = nuevaUnidad(
            "CJ" + UUID.randomUUID().toString().substring(0, 8),
            "Caja de prueba",
            false
        );
        ProductoUnidadConversion conversion = new ProductoUnidadConversion();
        conversion.setProducto(producto);
        conversion.setUnidadOrigen(caja);
        conversion.setUnidadDestino(unidadBase);
        conversion.setFactorConversion(new BigDecimal("10.000000"));
        conversion.setEstado(EstadoCatalogo.ACTIVO);
        conversionRepository.save(conversion);

        Number idCompra = crearCompra(producto, caja, "2.000");
        recibir(idCompra, producto.getId(), "2.000", true, null)
            .andExpect(status().isCreated());

        Inventario inventario = inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElseThrow();
        assertThat(inventario.getStockFisico()).isEqualByComparingTo("20.000");
        MovimientoInventario movimiento = movimientoDeProducto(producto.getId());
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("2.000");
        assertThat(movimiento.getCantidadBase()).isEqualByComparingTo("20.000");
        assertThat(movimiento.getUnidadMedida().getId()).isEqualTo(caja.getId());
    }

    @Test
    void revierteLaRecepcionCompletaSiFallaLaEntradaDeInventario() throws Exception {
        UnidadMedida paquete = nuevaUnidad(
            "PQ" + UUID.randomUUID().toString().substring(0, 8),
            "Paquete de prueba",
            false
        );
        ProductoUnidadConversion conversion = new ProductoUnidadConversion();
        conversion.setProducto(producto);
        conversion.setUnidadOrigen(paquete);
        conversion.setUnidadDestino(unidadBase);
        conversion.setFactorConversion(new BigDecimal("5.000000"));
        conversion.setEstado(EstadoCatalogo.ACTIVO);
        conversion = conversionRepository.save(conversion);

        Number idCompra = crearCompra(producto, paquete, "2.000");
        conversion.setEstado(EstadoCatalogo.INACTIVO);
        conversionRepository.saveAndFlush(conversion);
        long recepcionesAntes = recepcionRepository.count();
        long detallesAntes = detalleRecepcionRepository.count();
        long movimientosAntes = movimientoRepository.count();

        recibir(idCompra, producto.getId(), "2.000", true, null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "No existe una conversión activa entre la unidad indicada y la unidad base"
            ));

        TestTransaction.flagForRollback();
        TestTransaction.end();
        assertThat(recepcionRepository.count()).isEqualTo(recepcionesAntes);
        assertThat(detalleRecepcionRepository.count()).isEqualTo(detallesAntes);
        assertThat(movimientoRepository.count()).isEqualTo(movimientosAntes);
        assertThat(inventarioRepository.findBySedeIdAndProductoId(
            sede.getId(),
            producto.getId()
        )).isEmpty();
    }

    @Test
    void impideRecibirCompraAnuladaYProtegeElEndpointPorPermiso() throws Exception {
        Number idCompra = crearCompra(producto, unidadBase, "5.000");
        mockMvc.perform(patch("/api/v1/compras/{id}/estado", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_ANULAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ANULADA\"}"))
            .andExpect(status().isOk());

        recibir(idCompra, producto.getId(), "5.000", true, null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "La compra ANULADA no admite nuevas recepciones"
            ));

        mockMvc.perform(get("/api/v1/compras/{id}/recepciones", idCompra)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_VER)))
            .andExpect(status().isForbidden());
    }

    @Test
    void migracionRegistraYAsignaPermisosDeRecepcionAlAdministrador() {
        Set<String> esperados = Set.of(
            PermisosCompra.RECEPCIONES_VER,
            PermisosCompra.RECEPCIONES_CREAR
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

    @Test
    void detalleDeRecepcionConservaCantidadesYTrazabilidad() throws Exception {
        Number idCompra = crearCompra(producto, unidadBase, "4.000");
        recibir(idCompra, producto.getId(), "1.500", true, null)
            .andExpect(status().isCreated());

        assertThat(detalleRecepcionRepository.findAll()).anySatisfy(detalle -> {
            assertThat(detalle.getRecepcion().getCompra().getId())
                .isEqualTo(idCompra.longValue());
            assertThat(detalle.getProducto().getId()).isEqualTo(producto.getId());
            assertThat(detalle.getCantidadEsperada()).isEqualByComparingTo("4.000");
            assertThat(detalle.getCantidadRecibida()).isEqualByComparingTo("1.500");
            assertThat(detalle.getCantidadPendiente()).isEqualByComparingTo("2.500");
        });
    }

    private Number crearCompra(
        Producto productoCompra,
        UnidadMedida unidadCompra,
        String cantidad
    ) throws Exception {
        String comprobante = "R-" + UUID.randomUUID().toString().substring(0, 10);
        MvcResult resultado = mockMvc.perform(post("/api/v1/compras")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idProveedor": %d,
                      "fecha": "2026-08-22",
                      "tipoComprobante": "FACTURA",
                      "numeroComprobante": "%s",
                      "condicionPago": "CONTADO",
                      "igv": 0.00,
                      "detalles": [
                        {
                          "idProducto": %d,
                          "idUnidadMedida": %d,
                          "cantidad": %s,
                          "precioCompra": 10.00
                        }
                      ]
                    }
                    """.formatted(
                        proveedor.getId(),
                        comprobante,
                        productoCompra.getId(),
                        unidadCompra.getId(),
                        cantidad
                    )))
            .andExpect(status().isCreated())
            .andReturn();
        return idDe(resultado);
    }

    private org.springframework.test.web.servlet.ResultActions recibir(
        Number idCompra,
        Long idProducto,
        String cantidad,
        boolean conforme,
        String observacionItem
    ) throws Exception {
        String observacionJson = observacionItem == null
            ? "null"
            : "\"" + observacionItem + "\"";
        return mockMvc.perform(post("/api/v1/compras/{id}/recepciones", idCompra)
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.RECEPCIONES_CREAR))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idSede": %d,
                  "items": [
                    {
                      "idProducto": %d,
                      "cantidadRecibida": %s,
                      "conforme": %s,
                      "observacion": %s
                    }
                  ],
                  "observacion": null
                }
                """.formatted(
                    sede.getId(),
                    idProducto,
                    cantidad,
                    conforme,
                    observacionJson
                )));
    }

    private UnidadMedida nuevaUnidad(
        String codigo,
        String nombre,
        boolean permiteDecimales
    ) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setCodigo(codigo);
        unidad.setNombre(nombre);
        unidad.setPermiteDecimales(permiteDecimales);
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        return unidadMedidaRepository.save(unidad);
    }

    private MovimientoInventario movimientoDeProducto(Long idProducto) {
        return movimientoRepository.findAll().stream()
            .filter(movimiento -> movimiento.getProducto().getId().equals(idProducto))
            .findFirst()
            .orElseThrow();
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
