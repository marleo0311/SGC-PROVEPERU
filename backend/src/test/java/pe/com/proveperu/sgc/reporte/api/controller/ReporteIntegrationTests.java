package pe.com.proveperu.sgc.reporte.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.caja.domain.model.Caja;
import pe.com.proveperu.sgc.caja.domain.model.ConceptoMovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoSesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.MovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.SesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.TipoMovimientoCaja;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.CajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.MovimientoCajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.SesionCajaRepository;
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.reporte.application.service.PermisosReporte;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.VentaRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class ReporteIntegrationTests {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final LocalDate FECHA_REPORTE = LocalDate.of(2099, 6, 15);

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
    private InventarioRepository inventarioRepository;

    @Autowired
    private SedeRepository sedeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private SesionCajaRepository sesionCajaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    private Usuario usuario;
    private Sede sede;
    private Producto producto;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();

        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Analista reporte " + sufijo);
        usuario.setUsuarioLogin("reporte-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.saveAndFlush(usuario);

        Sede sedeBase = sedeRepository
            .findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
        sede = new Sede();
        sede.setIdEmpresa(sedeBase.getIdEmpresa());
        sede.setNombre("Sede reporte " + sufijo);
        sede.setDireccion("Dirección de prueba");
        sede.setEstado("ACTIVO");
        sede = sedeRepository.saveAndFlush(sede);

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría reporte " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.saveAndFlush(categoria);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setCodigo("UR" + sufijo);
        unidad.setNombre("Unidad reporte " + sufijo);
        unidad.setPermiteDecimales(true);
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        unidad = unidadMedidaRepository.saveAndFlush(unidad);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidad);
        producto.setCodigoInterno("REP-" + sufijo);
        producto.setNombre("Producto reporte " + sufijo);
        producto.setStockMinimo(new BigDecimal("5.000"));
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.saveAndFlush(producto);

        Inventario inventario = new Inventario();
        inventario.setSede(sede);
        inventario.setProducto(producto);
        inventario.setStockFisico(new BigDecimal("2.000"));
        inventario.setStockReservado(BigDecimal.ZERO.setScale(3));
        inventarioRepository.saveAndFlush(inventario);

        Venta venta = crearVenta(unidad);
        crearMovimientosCaja(sufijo, venta);
    }

    @Test
    void consolidaVentasPorDiaVendedorYProducto() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/ventas")
                .param("desde", FECHA_REPORTE.toString())
                .param("hasta", FECHA_REPORTE.toString())
                .param("idSede", sede.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosReporte.REPORTES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.periodo.desde")
                .value(FECHA_REPORTE.toString()))
            .andExpect(jsonPath("$.periodo.idSede").value(sede.getId()))
            .andExpect(jsonPath("$.resumen.cantidadVentas").value(1))
            .andExpect(jsonPath("$.resumen.subtotal").value(100.0))
            .andExpect(jsonPath("$.resumen.igv").value(18.0))
            .andExpect(jsonPath("$.resumen.totalVentas").value(118.0))
            .andExpect(jsonPath("$.resumen.ticketPromedio").value(118.0))
            .andExpect(jsonPath("$.ventasDiarias[0].fecha")
                .value(FECHA_REPORTE.toString()))
            .andExpect(jsonPath("$.ventasPorVendedor[0].idVendedor")
                .value(usuario.getId()))
            .andExpect(jsonPath("$.productosMasVendidos[0].idProducto")
                .value(producto.getId()))
            .andExpect(jsonPath(
                "$.productosMasVendidos[0].cantidadBaseVendida"
            ).value(4.0));
    }

    @Test
    void informaInventarioFinanzasCajaYDashboard() throws Exception {
        String autorizacion = bearer(PermisosReporte.REPORTES_VER);

        mockMvc.perform(get("/api/v1/reportes/inventario")
                .param("idSede", sede.getId().toString())
                .param("limite", "50")
                .header(HttpHeaders.AUTHORIZATION, autorizacion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idSede").value(sede.getId()))
            .andExpect(jsonPath("$.resumen.productosStockBajo").isNumber())
            .andExpect(jsonPath(
                "$.productosStockBajo[?(@.idProducto == %d)]"
                    .formatted(producto.getId())
            ).exists())
            .andExpect(jsonPath(
                "$.productosStockBajo[?(@.idProducto == %d)].estadoStock"
                    .formatted(producto.getId())
            ).value("BAJO"));

        mockMvc.perform(get("/api/v1/reportes/finanzas")
                .header(HttpHeaders.AUTHORIZATION, autorizacion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cuentasCobrar.saldoPendiente").isNumber())
            .andExpect(jsonPath("$.cuentasPagar.saldoPendiente").isNumber())
            .andExpect(jsonPath("$.balancePendiente").isNumber());

        mockMvc.perform(get("/api/v1/reportes/caja")
                .param("desde", FECHA_REPORTE.toString())
                .param("hasta", FECHA_REPORTE.toString())
                .param("idSede", sede.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, autorizacion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumen.cantidadMovimientos").value(2))
            .andExpect(jsonPath("$.resumen.totalIngresos").value(150.0))
            .andExpect(jsonPath("$.resumen.totalEgresos").value(20.0))
            .andExpect(jsonPath("$.resumen.neto").value(130.0))
            .andExpect(jsonPath("$.metodosPago[0].neto").value(130.0));

        mockMvc.perform(get("/api/v1/reportes/dashboard")
                .param("desde", FECHA_REPORTE.toString())
                .param("hasta", FECHA_REPORTE.toString())
                .param("idSede", sede.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, autorizacion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fechaGeneracion").exists())
            .andExpect(jsonPath("$.ventas.totalVentas").value(118.0))
            .andExpect(jsonPath("$.inventario.productosStockBajo").isNumber())
            .andExpect(jsonPath("$.caja.neto").value(130.0))
            .andExpect(jsonPath("$.cuentasCobrar.saldoPendiente").isNumber());
    }

    @Test
    void protegeValidaYRegistraElPermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/dashboard"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/reportes/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer("VEN_VENTAS_VER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/reportes/ventas")
                .param("desde", "2099-06-16")
                .param("hasta", "2099-06-15")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosReporte.REPORTES_VER
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(
                "La fecha inicial no puede ser posterior a la fecha final"
            ));

        mockMvc.perform(get("/api/v1/reportes/inventario")
                .param("limite", "0")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosReporte.REPORTES_VER
                )))
            .andExpect(status().isBadRequest());

        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Reportes")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(Collectors.toSet());
        assertThat(registrados).containsExactly(
            PermisosReporte.REPORTES_VER
        );

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        Set<String> asignados = rolRepository
            .findByIdWithPermisos(administrador.getId())
            .orElseThrow()
            .getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .collect(Collectors.toSet());
        assertThat(asignados).contains(PermisosReporte.REPORTES_VER);
    }

    private Venta crearVenta(UnidadMedida unidad) {
        Venta venta = new Venta();
        venta.setVendedor(usuario);
        venta.setSede(sede);
        venta.setFechaHora(FECHA_REPORTE.atTime(12, 0)
            .atZone(ZONA_NEGOCIO).toInstant());
        venta.setTipoVenta(TipoVenta.MINORISTA);
        venta.setCondicionPago(CondicionPagoVenta.CONTADO);
        venta.setTipoComprobante(TipoComprobanteVenta.NOTA_VENTA);
        venta.setSubtotal(new BigDecimal("100.00"));
        venta.setIgv(new BigDecimal("18.00"));
        venta.setDescuentoTotal(new BigDecimal("0.00"));
        venta.setTotal(new BigDecimal("118.00"));
        venta.setEstado(EstadoVenta.REGISTRADA);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidad);
        detalle.setCantidad(new BigDecimal("4.000"));
        detalle.setCantidadBase(new BigDecimal("4.000"));
        detalle.setPrecioUnitario(new BigDecimal("25.00"));
        detalle.setDescuento(new BigDecimal("0.00"));
        detalle.setSubtotal(new BigDecimal("100.00"));
        venta.agregarDetalle(detalle);
        return ventaRepository.saveAndFlush(venta);
    }

    private void crearMovimientosCaja(String sufijo, Venta venta) {
        Caja caja = new Caja();
        caja.setSede(sede);
        caja.setNombre("Caja reporte " + sufijo);
        caja.setEstado(EstadoCaja.ACTIVO);
        caja = cajaRepository.saveAndFlush(caja);

        SesionCaja sesion = new SesionCaja();
        sesion.setCaja(caja);
        sesion.setUsuarioApertura(usuario);
        sesion.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        sesion = sesionCajaRepository.saveAndFlush(sesion);

        MetodoPago efectivo = metodoPagoRepository
            .findByCodigoIgnoreCase("EFECTIVO")
            .orElseThrow();
        Instant fecha = FECHA_REPORTE.atTime(13, 0)
            .atZone(ZONA_NEGOCIO).toInstant();
        movimientoCajaRepository.save(crearMovimiento(
            sesion,
            efectivo,
            venta,
            TipoMovimientoCaja.INGRESO,
            ConceptoMovimientoCaja.INGRESO_MANUAL,
            "150.00",
            fecha
        ));
        movimientoCajaRepository.saveAndFlush(crearMovimiento(
            sesion,
            efectivo,
            venta,
            TipoMovimientoCaja.EGRESO,
            ConceptoMovimientoCaja.EGRESO_MANUAL,
            "20.00",
            fecha.plusSeconds(60)
        ));
    }

    private MovimientoCaja crearMovimiento(
        SesionCaja sesion,
        MetodoPago metodoPago,
        Venta venta,
        TipoMovimientoCaja tipo,
        ConceptoMovimientoCaja concepto,
        String importe,
        Instant fecha
    ) {
        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesion(sesion);
        movimiento.setMetodoPago(metodoPago);
        movimiento.setUsuario(usuario);
        movimiento.setVenta(venta);
        movimiento.setTipo(tipo);
        movimiento.setConcepto(concepto);
        movimiento.setImporte(new BigDecimal(importe));
        movimiento.setFechaHora(fecha);
        return movimiento;
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
