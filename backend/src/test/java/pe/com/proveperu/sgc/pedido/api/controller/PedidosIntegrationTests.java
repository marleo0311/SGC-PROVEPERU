package pe.com.proveperu.sgc.pedido.api.controller;

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
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.cotizacion.application.service.PermisosCotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;
import pe.com.proveperu.sgc.cotizacion.infrastructure.persistence.CotizacionRepository;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.pedido.application.service.PermisosPedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoPedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoReservaStock;
import pe.com.proveperu.sgc.pedido.infrastructure.persistence.PedidoRepository;
import pe.com.proveperu.sgc.pedido.infrastructure.persistence.ReservaStockRepository;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class PedidosIntegrationTests {

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
    private ProductoUnidadConversionRepository conversionRepository;

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
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ReservaStockRepository reservaRepository;

    private Usuario usuario;
    private Cliente cliente;
    private Producto producto;
    private UnidadMedida unidadBase;
    private Sede sede;
    private Inventario inventario;
    private LocalDate hoy;

    @BeforeEach
    void prepararDatos() {
        hoy = LocalDate.now(ZONA_NEGOCIO);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría pedido " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        unidadBase = nuevaUnidad("UP" + sufijo, "Unidad pedido " + sufijo, true);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidadBase);
        producto.setCodigoInterno("PED-" + sufijo);
        producto.setNombre("Producto pedido " + sufijo);
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
        cliente.setApellidos("Pedido " + sufijo);
        cliente.setEstado(EstadoCatalogo.ACTIVO);
        cliente = clienteRepository.save(cliente);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario pedido " + sufijo);
        usuario.setUsuarioLogin("pedido-" + sufijo);
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
    }

    @Test
    void registraPedidoWhatsAppSinReservarYCalculaImportes() throws Exception {
        MvcResult resultado = crearPedido(
            unidadBase.getId(),
            "2.000",
            "5.00",
            "8.10",
            "WHATSAPP",
            PermisosPedido.PEDIDOS_CREAR,
            PermisosCotizacion.DESCUENTOS_APLICAR
        )
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.pedido.canal").value("WHATSAPP"))
            .andExpect(jsonPath("$.pedido.estado").value("RECIBIDO"))
            .andExpect(jsonPath("$.pedido.subtotal").value(45.0))
            .andExpect(jsonPath("$.pedido.igv").value(8.1))
            .andExpect(jsonPath("$.pedido.total").value(53.1))
            .andExpect(jsonPath("$.detalles[0].cantidadBase").value(2.0))
            .andExpect(jsonPath("$.reservas.length()").value(0))
            .andReturn();

        long id = idPedido(resultado);
        assertThat(inventarioActual().getStockReservado())
            .isEqualByComparingTo("0.000");

        mockMvc.perform(get("/api/v1/pedidos/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosPedido.PEDIDOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedido.id").value(id));
    }

    @Test
    void convierteUnaCotizacionAceptadaUnaSolaVez() throws Exception {
        long idCotizacion = crearYAceptarCotizacion();

        MvcResult resultado = mockMvc.perform(post(
                "/api/v1/cotizaciones/{id}/convertir-pedido",
                idCotizacion
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CONVERTIR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idSede": %d,
                      "canal": "PRESENCIAL",
                      "observacion": "Cliente aceptó la propuesta"
                    }
                    """.formatted(sede.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pedido.idCotizacion").value(idCotizacion))
            .andExpect(jsonPath("$.pedido.estado").value("COTIZADO"))
            .andExpect(jsonPath("$.pedido.total").value(50.0))
            .andExpect(jsonPath("$.detalles[0].precioUnitario").value(25.0))
            .andReturn();
        long idPedido = idPedido(resultado);

        assertThat(cotizacionRepository.findById(idCotizacion).orElseThrow()
            .getEstado()).isEqualTo(EstadoCotizacion.CONVERTIDA);
        assertThat(pedidoRepository.findById(idPedido).orElseThrow()
            .getCotizacion().getId()).isEqualTo(idCotizacion);
        assertThat(inventarioActual().getStockReservado())
            .isEqualByComparingTo("0.000");

        mockMvc.perform(post("/api/v1/cotizaciones/{id}/convertir-pedido", idCotizacion)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CONVERTIR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isConflict());
    }

    @Test
    void confirmaPedidoReservaStockYRegistraMovimiento() throws Exception {
        long id = crearPedidoBase("3.000");

        mockMvc.perform(post("/api/v1/pedidos/{id}/confirmar", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CONFIRMAR
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedido.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.reservas[0].estado").value("ACTIVA"))
            .andExpect(jsonPath("$.reservas[0].cantidadBase").value(3.0));

        Inventario actualizado = inventarioActual();
        assertThat(actualizado.getStockFisico()).isEqualByComparingTo("20.000");
        assertThat(actualizado.getStockReservado()).isEqualByComparingTo("3.000");
        MovimientoInventario movimiento = movimientosDelPedido(id).getFirst();
        assertThat(movimiento.getTipoMovimiento())
            .isEqualTo(TipoMovimientoInventario.RESERVA);
        assertThat(movimiento.getStockAnterior()).isEqualByComparingTo("20.000");
        assertThat(movimiento.getStockResultante()).isEqualByComparingTo("17.000");
    }

    @Test
    void reservaEnUnidadBaseCuandoElPedidoUsaUnaConversion() throws Exception {
        UnidadMedida caja = nuevaUnidad(
            "CP" + UUID.randomUUID().toString().substring(0, 8),
            "Caja pedido",
            false
        );
        ProductoUnidadConversion conversion = new ProductoUnidadConversion();
        conversion.setProducto(producto);
        conversion.setUnidadOrigen(caja);
        conversion.setUnidadDestino(unidadBase);
        conversion.setFactorConversion(new BigDecimal("5.000000"));
        conversion.setEstado(EstadoCatalogo.ACTIVO);
        conversionRepository.save(conversion);

        long id = idPedido(crearPedido(
            caja.getId(),
            "2.000",
            "0.00",
            "0.00",
            "PRESENCIAL",
            PermisosPedido.PEDIDOS_CREAR
        ).andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/v1/pedidos/{id}/confirmar", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CONFIRMAR
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.detalles[0].cantidadBase").value(10.0))
            .andExpect(jsonPath("$.reservas[0].cantidadBase").value(10.0));
        assertThat(inventarioActual().getStockReservado())
            .isEqualByComparingTo("10.000");
    }

    @Test
    void stockInsuficienteRevierteTodaLaConfirmacion() throws Exception {
        inventario.setStockFisico(new BigDecimal("1.000"));
        inventarioRepository.saveAndFlush(inventario);
        long id = crearPedidoBase("2.000");
        long movimientosAntes = movimientoRepository.count();

        mockMvc.perform(post("/api/v1/pedidos/{id}/confirmar", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CONFIRMAR
                )))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                "Stock insuficiente"
            )));

        assertThat(pedidoRepository.findById(id).orElseThrow().getEstado())
            .isEqualTo(EstadoPedido.RECIBIDO);
        assertThat(reservaRepository.findAllByPedidoIdOrderByIdAsc(id)).isEmpty();
        assertThat(inventarioActual().getStockReservado())
            .isEqualByComparingTo("0.000");
        assertThat(movimientoRepository.count()).isEqualTo(movimientosAntes);
    }

    @Test
    void cancelarPedidoLiberaReservaYConservaTrazabilidad() throws Exception {
        long id = crearPedidoBase("4.000");
        confirmar(id);

        mockMvc.perform(post("/api/v1/pedidos/{id}/cancelar", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CANCELAR
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedido.estado").value("CANCELADO"))
            .andExpect(jsonPath("$.reservas[0].estado").value("LIBERADA"))
            .andExpect(jsonPath("$.reservas[0].fechaLiberacion").exists());

        assertThat(inventarioActual().getStockReservado())
            .isEqualByComparingTo("0.000");
        assertThat(reservaRepository.findAllByPedidoIdOrderByIdAsc(id).getFirst()
            .getEstado()).isEqualTo(EstadoReservaStock.LIBERADA);
        assertThat(movimientosDelPedido(id).stream()
            .map(MovimientoInventario::getTipoMovimiento))
            .containsExactlyInAnyOrder(
                TipoMovimientoInventario.RESERVA,
                TipoMovimientoInventario.LIBERACION_RESERVA
            );
    }

    @Test
    void controlaTransicionesDePreparacionYBloqueaCancelarEntregado()
        throws Exception {
        long id = crearPedidoBase("2.000");

        cambiarEstado(id, "EN_PREPARACION")
            .andExpect(status().isConflict());
        cambiarEstado(id, "COTIZADO")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedido.estado").value("COTIZADO"));
        confirmar(id);
        cambiarEstado(id, "EN_PREPARACION")
            .andExpect(status().isOk());
        cambiarEstado(id, "LISTO")
            .andExpect(status().isOk());
        cambiarEstado(id, "ENTREGADO")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedido.estado").value("ENTREGADO"));

        mockMvc.perform(post("/api/v1/pedidos/{id}/cancelar", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CANCELAR
                )))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Un pedido ENTREGADO no puede cancelarse"
            ));
    }

    @Test
    void protegeEndpointsYRegistraPermisosDePedidos() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCompra.COMPRAS_VER)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/pedidos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosVenta.VENTAS_CREAR)))
            .andExpect(status().isOk());

        Set<String> esperados = Set.of(
            PermisosPedido.PEDIDOS_VER,
            PermisosPedido.PEDIDOS_CREAR,
            PermisosPedido.PEDIDOS_CONVERTIR,
            PermisosPedido.PEDIDOS_CONFIRMAR,
            PermisosPedido.PEDIDOS_ESTADO,
            PermisosPedido.PEDIDOS_CANCELAR,
            PermisosPedido.RESERVAS_VER
        );
        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Pedidos")
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

    private long crearPedidoBase(String cantidad) throws Exception {
        return idPedido(crearPedido(
            unidadBase.getId(),
            cantidad,
            "0.00",
            "0.00",
            "PRESENCIAL",
            PermisosPedido.PEDIDOS_CREAR
        ).andExpect(status().isCreated()).andReturn());
    }

    private org.springframework.test.web.servlet.ResultActions crearPedido(
        Long idUnidad,
        String cantidad,
        String descuento,
        String igv,
        String canal,
        String... authorities
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/pedidos")
            .header(HttpHeaders.AUTHORIZATION, bearer(authorities))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idCliente": %d,
                  "idSede": %d,
                  "canal": "%s",
                  "igv": %s,
                  "observacion": "Pedido de prueba",
                  "detalles": [
                    {
                      "idProducto": %d,
                      "idUnidadMedida": %d,
                      "cantidad": %s,
                      "tipoPrecio": "MINORISTA",
                      "descuento": %s
                    }
                  ]
                }
                """.formatted(
                    cliente.getId(),
                    sede.getId(),
                    canal,
                    igv,
                    producto.getId(),
                    idUnidad,
                    cantidad,
                    descuento
                )));
    }

    private long crearYAceptarCotizacion() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/cotizaciones")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCotizacion.COTIZACIONES_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idCliente": %d,
                      "fecha": "%s",
                      "fechaVencimiento": "%s",
                      "igv": 0.00,
                      "detalles": [
                        {
                          "idProducto": %d,
                          "idUnidadMedida": %d,
                          "cantidad": 2.000,
                          "tipoPrecio": "MINORISTA",
                          "descuento": 0.00
                        }
                      ]
                    }
                    """.formatted(
                        cliente.getId(),
                        hoy,
                        hoy.plusDays(7),
                        producto.getId(),
                        unidadBase.getId()
                    )))
            .andExpect(status().isCreated())
            .andReturn();
        long id = ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.cotizacion.id"
        )).longValue();
        mockMvc.perform(patch("/api/v1/cotizaciones/{id}/estado", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCotizacion.COTIZACIONES_ESTADO
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ACEPTADA\"}"))
            .andExpect(status().isOk());
        return id;
    }

    private void confirmar(long id) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/{id}/confirmar", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosPedido.PEDIDOS_CONFIRMAR
                )))
            .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions cambiarEstado(
        long id,
        String estado
    ) throws Exception {
        return mockMvc.perform(patch("/api/v1/pedidos/{id}/estado", id)
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosPedido.PEDIDOS_ESTADO))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"estado\":\"" + estado + "\"}"));
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

    private Inventario inventarioActual() {
        return inventarioRepository.findById(inventario.getId()).orElseThrow();
    }

    private List<MovimientoInventario> movimientosDelPedido(long idPedido) {
        return movimientoRepository.findAll().stream()
            .filter(movimiento -> "PEDIDO".equals(movimiento.getDocumentoOrigen()))
            .filter(movimiento -> Long.valueOf(idPedido).equals(movimiento.getIdOrigen()))
            .toList();
    }

    private long idPedido(MvcResult resultado) throws Exception {
        return ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.pedido.id"
        )).longValue();
    }

    private String nuevoDni() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(
            10_000_000,
            100_000_000
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
