package pe.com.proveperu.sgc.venta.api.controller;

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
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.caja.domain.model.Caja;
import pe.com.proveperu.sgc.caja.domain.model.ConceptoMovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoSesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.SesionCaja;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.CajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.MovimientoCajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.SesionCajaRepository;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.inventario.application.service.InventarioService;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.impresion.application.service.PermisosImpresion;
import pe.com.proveperu.sgc.pedido.domain.model.CanalPedido;
import pe.com.proveperu.sgc.pedido.domain.model.DetallePedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoPedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoReservaStock;
import pe.com.proveperu.sgc.pedido.domain.model.Pedido;
import pe.com.proveperu.sgc.pedido.domain.model.ReservaStock;
import pe.com.proveperu.sgc.pedido.infrastructure.persistence.PedidoRepository;
import pe.com.proveperu.sgc.pedido.infrastructure.persistence.ReservaStockRepository;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.CuentaCobrarRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.PagoClienteRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.VentaRepository;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class VentasIntegrationTests {

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
    private PrecioProductoRepository precioRepository;

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
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ReservaStockRepository reservaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ComprobanteRepository comprobanteRepository;

    @Autowired
    private CuentaCobrarRepository cuentaRepository;

    @Autowired
    private PagoClienteRepository pagoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private SesionCajaRepository sesionCajaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    private Usuario usuario;
    private Cliente cliente;
    private Producto producto;
    private UnidadMedida unidadBase;
    private Sede sede;
    private Inventario inventario;
    private MetodoPago efectivo;
    private SesionCaja sesionCaja;
    private LocalDate hoy;

    @BeforeEach
    void prepararDatos() {
        hoy = LocalDate.now(ZONA_NEGOCIO);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría venta " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        unidadBase = new UnidadMedida();
        unidadBase.setCodigo("UV" + sufijo);
        unidadBase.setNombre("Unidad venta " + sufijo);
        unidadBase.setPermiteDecimales(true);
        unidadBase.setEstado(EstadoCatalogo.ACTIVO);
        unidadBase = unidadMedidaRepository.save(unidadBase);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidadBase);
        producto.setCodigoInterno("VEN-" + sufijo);
        producto.setNombre("Producto venta " + sufijo);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        PrecioProducto precio = new PrecioProducto();
        precio.setProducto(producto);
        precio.setTipoPrecio("MINORISTA");
        precio.setMonto(new BigDecimal("25.00"));
        precio.setVigenteDesde(hoy.minusDays(10));
        precio.setEstado(EstadoCatalogo.ACTIVO);
        precioRepository.save(precio);

        cliente = new Cliente();
        cliente.setTipoPersona(TipoPersona.NATURAL);
        cliente.setTipoDocumento(TipoDocumentoCliente.DNI);
        cliente.setNumeroDocumento(nuevoDni());
        cliente.setNombres("Cliente");
        cliente.setApellidos("Venta " + sufijo);
        cliente.setEstado(EstadoCatalogo.ACTIVO);
        cliente = clienteRepository.save(cliente);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario venta " + sufijo);
        usuario.setUsuarioLogin("venta-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
        inventario = new Inventario();
        inventario.setSede(sede);
        inventario.setProducto(producto);
        inventario.setStockFisico(new BigDecimal("20.000"));
        inventario.setStockReservado(BigDecimal.ZERO.setScale(3));
        inventario = inventarioRepository.save(inventario);

        efectivo = metodoPagoRepository.findByCodigoIgnoreCase("EFECTIVO")
            .orElseThrow();

        Caja caja = new Caja();
        caja.setSede(sede);
        caja.setNombre("Caja venta " + sufijo);
        caja.setEstado(EstadoCaja.ACTIVO);
        caja = cajaRepository.saveAndFlush(caja);
        sesionCaja = new SesionCaja();
        sesionCaja.setCaja(caja);
        sesionCaja.setUsuarioApertura(usuario);
        sesionCaja.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        sesionCaja.setEstado(EstadoSesionCaja.ABIERTA);
        sesionCaja = sesionCajaRepository.saveAndFlush(sesionCaja);
    }

    @Test
    void registraVentaContadoDescuentaStockYConservaPago() throws Exception {
        MvcResult resultado = crearVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            PermisosVenta.VENTAS_CREAR
        )
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.venta.estado").value("REGISTRADA"))
            .andExpect(jsonPath("$.venta.total").value(50.0))
            .andExpect(jsonPath("$.venta.importePagado").value(50.0))
            .andExpect(jsonPath("$.venta.saldoPendiente").value(0.0))
            .andExpect(jsonPath("$.venta.vendedorLogin")
                .value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.pagos[0].metodoPagoCodigo").value("EFECTIVO"))
            .andReturn();
        long idVenta = idVenta(resultado);

        Inventario actualizado = inventarioActual();
        assertThat(actualizado.getStockFisico()).isEqualByComparingTo("18.000");
        assertThat(actualizado.getStockReservado()).isEqualByComparingTo("0.000");
        MovimientoInventario movimiento = movimientosVenta(idVenta).getFirst();
        assertThat(movimiento.getTipoMovimiento())
            .isEqualTo(TipoMovimientoInventario.VENTA);
        assertThat(movimiento.getCantidadBase()).isEqualByComparingTo("-2.000");
        assertThat(movimiento.getStockAnterior()).isEqualByComparingTo("20.000");
        assertThat(movimiento.getStockResultante()).isEqualByComparingTo("18.000");
        assertThat(pagoRepository.findAllByVentaIdOrderByFechaHoraDescIdDesc(idVenta))
            .hasSize(1);
        var movimientosCaja = movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesionCaja.getId());
        assertThat(movimientosCaja).hasSize(1);
        assertThat(movimientosCaja.getFirst().getConcepto())
            .isEqualTo(ConceptoMovimientoCaja.VENTA);
        assertThat(movimientosCaja.getFirst().getImporte())
            .isEqualByComparingTo("50.00");
        assertThat(cuentaRepository.findByVentaId(idVenta)).isEmpty();
    }

    @Test
    void rechazaVentaConPagoCuandoElVendedorNoTieneCajaAbierta() throws Exception {
        sesionCaja.setUsuarioCierre(usuario);
        sesionCaja.setFechaHoraCierre(Instant.now());
        sesionCaja.setSaldoEsperado(BigDecimal.ZERO.setScale(2));
        sesionCaja.setSaldoReal(BigDecimal.ZERO.setScale(2));
        sesionCaja.setDiferencia(BigDecimal.ZERO.setScale(2));
        sesionCaja.setEstado(EstadoSesionCaja.CERRADA);
        sesionCajaRepository.saveAndFlush(sesionCaja);
        crearVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            PermisosVenta.VENTAS_CREAR
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "abrir una caja"
            )));
    }

    @Test
    void ventaDirectaNoPuedeConsumirStockReservado() throws Exception {
        inventario.setStockFisico(new BigDecimal("5.000"));
        inventario.setStockReservado(new BigDecimal("4.000"));
        inventarioRepository.saveAndFlush(inventario);
        long movimientosAntes = movimientoRepository.count();

        crearVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            PermisosVenta.VENTAS_CREAR
        )
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "Disponible: 1.000"
            )));

        assertThat(inventarioActual().getStockFisico())
            .isEqualByComparingTo("5.000");
        assertThat(movimientoRepository.count()).isEqualTo(movimientosAntes);
    }

    @Test
    void conviertePedidoConfirmadoYConsumeSuReservaUnaSolaVez() throws Exception {
        Pedido pedido = crearPedidoConfirmado(new BigDecimal("3.000"));

        MvcResult resultado = mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idPedido": %d,
                      "tipoVenta": "MINORISTA",
                      "condicionPago": "CONTADO",
                      "idMetodoPago": %d,
                      "tipoComprobante": "NOTA_VENTA"
                    }
                    """.formatted(pedido.getId(), efectivo.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venta.idPedido").value(pedido.getId()))
            .andExpect(jsonPath("$.venta.total").value(75.0))
            .andReturn();
        long idVenta = idVenta(resultado);

        Inventario actualizado = inventarioActual();
        assertThat(actualizado.getStockFisico()).isEqualByComparingTo("17.000");
        assertThat(actualizado.getStockReservado()).isEqualByComparingTo("0.000");
        assertThat(reservaRepository.findAllByPedidoIdOrderByIdAsc(pedido.getId())
            .getFirst().getEstado()).isEqualTo(EstadoReservaStock.CONSUMIDA);
        assertThat(pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(movimientosVenta(idVenta)).singleElement()
            .extracting(MovimientoInventario::getTipoMovimiento)
            .isEqualTo(TipoMovimientoInventario.VENTA);

        mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idPedido": %d,
                      "tipoVenta": "MINORISTA",
                      "condicionPago": "CONTADO",
                      "idMetodoPago": %d,
                      "tipoComprobante": "NOTA_VENTA"
                    }
                    """.formatted(pedido.getId(), efectivo.getId())))
            .andExpect(status().isConflict());
    }

    @Test
    void ventaCreditoCreaCuentaSinRegistrarPago() throws Exception {
        MvcResult resultado = crearVentaDirecta(
            "CREDITO",
            "",
            "\"fechaVencimiento\": \"%s\",".formatted(hoy.plusDays(15)),
            PermisosVenta.VENTAS_CREAR
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venta.importePagado").value(0.0))
            .andExpect(jsonPath("$.venta.saldoPendiente").value(50.0))
            .andExpect(jsonPath("$.cuentaCobrar.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.cuentaCobrar.fechaVencimiento")
                .value(hoy.plusDays(15).toString()))
            .andExpect(jsonPath("$.pagos").isEmpty())
            .andReturn();
        long idVenta = idVenta(resultado);

        assertThat(cuentaRepository.findByVentaId(idVenta)).isPresent();
        assertThat(pagoRepository.findAllByVentaIdOrderByFechaHoraDescIdDesc(idVenta))
            .isEmpty();
    }

    @Test
    void ventaParcialRegistraPagoInicialYSaldo() throws Exception {
        MvcResult resultado = crearVentaDirecta(
            "PARCIAL",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            """
            "montoPagado": 10.00,
            "fechaVencimiento": "%s",
            """.formatted(hoy.plusDays(7)),
            PermisosVenta.VENTAS_CREAR
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venta.importePagado").value(10.0))
            .andExpect(jsonPath("$.venta.saldoPendiente").value(40.0))
            .andExpect(jsonPath("$.cuentaCobrar.estado").value("PARCIAL"))
            .andExpect(jsonPath("$.pagos[0].monto").value(10.0))
            .andReturn();

        assertThat(cuentaRepository.findByVentaId(idVenta(resultado)).orElseThrow()
            .getEstado()).isEqualTo(EstadoCuentaCobrar.PARCIAL);
    }

    @Test
    void anulaVentaReponeStockYAnulaSaldoSinBorrarHistorial() throws Exception {
        MvcResult resultado = crearVentaDirecta(
            "PARCIAL",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            """
            "montoPagado": 10.00,
            "fechaVencimiento": "%s",
            """.formatted(hoy.plusDays(7)),
            PermisosVenta.VENTAS_CREAR
        ).andExpect(status().isCreated()).andReturn();
        long idVenta = idVenta(resultado);

        mockMvc.perform(post("/api/v1/ventas/{id}/anular", idVenta)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.VENTAS_ANULAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Error de digitación\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.venta.estado").value("ANULADA"))
            .andExpect(jsonPath("$.venta.motivoAnulacion")
                .value("Error de digitación"))
            .andExpect(jsonPath("$.venta.saldoPendiente").value(0.0))
            .andExpect(jsonPath("$.cuentaCobrar.estado").value("ANULADO"))
            .andExpect(jsonPath("$.cuentaCobrar.saldoPendiente").value(0.0))
            .andExpect(jsonPath("$.pagos[0].monto").value(10.0));

        assertThat(inventarioActual().getStockFisico())
            .isEqualByComparingTo("20.000");
        assertThat(ventaRepository.findById(idVenta).orElseThrow().getEstado())
            .isEqualTo(EstadoVenta.ANULADA);
        assertThat(comprobanteRepository.findByVentaId(idVenta).orElseThrow()
            .getEstado()).isEqualTo(EstadoComprobante.ANULADO);
        assertThat(movimientosVenta(idVenta).stream()
            .map(MovimientoInventario::getTipoMovimiento))
            .containsExactlyInAnyOrder(
                TipoMovimientoInventario.VENTA,
                TipoMovimientoInventario.ANULACION_VENTA
            );
        assertThat(pagoRepository.findAllByVentaIdOrderByFechaHoraDescIdDesc(idVenta))
            .hasSize(1);
    }

    @Test
    void validaPrecioVigenteClienteYPermisoDeDescuento() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoVentaDirecta(
                    "CONTADO",
                    "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
                    "",
                    "24.00",
                    "0.00"
                )))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "precio vigente"
            )));

        mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoVentaDirecta(
                    "CONTADO",
                    "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
                    "",
                    "25.00",
                    "5.00"
                )))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "No tiene permiso para aplicar descuentos en ventas"
            ));
    }

    @Test
    void protegeEndpointsExponeComprobanteYRegistraPermisos() throws Exception {
        MvcResult resultado = crearVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            PermisosVenta.VENTAS_CREAR
        ).andExpect(status().isCreated()).andReturn();
        long idVenta = idVenta(resultado);

        MvcResult comprobanteResult = mockMvc.perform(get(
                "/api/v1/ventas/{id}/comprobante",
                idVenta
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("NOTA_VENTA"))
            .andExpect(jsonPath("$.serie").value("NV01"))
            .andExpect(jsonPath("$.numero").value(
                org.hamcrest.Matchers.matchesPattern("[0-9]{8,}")
            ))
            .andExpect(jsonPath("$.numeroCompleto").value(
                org.hamcrest.Matchers.startsWith("NV01-")
            ))
            .andExpect(jsonPath("$.estado").value("EMITIDO"))
            .andExpect(jsonPath("$.items[0].codigoProducto")
                .value(producto.getCodigoInterno()))
            .andReturn();
        long idComprobante = ((Number) JsonPath.read(
            comprobanteResult.getResponse().getContentAsString(),
            "$.id"
        )).longValue();

        mockMvc.perform(get("/api/v1/comprobantes/{id}", idComprobante)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idVenta").value(idVenta))
            .andExpect(jsonPath("$.numeroCompleto").value(
                org.hamcrest.Matchers.startsWith("NV01-")
            ));

        mockMvc.perform(get(
                "/api/v1/comprobantes/{id}/representacion",
                idComprobante
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("NOTA DE VENTA"))
            .andExpect(jsonPath("$.moneda").value("PEN"))
            .andExpect(jsonPath("$.emisor.ruc").value("20612296911"))
            .andExpect(jsonPath("$.cliente.numeroDocumento")
                .value(cliente.getNumeroDocumento()))
            .andExpect(jsonPath("$.comprobante.id").value(idComprobante));

        mockMvc.perform(get("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer("INV_STOCK_VER")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_VER
                )))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ventas/metodos-pago")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.codigo == 'EFECTIVO')]").exists());

        Set<String> esperados = Set.of(
            PermisosVenta.VENTAS_VER,
            PermisosVenta.VENTAS_CREAR,
            PermisosVenta.VENTAS_ANULAR,
            PermisosVenta.DESCUENTOS_APLICAR
        );
        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Ventas")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(Collectors.toSet());
        assertThat(registrados).containsExactlyInAnyOrderElementsOf(esperados);

        Set<String> esperadosComprobantes = Set.of(
            PermisosVenta.COMPROBANTES_VER,
            PermisosVenta.COMPROBANTES_ANULAR
        );
        Set<String> registradosComprobantes = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Comprobantes")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(Collectors.toSet());
        assertThat(registradosComprobantes)
            .containsExactlyInAnyOrderElementsOf(esperadosComprobantes);

        Set<String> esperadosTicketera = Set.of(
            PermisosImpresion.TICKETS_IMPRIMIR
        );
        Set<String> registradosTicketera = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Ticketera")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(Collectors.toSet());
        assertThat(registradosTicketera)
            .containsExactlyInAnyOrderElementsOf(esperadosTicketera);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        Set<String> todosEsperados = java.util.stream.Stream.of(
                esperados,
                esperadosComprobantes,
                esperadosTicketera
            )
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        Set<String> asignados = rolRepository.findByIdWithPermisos(administrador.getId())
            .orElseThrow()
            .getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .filter(todosEsperados::contains)
            .collect(Collectors.toSet());
        assertThat(asignados)
            .containsExactlyInAnyOrderElementsOf(todosEsperados);
    }

    @Test
    void generaTicketTermicoDe58Y80Milimetros() throws Exception {
        MvcResult ventaResult = crearVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            PermisosVenta.VENTAS_CREAR
        ).andExpect(status().isCreated()).andReturn();
        long idComprobante = comprobanteRepository
            .findByVentaId(idVenta(ventaResult))
            .orElseThrow()
            .getId();

        MvcResult ticket58 = mockMvc.perform(get(
                "/api/v1/impresiones/ticket/{idComprobante}",
                idComprobante
            )
                .queryParam("formato", "MM58")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosImpresion.TICKETS_IMPRIMIR
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formato").value("MM58"))
            .andExpect(jsonPath("$.anchoCaracteres").value(32))
            .andExpect(jsonPath("$.codificacion").value("UTF-8"))
            .andExpect(jsonPath("$.incluyeComandosEscPos").value(false))
            .andExpect(jsonPath("$.contenido").value(
                org.hamcrest.Matchers.containsString("PROVEPERU")
            ))
            .andExpect(jsonPath("$.contenido").value(
                org.hamcrest.Matchers.containsString(producto.getCodigoInterno())
            ))
            .andExpect(jsonPath("$.contenido").value(
                org.hamcrest.Matchers.containsString("TOTAL PEN")
            ))
            .andReturn();
        String contenido58 = JsonPath.read(
            ticket58.getResponse().getContentAsString(),
            "$.contenido"
        );
        assertThat(contenido58.lines())
            .allMatch(linea -> linea.length() <= 32);

        mockMvc.perform(get(
                "/api/v1/impresiones/ticket/{idComprobante}",
                idComprobante
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosImpresion.TICKETS_IMPRIMIR
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formato").value("MM80"))
            .andExpect(jsonPath("$.anchoCaracteres").value(48));

        mockMvc.perform(get(
                "/api/v1/impresiones/ticket/{idComprobante}",
                idComprobante
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_VER
                )))
            .andExpect(status().isForbidden());
    }

    @Test
    void emiteBoletaYFacturaConCorrelativosIndependientes() throws Exception {
        String boleta = cuerpoVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            "25.00",
            "0.00"
        ).replace("\"NOTA_VENTA\"", "\"BOLETA\"");
        MvcResult ventaBoleta = mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.VENTAS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(boleta))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venta.tipoComprobante").value("BOLETA"))
            .andExpect(jsonPath("$.venta.numeroComprobante").value(
                org.hamcrest.Matchers.startsWith("B001-")
            ))
            .andReturn();
        mockMvc.perform(get(
                "/api/v1/ventas/{id}/comprobante",
                idVenta(ventaBoleta)
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serie").value("B001"));

        Cliente empresa = new Cliente();
        empresa.setTipoPersona(TipoPersona.JURIDICA);
        empresa.setTipoDocumento(TipoDocumentoCliente.RUC);
        empresa.setNumeroDocumento(nuevoRuc());
        empresa.setRazonSocial("Cliente Factura " + UUID.randomUUID());
        empresa.setEstado(EstadoCatalogo.ACTIVO);
        cliente = clienteRepository.saveAndFlush(empresa);
        String factura = cuerpoVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            "25.00",
            "0.00"
        ).replace("\"NOTA_VENTA\"", "\"FACTURA\"");
        MvcResult ventaFactura = mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.VENTAS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(factura))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venta.tipoComprobante").value("FACTURA"))
            .andExpect(jsonPath("$.venta.numeroComprobante").value(
                org.hamcrest.Matchers.startsWith("F001-")
            ))
            .andReturn();
        mockMvc.perform(get(
                "/api/v1/ventas/{id}/comprobante",
                idVenta(ventaFactura)
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serie").value("F001"));
    }

    @Test
    void rechazaFacturaSinClienteRucYRevierteTodaLaVenta() throws Exception {
        long ventasAntes = ventaRepository.count();
        long comprobantesAntes = comprobanteRepository.count();
        String factura = cuerpoVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            "25.00",
            "0.00"
        ).replace("\"NOTA_VENTA\"", "\"FACTURA\"");

        mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.VENTAS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(factura))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Una factura requiere un cliente identificado con RUC"
            ));

        assertThat(ventaRepository.count()).isEqualTo(ventasAntes);
        assertThat(comprobanteRepository.count()).isEqualTo(comprobantesAntes);
        assertThat(inventarioActual().getStockFisico())
            .isEqualByComparingTo("20.000");
    }

    @Test
    void anulaComprobanteYVentaConTrazabilidad() throws Exception {
        MvcResult ventaResult = crearVentaDirecta(
            "CONTADO",
            "\"idMetodoPago\": %d,".formatted(efectivo.getId()),
            "",
            PermisosVenta.VENTAS_CREAR
        ).andExpect(status().isCreated()).andReturn();
        long idVenta = idVenta(ventaResult);
        long idComprobante = comprobanteRepository.findByVentaId(idVenta)
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/v1/comprobantes/{id}/anular", idComprobante)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosVenta.COMPROBANTES_ANULAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Comprobante emitido por error\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ANULADO"))
            .andExpect(jsonPath("$.motivoAnulacion")
                .value("Comprobante emitido por error"))
            .andExpect(jsonPath("$.usuarioAnulacion")
                .value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.venta.estado").value("ANULADA"));

        MvcResult ticketAnulado = mockMvc.perform(get(
                "/api/v1/impresiones/ticket/{idComprobante}",
                idComprobante
            )
                .queryParam("formato", "MM58")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosImpresion.TICKETS_IMPRIMIR
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ANULADO"))
            .andExpect(jsonPath("$.contenido").value(
                org.hamcrest.Matchers.containsString("COMPROBANTE ANULADO")
            ))
            .andReturn();

        String contenidoTicketAnulado = JsonPath.read(
            ticketAnulado.getResponse().getContentAsString(),
            "$.contenido"
        );
        assertThat(contenidoTicketAnulado.replaceAll("\\s+", " "))
            .contains("Motivo: Comprobante emitido por error");

        assertThat(inventarioActual().getStockFisico())
            .isEqualByComparingTo("20.000");
        assertThat(ventaRepository.findById(idVenta).orElseThrow().getEstado())
            .isEqualTo(EstadoVenta.ANULADA);
    }

    private org.springframework.test.web.servlet.ResultActions crearVentaDirecta(
        String condicionPago,
        String metodo,
        String financiacion,
        String... authorities
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/ventas")
            .header(HttpHeaders.AUTHORIZATION, bearer(authorities))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoVentaDirecta(
                condicionPago,
                metodo,
                financiacion,
                "25.00",
                "0.00"
            )));
    }

    private String cuerpoVentaDirecta(
        String condicionPago,
        String metodo,
        String financiacion,
        String precio,
        String descuento
    ) {
        return """
            {
              "idCliente": %d,
              "idSede": %d,
              "tipoVenta": "MINORISTA",
              "condicionPago": "%s",
              %s
              "tipoComprobante": "NOTA_VENTA",
              "igv": 0.00,
              %s
              "items": [
                {
                  "idProducto": %d,
                  "idUnidadMedida": %d,
                  "cantidad": 2.000,
                  "precioUnitario": %s,
                  "descuento": %s
                }
              ]
            }
            """.formatted(
                cliente.getId(),
                sede.getId(),
                condicionPago,
                metodo,
                financiacion,
                producto.getId(),
                unidadBase.getId(),
                precio,
                descuento
            );
    }

    private Pedido crearPedidoConfirmado(BigDecimal cantidad) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUsuario(usuario);
        pedido.setSede(sede);
        pedido.setCanal(CanalPedido.PRESENCIAL);
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido.setSubtotal(cantidad.multiply(new BigDecimal("25.00")));
        pedido.setIgv(BigDecimal.ZERO.setScale(2));
        pedido.setTotal(pedido.getSubtotal());

        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidadBase);
        detalle.setCantidad(cantidad);
        detalle.setCantidadBase(cantidad);
        detalle.setPrecioUnitario(new BigDecimal("25.00"));
        detalle.setDescuento(BigDecimal.ZERO.setScale(2));
        detalle.setSubtotal(pedido.getSubtotal());
        pedido.agregarDetalle(detalle);
        pedido = pedidoRepository.saveAndFlush(pedido);

        inventarioService.reservarParaPedido(
            sede,
            producto,
            unidadBase,
            cantidad,
            cantidad,
            usuario,
            pedido.getId()
        );
        ReservaStock reserva = new ReservaStock();
        reserva.setPedido(pedido);
        reserva.setDetallePedido(pedido.getDetalles().getFirst());
        reserva.setSede(sede);
        reserva.setProducto(producto);
        reserva.setCantidad(cantidad);
        reserva.setEstado(EstadoReservaStock.ACTIVA);
        reservaRepository.saveAndFlush(reserva);
        return pedido;
    }

    private Inventario inventarioActual() {
        return inventarioRepository.findById(inventario.getId()).orElseThrow();
    }

    private List<MovimientoInventario> movimientosVenta(long idVenta) {
        return movimientoRepository.findAll().stream()
            .filter(movimiento -> "VENTA".equals(movimiento.getDocumentoOrigen()))
            .filter(movimiento -> Long.valueOf(idVenta).equals(movimiento.getIdOrigen()))
            .toList();
    }

    private long idVenta(MvcResult resultado) throws Exception {
        return ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.venta.id"
        )).longValue();
    }

    private String nuevoDni() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(
            10_000_000,
            100_000_000
        ));
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
