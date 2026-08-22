package pe.com.proveperu.sgc.cuentapagar.api.controller;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
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
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.compra.domain.model.CondicionPagoCompra;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.cuentapagar.application.service.PermisosCuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.domain.model.CuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.domain.model.EstadoCuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.infrastructure.persistence.CuentaPagarRepository;
import pe.com.proveperu.sgc.cuentapagar.infrastructure.persistence.PagoProveedorRepository;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.proveedor.application.service.PermisosProveedor;
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
class CuentasPagarIntegrationTests {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");

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
    private SedeRepository sedeRepository;

    @Autowired
    private CuentaPagarRepository cuentaRepository;

    @Autowired
    private PagoProveedorRepository pagoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    private Usuario usuario;
    private Proveedor proveedor;
    private Producto producto;
    private UnidadMedida unidad;
    private Sede sede;
    private MetodoPago efectivo;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría CxP " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        unidad = new UnidadMedida();
        unidad.setCodigo("CX" + sufijo);
        unidad.setNombre("Unidad CxP " + sufijo);
        unidad.setPermiteDecimales(true);
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        unidad = unidadMedidaRepository.save(unidad);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidad);
        producto.setCodigoInterno("CXP-" + sufijo);
        producto.setNombre("Producto CxP " + sufijo);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        proveedor = new Proveedor();
        proveedor.setRuc(nuevoRuc());
        proveedor.setRazonSocial("Proveedor CxP " + sufijo);
        proveedor.setEstado(EstadoCatalogo.ACTIVO);
        proveedor = proveedorRepository.save(proveedor);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario CxP " + sufijo);
        usuario.setUsuarioLogin("cxp-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
        efectivo = metodoPagoRepository.findAllByEstadoIgnoreCaseOrderByNombreAsc("ACTIVO")
            .stream()
            .filter(metodo -> "EFECTIVO".equals(metodo.getCodigo()))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void generaCuentaCreditoSoloAlCompletarRecepcionYNoParaContado() throws Exception {
        long idCompraCredito = crearCompra(CondicionPagoCompra.CREDITO);

        recibir(idCompraCredito, "4.000")
            .andExpect(status().isCreated());
        assertThat(cuentaRepository.findByCompraId(idCompraCredito)).isEmpty();

        recibir(idCompraCredito, "6.000")
            .andExpect(status().isCreated());
        CuentaPagar cuenta = cuentaRepository.findByCompraId(idCompraCredito)
            .orElseThrow();
        assertThat(cuenta.getTotal()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getImportePagado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPagar.PENDIENTE);

        mockMvc.perform(get("/api/v1/cuentas-pagar")
                .param("idProveedor", proveedor.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaPagar.CUENTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].id").value(cuenta.getId()))
            .andExpect(jsonPath("$.contenido[0].idCompra").value(idCompraCredito))
            .andExpect(jsonPath("$.contenido[0].saldoPendiente").value(1000.0));

        long idCompraContado = crearCompra(CondicionPagoCompra.CONTADO);
        recibir(idCompraContado, "10.000")
            .andExpect(status().isCreated());
        assertThat(cuentaRepository.findByCompraId(idCompraContado)).isEmpty();
    }

    @Test
    void registraPagosParcialesYTotalesConHistorialYSaldoDelProveedor()
        throws Exception {
        CuentaPagar cuenta = crearCuentaCredito();

        pagar(cuenta.getId(), "300.00", "OP-PRIMER-ABONO")
            .andExpect(status().isCreated())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                "/api/v1/cuentas-pagar/" + cuenta.getId()
            ))
            .andExpect(jsonPath("$.cuenta.importePagado").value(300.0))
            .andExpect(jsonPath("$.cuenta.saldoPendiente").value(700.0))
            .andExpect(jsonPath("$.cuenta.estado").value("PARCIAL"))
            .andExpect(jsonPath("$.pagos[0].referencia").value("OP-PRIMER-ABONO"))
            .andExpect(jsonPath("$.pagos[0].usuarioLogin").value(
                usuario.getUsuarioLogin()
            ));

        mockMvc.perform(get("/api/v1/proveedores/{id}/compras", proveedor.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosProveedor.HISTORIAL_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumen.saldoPendiente").value(700.0))
            .andExpect(jsonPath("$.compras[0].saldoPendiente").value(700.0));

        pagar(cuenta.getId(), "700.00", "OP-PAGO-FINAL")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cuenta.importePagado").value(1000.0))
            .andExpect(jsonPath("$.cuenta.saldoPendiente").value(0.0))
            .andExpect(jsonPath("$.cuenta.estado").value("PAGADO"))
            .andExpect(jsonPath("$.pagos.length()").value(2));

        pagar(cuenta.getId(), "1.00", null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "La cuenta PAGADO no admite nuevos pagos"
            ));
        assertThat(pagoRepository.findAllByCuentaPagarIdOrderByFechaHoraDescIdDesc(
            cuenta.getId()
        )).hasSize(2);
    }

    @Test
    void rechazaSobrepagoSinAlterarCuenta() throws Exception {
        CuentaPagar cuenta = crearCuentaCredito();

        pagar(cuenta.getId(), "1000.01", "SOBREPAGO")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(
                "El monto supera el saldo pendiente: 1000.00"
            ));

        CuentaPagar sinCambios = cuentaRepository.findById(cuenta.getId()).orElseThrow();
        assertThat(sinCambios.getImportePagado()).isEqualByComparingTo("0.00");
        assertThat(sinCambios.getSaldoPendiente()).isEqualByComparingTo("1000.00");
        assertThat(pagoRepository.findAllByCuentaPagarIdOrderByFechaHoraDescIdDesc(
            cuenta.getId()
        )).isEmpty();
    }

    @Test
    void configuraVencimientoEIdentificaLasObligacionesVencidas() throws Exception {
        CuentaPagar cuenta = crearCuentaCredito();
        String ayer = LocalDate.now(ZONA_NEGOCIO).minusDays(1).toString();

        mockMvc.perform(patch("/api/v1/cuentas-pagar/{id}/vencimiento", cuenta.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaPagar.CUENTAS_EDITAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaVencimiento\":\"" + ayer + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fechaVencimiento").value(ayer))
            .andExpect(jsonPath("$.estado").value("VENCIDO"));

        mockMvc.perform(get("/api/v1/cuentas-pagar/vencidas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaPagar.CUENTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].id").value(cuenta.getId()))
            .andExpect(jsonPath("$.contenido[0].estado").value("VENCIDO"));
    }

    @Test
    void exponeMetodosDePagoYProtegeLosEndpointsConPermisos() throws Exception {
        mockMvc.perform(get("/api/v1/cuentas-pagar/metodos-pago")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaPagar.CUENTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.codigo == 'EFECTIVO')]").exists())
            .andExpect(jsonPath("$[?(@.codigo == 'TRANSFERENCIA')]").exists());

        mockMvc.perform(get("/api/v1/cuentas-pagar")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCompra.COMPRAS_VER
                )))
            .andExpect(status().isForbidden());

        Set<String> esperados = Set.of(
            PermisosCuentaPagar.CUENTAS_VER,
            PermisosCuentaPagar.CUENTAS_EDITAR,
            PermisosCuentaPagar.PAGOS_CREAR
        );
        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Cuentas por pagar")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(Collectors.toSet());
        assertThat(registrados).containsExactlyInAnyOrderElementsOf(esperados);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        Set<String> asignados = rolRepository.findByIdWithPermisos(administrador.getId())
            .orElseThrow()
            .getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .filter(esperados::contains)
            .collect(Collectors.toSet());
        assertThat(asignados).containsExactlyInAnyOrderElementsOf(esperados);
    }

    private CuentaPagar crearCuentaCredito() throws Exception {
        long idCompra = crearCompra(CondicionPagoCompra.CREDITO);
        recibir(idCompra, "10.000")
            .andExpect(status().isCreated());
        return cuentaRepository.findByCompraId(idCompra).orElseThrow();
    }

    private long crearCompra(CondicionPagoCompra condicionPago) throws Exception {
        String comprobante = "CXP-" + UUID.randomUUID().toString().substring(0, 10);
        MvcResult resultado = mockMvc.perform(post("/api/v1/compras")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCompra.COMPRAS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idProveedor": %d,
                      "fecha": "2026-08-22",
                      "tipoComprobante": "FACTURA",
                      "numeroComprobante": "%s",
                      "condicionPago": "%s",
                      "igv": 0.00,
                      "detalles": [
                        {
                          "idProducto": %d,
                          "idUnidadMedida": %d,
                          "cantidad": 10.000,
                          "precioCompra": 100.00
                        }
                      ]
                    }
                    """.formatted(
                        proveedor.getId(),
                        comprobante,
                        condicionPago,
                        producto.getId(),
                        unidad.getId()
                    )))
            .andExpect(status().isCreated())
            .andReturn();
        return ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.id"
        )).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions recibir(
        long idCompra,
        String cantidad
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/compras/{id}/recepciones", idCompra)
            .header(HttpHeaders.AUTHORIZATION, bearer(
                PermisosCompra.RECEPCIONES_CREAR
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idSede": %d,
                  "items": [
                    {
                      "idProducto": %d,
                      "cantidadRecibida": %s,
                      "conforme": true
                    }
                  ]
                }
                """.formatted(sede.getId(), producto.getId(), cantidad)));
    }

    private org.springframework.test.web.servlet.ResultActions pagar(
        Long idCuenta,
        String monto,
        String referencia
    ) throws Exception {
        String referenciaJson = referencia == null ? "null" : "\"" + referencia + "\"";
        return mockMvc.perform(post("/api/v1/cuentas-pagar/{id}/pagos", idCuenta)
            .header(HttpHeaders.AUTHORIZATION, bearer(
                PermisosCuentaPagar.PAGOS_CREAR
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idMetodoPago": %d,
                  "monto": %s,
                  "referencia": %s
                }
                """.formatted(efectivo.getId(), monto, referenciaJson)));
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
