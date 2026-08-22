package pe.com.proveperu.sgc.devolucion.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import pe.com.proveperu.sgc.caja.application.service.PermisosCaja;
import pe.com.proveperu.sgc.caja.domain.model.Caja;
import pe.com.proveperu.sgc.caja.domain.model.ConceptoMovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoSesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.SesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.TipoMovimientoCaja;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.CajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.MovimientoCajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.SesionCajaRepository;
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.devolucion.application.service.PermisosDevolucion;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;
import pe.com.proveperu.sgc.venta.domain.model.PagoCliente;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.CuentaCobrarRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.PagoClienteRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.VentaRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class DevolucionesIntegrationTests {

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
    private PrecioProductoRepository precioProductoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

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
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private CuentaCobrarRepository cuentaRepository;

    @Autowired
    private PagoClienteRepository pagoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private SesionCajaRepository sesionRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    private Usuario usuario;
    private Venta venta;
    private DetalleVenta detalleVenta;
    private Inventario inventario;
    private Producto productoReemplazo;
    private Inventario inventarioReemplazo;
    private PrecioProducto precioReemplazo;
    private MetodoPago efectivo;
    private SesionCaja sesion;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría devolución " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setCodigo("DV" + sufijo);
        unidad.setNombre("Unidad devolución " + sufijo);
        unidad.setPermiteDecimales(true);
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        unidad = unidadMedidaRepository.save(unidad);

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidad);
        producto.setCodigoInterno("DEV-" + sufijo);
        producto.setNombre("Producto devolución " + sufijo);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        productoReemplazo = new Producto();
        productoReemplazo.setCategoria(categoria);
        productoReemplazo.setUnidadBase(unidad);
        productoReemplazo.setCodigoInterno("CAM-" + sufijo);
        productoReemplazo.setNombre("Producto de cambio " + sufijo);
        productoReemplazo.setStockMinimo(BigDecimal.ZERO);
        productoReemplazo.setEstado(EstadoCatalogo.ACTIVO);
        productoReemplazo = productoRepository.save(productoReemplazo);

        precioReemplazo = new PrecioProducto();
        precioReemplazo.setProducto(productoReemplazo);
        precioReemplazo.setTipoPrecio("MINORISTA");
        precioReemplazo.setMonto(new BigDecimal("25.00"));
        precioReemplazo.setVigenteDesde(LocalDate.now().minusDays(1));
        precioReemplazo.setEstado(EstadoCatalogo.ACTIVO);
        precioReemplazo = precioProductoRepository.save(precioReemplazo);

        Cliente cliente = new Cliente();
        cliente.setTipoPersona(TipoPersona.NATURAL);
        cliente.setTipoDocumento(TipoDocumentoCliente.DNI);
        cliente.setNumeroDocumento(nuevoDni());
        cliente.setNombres("Cliente");
        cliente.setApellidos("Devolución " + sufijo);
        cliente.setEstado(EstadoCatalogo.ACTIVO);
        cliente = clienteRepository.save(cliente);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario devolución " + sufijo);
        usuario.setUsuarioLogin("devolucion-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        Sede sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
        venta = new Venta();
        venta.setCliente(cliente);
        venta.setVendedor(usuario);
        venta.setSede(sede);
        venta.setTipoVenta(TipoVenta.MINORISTA);
        venta.setCondicionPago(CondicionPagoVenta.CONTADO);
        venta.setTipoComprobante(TipoComprobanteVenta.NOTA_VENTA);
        venta.setSubtotal(new BigDecimal("50.00"));
        venta.setIgv(BigDecimal.ZERO.setScale(2));
        venta.setDescuentoTotal(BigDecimal.ZERO.setScale(2));
        venta.setTotal(new BigDecimal("50.00"));
        venta.setEstado(EstadoVenta.REGISTRADA);

        detalleVenta = new DetalleVenta();
        detalleVenta.setProducto(producto);
        detalleVenta.setUnidadMedida(unidad);
        detalleVenta.setCantidad(new BigDecimal("2.000"));
        detalleVenta.setCantidadBase(new BigDecimal("2.000"));
        detalleVenta.setPrecioUnitario(new BigDecimal("25.00"));
        detalleVenta.setDescuento(BigDecimal.ZERO.setScale(2));
        detalleVenta.setSubtotal(new BigDecimal("50.00"));
        venta.agregarDetalle(detalleVenta);
        venta = ventaRepository.saveAndFlush(venta);

        inventario = new Inventario();
        inventario.setSede(sede);
        inventario.setProducto(producto);
        inventario.setStockFisico(new BigDecimal("18.000"));
        inventario.setStockReservado(BigDecimal.ZERO.setScale(3));
        inventario = inventarioRepository.saveAndFlush(inventario);

        inventarioReemplazo = new Inventario();
        inventarioReemplazo.setSede(sede);
        inventarioReemplazo.setProducto(productoReemplazo);
        inventarioReemplazo.setStockFisico(new BigDecimal("8.000"));
        inventarioReemplazo.setStockReservado(BigDecimal.ZERO.setScale(3));
        inventarioReemplazo = inventarioRepository.saveAndFlush(
            inventarioReemplazo
        );

        efectivo = metodoPagoRepository.findByCodigoIgnoreCase("EFECTIVO")
            .orElseThrow();
        PagoCliente pago = new PagoCliente();
        pago.setVenta(venta);
        pago.setMetodoPago(efectivo);
        pago.setUsuario(usuario);
        pago.setMonto(new BigDecimal("50.00"));
        pago.setReferencia("PAGO-VENTA-ORIGINAL");
        pagoRepository.saveAndFlush(pago);

        Caja caja = new Caja();
        caja.setSede(sede);
        caja.setNombre("Caja devolución " + sufijo);
        caja.setEstado(EstadoCaja.ACTIVO);
        caja = cajaRepository.saveAndFlush(caja);
        sesion = new SesionCaja();
        sesion.setCaja(caja);
        sesion.setUsuarioApertura(usuario);
        sesion.setSaldoInicial(new BigDecimal("100.00"));
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        sesion = sesionRepository.saveAndFlush(sesion);
    }

    @Test
    void devuelveProductoAptoYRegistraReembolsoEnCaja() throws Exception {
        long idDevolucion = registrarDevolucion("1.000", "APTO")
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.devolucion.estado")
                .value("PENDIENTE_REEMBOLSO"))
            .andExpect(jsonPath("$.devolucion.importeTotal").value(25.0))
            .andExpect(jsonPath("$.devolucion.importeAplicadoSaldo").value(0.0))
            .andExpect(jsonPath("$.devolucion.importeReembolsable").value(25.0))
            .andExpect(jsonPath("$.items[0].reincorporadoInventario").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .transform(contenido -> ((Number) JsonPath.read(
                contenido,
                "$.devolucion.id"
            )).longValue());

        Inventario actualizado = inventarioRepository
            .findBySedeIdAndProductoId(
                inventario.getSede().getId(),
                inventario.getProducto().getId()
            ).orElseThrow();
        assertThat(actualizado.getStockFisico()).isEqualByComparingTo("19.000");
        assertThat(ventaRepository.findById(venta.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoVenta.DEVUELTA_PARCIAL);
        assertThat(movimientosInventario(idDevolucion)).singleElement()
            .satisfies(movimiento -> {
                assertThat(movimiento.getTipoMovimiento())
                    .isEqualTo(TipoMovimientoInventario.DEVOLUCION_ENTRADA);
                assertThat(movimiento.getCantidadBase())
                    .isEqualByComparingTo("1.000");
            });

        mockMvc.perform(get("/api/v1/devoluciones")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.DEVOLUCIONES_VER
                ))
                .param("idVenta", venta.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1));

        mockMvc.perform(post(
                "/api/v1/devoluciones/{id}/reembolso",
                idDevolucion
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.REEMBOLSOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idMetodoPago": %d,
                      "importe": 25.00,
                      "referencia": "REEMBOLSO-EFECTIVO"
                    }
                    """.formatted(efectivo.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.estado").value("REEMBOLSADA"))
            .andExpect(jsonPath("$.devolucion.importeReembolsado").value(25.0))
            .andExpect(jsonPath("$.reembolso.metodoPagoCodigo").value("EFECTIVO"))
            .andExpect(jsonPath("$.reembolso.usuarioLogin")
                .value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.items[0].importeReembolso").value(25.0));

        var movimientosCaja = movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesion.getId());
        assertThat(movimientosCaja).singleElement().satisfies(movimiento -> {
            assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoCaja.EGRESO);
            assertThat(movimiento.getConcepto())
                .isEqualTo(ConceptoMovimientoCaja.REEMBOLSO);
            assertThat(movimiento.getImporte()).isEqualByComparingTo("25.00");
        });

        mockMvc.perform(get(
                "/api/v1/sesiones-caja/{id}/resumen",
                sesion.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCaja.RESUMEN_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.saldoEsperado").value(75.0));
    }

    @Test
    void productoDefectuosoNoRegresaAlStockYCompletaVentaDevuelta() throws Exception {
        long movimientosAntes = movimientoInventarioRepository.count();
        registrarDevolucion("2.000", "DEFECTUOSO")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].reincorporadoInventario").value(false));

        Inventario actualizado = inventarioRepository
            .findBySedeIdAndProductoId(
                inventario.getSede().getId(),
                inventario.getProducto().getId()
            ).orElseThrow();
        assertThat(actualizado.getStockFisico()).isEqualByComparingTo("18.000");
        assertThat(movimientoInventarioRepository.count()).isEqualTo(movimientosAntes);
        assertThat(ventaRepository.findById(venta.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoVenta.DEVUELTA_TOTAL);

        registrarDevolucion("0.001", "APTO")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "La venta ya fue devuelta completamente"
            ));
    }

    @Test
    void aplicaPrimeroLaDevolucionAlSaldoYReembolsaSoloLoPagado() throws Exception {
        venta.setCondicionPago(CondicionPagoVenta.PARCIAL);
        ventaRepository.saveAndFlush(venta);
        CuentaCobrar cuenta = new CuentaCobrar();
        cuenta.setVenta(venta);
        cuenta.setTotal(new BigDecimal("50.00"));
        cuenta.setImportePagado(new BigDecimal("10.00"));
        cuenta.setSaldoPendiente(new BigDecimal("40.00"));
        cuenta.setFechaVencimiento(LocalDate.now().plusDays(15));
        cuenta.setEstado(EstadoCuentaCobrar.PARCIAL);
        cuenta = cuentaRepository.saveAndFlush(cuenta);

        MvcResult resultado = registrarDevolucion("2.000", "APTO")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.importeTotal").value(50.0))
            .andExpect(jsonPath("$.devolucion.importeAplicadoSaldo").value(40.0))
            .andExpect(jsonPath("$.devolucion.importeReembolsable").value(10.0))
            .andReturn();
        long idDevolucion = ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.devolucion.id"
        )).longValue();

        CuentaCobrar ajustada = cuentaRepository.findById(cuenta.getId())
            .orElseThrow();
        assertThat(ajustada.getTotal()).isEqualByComparingTo("10.00");
        assertThat(ajustada.getImportePagado()).isEqualByComparingTo("10.00");
        assertThat(ajustada.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(ajustada.getEstado()).isEqualTo(EstadoCuentaCobrar.PAGADO);

        mockMvc.perform(post(
                "/api/v1/devoluciones/{id}/reembolso",
                idDevolucion
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.REEMBOLSOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idMetodoPago": %d,
                      "importe": 10.00
                    }
                    """.formatted(efectivo.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.estado").value("REEMBOLSADA"));

        CuentaCobrar reembolsada = cuentaRepository.findById(cuenta.getId())
            .orElseThrow();
        assertThat(reembolsada.getTotal()).isEqualByComparingTo("0.00");
        assertThat(reembolsada.getImportePagado()).isEqualByComparingTo("0.00");
        assertThat(reembolsada.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(reembolsada.getEstado()).isEqualTo(EstadoCuentaCobrar.ANULADO);
    }

    @Test
    void cambiaProductoDelMismoValorYRegistraSalidaEnKardex() throws Exception {
        long id = idDevolucion(registrarDevolucion(
            "1.000",
            "APTO",
            "CAMBIO"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.estado")
                .value("PENDIENTE_CAMBIO"))
            .andReturn());

        mockMvc.perform(post("/api/v1/devoluciones/{id}/cambio", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.DEVOLUCIONES_VER
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCambio("1.000", "25.00", false)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/devoluciones/{id}/cambio", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.CAMBIOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCambio("1.000", "25.00", false)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.estado").value("CAMBIADA"))
            .andExpect(jsonPath("$.devolucion.importeReemplazo").value(25.0))
            .andExpect(jsonPath("$.devolucion.importeCobrado").value(0.0))
            .andExpect(jsonPath("$.resolucion.usuarioLogin")
                .value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.resolucion.reemplazos[0].idProducto")
                .value(productoReemplazo.getId()))
            .andExpect(jsonPath("$.resolucion.reemplazos[0].subtotal")
                .value(25.0));

        Inventario original = inventarioRepository.findById(inventario.getId())
            .orElseThrow();
        Inventario reemplazo = inventarioRepository
            .findById(inventarioReemplazo.getId()).orElseThrow();
        assertThat(original.getStockFisico()).isEqualByComparingTo("19.000");
        assertThat(reemplazo.getStockFisico()).isEqualByComparingTo("7.000");
        assertThat(movimientosCambio(id)).singleElement().satisfies(movimiento -> {
            assertThat(movimiento.getTipoMovimiento())
                .isEqualTo(TipoMovimientoInventario.DEVOLUCION_SALIDA);
            assertThat(movimiento.getCantidadBase())
                .isEqualByComparingTo("-1.000");
        });
        assertThat(movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesion.getId())).isEmpty();
    }

    @Test
    void cobraEnCajaLaDiferenciaDeUnReemplazoMasCaro() throws Exception {
        precioReemplazo.setMonto(new BigDecimal("30.00"));
        precioProductoRepository.saveAndFlush(precioReemplazo);
        long id = idDevolucion(registrarDevolucion(
            "1.000",
            "DEFECTUOSO",
            "CAMBIO"
        ).andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/v1/devoluciones/{id}/cambio", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.CAMBIOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCambio("1.000", "30.00", true)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.importeReemplazo").value(30.0))
            .andExpect(jsonPath("$.devolucion.importeCobrado").value(5.0))
            .andExpect(jsonPath("$.resolucion.metodoPagoCodigo")
                .value("EFECTIVO"));

        assertThat(movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesion.getId()))
            .singleElement()
            .satisfies(movimiento -> {
                assertThat(movimiento.getTipo())
                    .isEqualTo(TipoMovimientoCaja.INGRESO);
                assertThat(movimiento.getConcepto())
                    .isEqualTo(ConceptoMovimientoCaja.CAMBIO_COBRO);
                assertThat(movimiento.getImporte()).isEqualByComparingTo("5.00");
            });
    }

    @Test
    void devuelveEnCajaLaDiferenciaDeUnReemplazoMasBarato() throws Exception {
        precioReemplazo.setMonto(new BigDecimal("20.00"));
        precioProductoRepository.saveAndFlush(precioReemplazo);
        long id = idDevolucion(registrarDevolucion(
            "1.000",
            "DEFECTUOSO",
            "CAMBIO"
        ).andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/v1/devoluciones/{id}/cambio", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.CAMBIOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCambio("1.000", "20.00", true)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.importeReemplazo").value(20.0))
            .andExpect(jsonPath("$.devolucion.importeReembolsable").value(5.0))
            .andExpect(jsonPath("$.devolucion.importeReembolsado").value(5.0))
            .andExpect(jsonPath("$.items[0].importeReembolso").value(5.0));

        assertThat(movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesion.getId()))
            .singleElement()
            .satisfies(movimiento -> {
                assertThat(movimiento.getTipo())
                    .isEqualTo(TipoMovimientoCaja.EGRESO);
                assertThat(movimiento.getConcepto())
                    .isEqualTo(ConceptoMovimientoCaja.CAMBIO_REEMBOLSO);
                assertThat(movimiento.getImporte()).isEqualByComparingTo("5.00");
            });
    }

    @Test
    void autorizaDescuentoDefectuosoSinRetornarProductoAlStock() throws Exception {
        long id = idDevolucion(registrarDevolucion(
            "1.000",
            "DEFECTUOSO",
            "DESCUENTO"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.estado")
                .value("PENDIENTE_DESCUENTO"))
            .andExpect(jsonPath("$.items[0].reincorporadoInventario")
                .value(false))
            .andReturn());

        assertThat(inventarioRepository.findById(inventario.getId())
            .orElseThrow().getStockFisico()).isEqualByComparingTo("18.000");
        assertThat(ventaRepository.findById(venta.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoVenta.REGISTRADA);

        mockMvc.perform(post("/api/v1/devoluciones/{id}/descuento", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.DEVOLUCIONES_VER
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"importe": 10.00, "idMetodoPago": %d}
                    """.formatted(efectivo.getId())))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/devoluciones/{id}/descuento", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.DESCUENTOS_APLICAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "importe": 10.00,
                      "idMetodoPago": %d,
                      "referencia": "ACUERDO-DEFECTUOSO"
                    }
                    """.formatted(efectivo.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.estado").value("DESCONTADA"))
            .andExpect(jsonPath("$.devolucion.importeReembolsado").value(10.0))
            .andExpect(jsonPath("$.items[0].descuentoAplicado").value(10.0))
            .andExpect(jsonPath("$.resolucion.importeDescuento").value(10.0))
            .andExpect(jsonPath("$.resolucion.usuarioLogin")
                .value(usuario.getUsuarioLogin()));

        assertThat(movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesion.getId()))
            .singleElement()
            .satisfies(movimiento -> {
                assertThat(movimiento.getConcepto())
                    .isEqualTo(ConceptoMovimientoCaja.DESCUENTO_REEMBOLSO);
                assertThat(movimiento.getImporte()).isEqualByComparingTo("10.00");
            });
    }

    @Test
    void aplicaDescuentoPrimeroAlSaldoSinExigirCaja() throws Exception {
        venta.setCondicionPago(CondicionPagoVenta.PARCIAL);
        ventaRepository.saveAndFlush(venta);
        CuentaCobrar cuenta = new CuentaCobrar();
        cuenta.setVenta(venta);
        cuenta.setTotal(new BigDecimal("50.00"));
        cuenta.setImportePagado(new BigDecimal("10.00"));
        cuenta.setSaldoPendiente(new BigDecimal("40.00"));
        cuenta.setFechaVencimiento(LocalDate.now().plusDays(15));
        cuenta.setEstado(EstadoCuentaCobrar.PARCIAL);
        cuenta = cuentaRepository.saveAndFlush(cuenta);

        long id = idDevolucion(registrarDevolucion(
            "1.000",
            "DANADO",
            "DESCUENTO"
        ).andExpect(status().isCreated()).andReturn());
        mockMvc.perform(post("/api/v1/devoluciones/{id}/descuento", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.DESCUENTOS_APLICAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"importe": 10.00, "referencia": "AJUSTE-SALDO"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.devolucion.importeAplicadoSaldo")
                .value(10.0))
            .andExpect(jsonPath("$.devolucion.importeReembolsable")
                .value(0.0))
            .andExpect(jsonPath("$.resolucion.metodoPagoCodigo").doesNotExist());

        CuentaCobrar ajustada = cuentaRepository.findById(cuenta.getId())
            .orElseThrow();
        assertThat(ajustada.getTotal()).isEqualByComparingTo("40.00");
        assertThat(ajustada.getSaldoPendiente()).isEqualByComparingTo("30.00");
        assertThat(movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesion.getId())).isEmpty();
    }

    @Test
    void validaCantidadesSolucionPermisosYReembolsoPendiente() throws Exception {
        registrarDevolucion("2.001", "APTO")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "supera lo pendiente de devolver"
            )));

        mockMvc.perform(post("/api/v1/devoluciones")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.DEVOLUCIONES_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDevolucion("1.000", "APTO")
                    .replace("REEMBOLSO", "DESCUENTO")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "solo corresponde a productos defectuosos"
            )));

        MvcResult resultado = registrarDevolucion("1.000", "PENDIENTE")
            .andExpect(status().isCreated())
            .andReturn();
        long id = ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.devolucion.id"
        )).longValue();
        mockMvc.perform(post("/api/v1/devoluciones/{id}/reembolso", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.REEMBOLSOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idMetodoPago": %d, "importe": 24.00}
                    """.formatted(efectivo.getId())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(
                "El importe debe coincidir con el reembolso pendiente: 25.00"
            ));

        mockMvc.perform(get("/api/v1/devoluciones")
                .header(HttpHeaders.AUTHORIZATION, bearer("VEN_VENTAS_VER")))
            .andExpect(status().isForbidden());

        Set<String> esperados = Set.of(
            PermisosDevolucion.DEVOLUCIONES_VER,
            PermisosDevolucion.DEVOLUCIONES_CREAR,
            PermisosDevolucion.REEMBOLSOS_CREAR,
            PermisosDevolucion.CAMBIOS_CREAR,
            PermisosDevolucion.DESCUENTOS_APLICAR
        );
        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Devoluciones")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(Collectors.toSet());
        assertThat(registrados).containsExactlyInAnyOrderElementsOf(esperados);
    }

    @Test
    void exigeCajaAbiertaParaEntregarElReembolso() throws Exception {
        MvcResult resultado = registrarDevolucion("1.000", "APTO")
            .andExpect(status().isCreated())
            .andReturn();
        long id = ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.devolucion.id"
        )).longValue();

        sesion.setUsuarioCierre(usuario);
        sesion.setFechaHoraCierre(Instant.now());
        sesion.setSaldoEsperado(new BigDecimal("100.00"));
        sesion.setSaldoReal(new BigDecimal("100.00"));
        sesion.setDiferencia(BigDecimal.ZERO.setScale(2));
        sesion.setEstado(EstadoSesionCaja.CERRADA);
        sesionRepository.saveAndFlush(sesion);

        mockMvc.perform(post("/api/v1/devoluciones/{id}/reembolso", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosDevolucion.REEMBOLSOS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idMetodoPago": %d, "importe": 25.00}
                    """.formatted(efectivo.getId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Debe abrir una caja antes de registrar un reembolso"
            ));
    }

    private org.springframework.test.web.servlet.ResultActions registrarDevolucion(
        String cantidad,
        String estadoProducto
    ) throws Exception {
        return registrarDevolucion(cantidad, estadoProducto, "REEMBOLSO");
    }

    private org.springframework.test.web.servlet.ResultActions registrarDevolucion(
        String cantidad,
        String estadoProducto,
        String tipoSolucion
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/devoluciones")
            .header(HttpHeaders.AUTHORIZATION, bearer(
                PermisosDevolucion.DEVOLUCIONES_CREAR
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoDevolucion(cantidad, estadoProducto)
                .replace("REEMBOLSO", tipoSolucion)));
    }

    private long idDevolucion(MvcResult resultado) throws Exception {
        return ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.devolucion.id"
        )).longValue();
    }

    private String cuerpoCambio(
        String cantidad,
        String precio,
        boolean incluirMetodo
    ) {
        String metodo = incluirMetodo
            ? "\"idMetodoPago\": " + efectivo.getId() + ","
            : "";
        return """
            {
              "items": [
                {
                  "idProducto": %d,
                  "idUnidadMedida": %d,
                  "cantidad": %s,
                  "precioUnitario": %s
                }
              ],
              %s
              "referencia": "CAMBIO-CLIENTE"
            }
            """.formatted(
                productoReemplazo.getId(),
                productoReemplazo.getUnidadBase().getId(),
                cantidad,
                precio,
                metodo
            );
    }

    private String cuerpoDevolucion(String cantidad, String estadoProducto) {
        return """
            {
              "idVenta": %d,
              "motivo": "Solicitud del cliente",
              "tipoSolucion": "REEMBOLSO",
              "items": [
                {
                  "idDetalleVenta": %d,
                  "cantidad": %s,
                  "estadoProducto": "%s"
                }
              ]
            }
            """.formatted(
                venta.getId(),
                detalleVenta.getId(),
                cantidad,
                estadoProducto
            );
    }

    private List<pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario>
        movimientosInventario(long idDevolucion) {
        return movimientoInventarioRepository.findAll().stream()
            .filter(movimiento -> "DEVOLUCION".equals(
                movimiento.getDocumentoOrigen()
            ))
            .filter(movimiento -> Long.valueOf(idDevolucion).equals(
                movimiento.getIdOrigen()
            ))
            .toList();
    }

    private List<pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario>
        movimientosCambio(long idDevolucion) {
        return movimientoInventarioRepository.findAll().stream()
            .filter(movimiento -> "CAMBIO".equals(
                movimiento.getDocumentoOrigen()
            ))
            .filter(movimiento -> Long.valueOf(idDevolucion).equals(
                movimiento.getIdOrigen()
            ))
            .toList();
    }

    private String nuevoDni() {
        return Long.toString(
            10_000_000L + Math.abs(UUID.randomUUID().getLeastSignificantBits())
                % 90_000_000L
        );
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
