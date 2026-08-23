package pe.com.proveperu.sgc.cuentacobrar.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
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
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.cuentacobrar.application.service.PermisosCuentaCobrar;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
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
class CuentasCobrarIntegrationTests {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

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
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private CuentaCobrarRepository cuentaRepository;

    @Autowired
    private PagoClienteRepository pagoRepository;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private SesionCajaRepository sesionCajaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    private Usuario usuario;
    private Cliente cliente;
    private MetodoPago efectivo;
    private CuentaCobrar cuenta;
    private SesionCaja sesionCaja;
    private LocalDate hoy;

    @BeforeEach
    void prepararDatos() {
        hoy = LocalDate.now(ZONA_NEGOCIO);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        cliente = new Cliente();
        cliente.setTipoPersona(TipoPersona.NATURAL);
        cliente.setTipoDocumento(TipoDocumentoCliente.DNI);
        cliente.setNumeroDocumento(nuevoDni());
        cliente.setNombres("Cliente");
        cliente.setApellidos("Cobranza " + sufijo);
        cliente.setEstado(EstadoCatalogo.ACTIVO);
        cliente = clienteRepository.save(cliente);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario cobranza " + sufijo);
        usuario.setUsuarioLogin("cobranza-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        Sede sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setVendedor(usuario);
        venta.setSede(sede);
        venta.setTipoVenta(TipoVenta.MINORISTA);
        venta.setCondicionPago(CondicionPagoVenta.CREDITO);
        venta.setTipoComprobante(TipoComprobanteVenta.NOTA_VENTA);
        venta.setSubtotal(new BigDecimal("100.00"));
        venta.setIgv(BigDecimal.ZERO.setScale(2));
        venta.setDescuentoTotal(BigDecimal.ZERO.setScale(2));
        venta.setTotal(new BigDecimal("100.00"));
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta = ventaRepository.saveAndFlush(venta);

        cuenta = new CuentaCobrar();
        cuenta.setVenta(venta);
        cuenta.setTotal(new BigDecimal("100.00"));
        cuenta.setImportePagado(BigDecimal.ZERO.setScale(2));
        cuenta.setSaldoPendiente(new BigDecimal("100.00"));
        cuenta.setFechaVencimiento(hoy.plusDays(10));
        cuenta.setEstado(EstadoCuentaCobrar.PENDIENTE);
        cuenta = cuentaRepository.saveAndFlush(cuenta);

        efectivo = metodoPagoRepository.findByCodigoIgnoreCase("EFECTIVO")
            .orElseThrow();

        Caja caja = new Caja();
        caja.setSede(sede);
        caja.setNombre("Caja cobranza " + sufijo);
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
    void listaFiltraYConsultaCuentaConSuVenta() throws Exception {
        mockMvc.perform(get("/api/v1/cuentas-cobrar")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaCobrar.CUENTAS_VER
                ))
                .param("idCliente", cliente.getId().toString())
                .param("estado", "PENDIENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido.length()").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(cuenta.getId()))
            .andExpect(jsonPath("$.contenido[0].clienteDocumento")
                .value(cliente.getNumeroDocumento()))
            .andExpect(jsonPath("$.contenido[0].numeroComprobante")
                .value("NV-%08d".formatted(cuenta.getVenta().getId())))
            .andExpect(jsonPath("$.contenido[0].saldoPendiente").value(100.0));

        mockMvc.perform(get("/api/v1/cuentas-cobrar/{id}", cuenta.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaCobrar.CUENTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cuenta.id").value(cuenta.getId()))
            .andExpect(jsonPath("$.cuenta.condicionPago").value("CREDITO"))
            .andExpect(jsonPath("$.pagos").isEmpty());
    }

    @Test
    void registraPagoParcialYLuegoCancelaTodaLaCuenta() throws Exception {
        registrarPago("30.00", "ABONO-30")
            .andExpect(status().isCreated())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                "/api/v1/cuentas-cobrar/" + cuenta.getId()
            ))
            .andExpect(jsonPath("$.cuenta.importePagado").value(30.0))
            .andExpect(jsonPath("$.cuenta.saldoPendiente").value(70.0))
            .andExpect(jsonPath("$.cuenta.estado").value("PARCIAL"))
            .andExpect(jsonPath("$.pagos[0].referencia").value("ABONO-30"))
            .andExpect(jsonPath("$.pagos[0].usuarioLogin")
                .value(usuario.getUsuarioLogin()));

        registrarPago("70.00", "CANCELACION")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cuenta.importePagado").value(100.0))
            .andExpect(jsonPath("$.cuenta.saldoPendiente").value(0.0))
            .andExpect(jsonPath("$.cuenta.estado").value("PAGADO"))
            .andExpect(jsonPath("$.pagos.length()").value(2));

        registrarPago("1.00", "NO-DEBE-ACEPTARSE")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "La cuenta PAGADO no admite nuevos pagos"
            ));

        var movimientosCaja = movimientoCajaRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(sesionCaja.getId());
        assertThat(movimientosCaja).hasSize(2);
        assertThat(movimientosCaja)
            .allMatch(movimiento -> movimiento.getConcepto()
                == ConceptoMovimientoCaja.PAGO_CLIENTE);
        assertThat(movimientosCaja)
            .extracting(movimiento -> movimiento.getImporte())
            .containsExactlyInAnyOrder(
                new BigDecimal("30.00"),
                new BigDecimal("70.00")
            );
    }

    @Test
    void rechazaPagoMayorAlSaldoYMetodoInactivo() throws Exception {
        registrarPago("100.01", "EXCESO")
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(
                "El monto supera el saldo pendiente: 100.00"
            ));
        assertThat(cuentaRepository.findById(cuenta.getId()).orElseThrow()
            .getSaldoPendiente()).isEqualByComparingTo("100.00");

        efectivo.setEstado("INACTIVO");
        metodoPagoRepository.saveAndFlush(efectivo);
        registrarPago("10.00", "METODO-INACTIVO")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "El método de pago seleccionado no está activo"
            ));
        assertThat(pagoRepository
            .findAllByCuentaCobrarIdOrderByFechaHoraDescIdDesc(cuenta.getId()))
            .isEmpty();
    }

    @Test
    void marcaVencidaYPermiteReprogramarLaFecha() throws Exception {
        cuenta.setFechaVencimiento(hoy.minusDays(1));
        cuentaRepository.saveAndFlush(cuenta);

        mockMvc.perform(get("/api/v1/cuentas-cobrar/vencidas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaCobrar.CUENTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.contenido[?(@.id == %d)].estado".formatted(cuenta.getId())
            ).value(org.hamcrest.Matchers.hasItem("VENCIDO")));

        mockMvc.perform(patch(
                "/api/v1/cuentas-cobrar/{id}/vencimiento",
                cuenta.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaCobrar.CUENTAS_EDITAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaVencimiento\":\""
                    + hoy.plusDays(20) + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fechaVencimiento")
                .value(hoy.plusDays(20).toString()))
            .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void conservaElPagoInicialCreadoPorUnaVentaParcial() throws Exception {
        cuenta.setImportePagado(new BigDecimal("20.00"));
        cuenta.setSaldoPendiente(new BigDecimal("80.00"));
        cuenta.setEstado(EstadoCuentaCobrar.PARCIAL);
        cuentaRepository.saveAndFlush(cuenta);

        PagoCliente inicial = new PagoCliente();
        inicial.setVenta(cuenta.getVenta());
        inicial.setCuentaCobrar(cuenta);
        inicial.setMetodoPago(efectivo);
        inicial.setUsuario(usuario);
        inicial.setMonto(new BigDecimal("20.00"));
        inicial.setReferencia("PAGO-INICIAL-VENTA");
        pagoRepository.saveAndFlush(inicial);

        mockMvc.perform(get("/api/v1/cuentas-cobrar/{id}", cuenta.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaCobrar.CUENTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cuenta.estado").value("PARCIAL"))
            .andExpect(jsonPath("$.cuenta.importePagado").value(20.0))
            .andExpect(jsonPath("$.pagos[0].referencia")
                .value("PAGO-INICIAL-VENTA"));
    }

    @Test
    void protegeEndpointsYRegistraPermisosDeCobranzas() throws Exception {
        mockMvc.perform(get("/api/v1/cuentas-cobrar")
                .header(HttpHeaders.AUTHORIZATION, bearer("CXP_CUENTAS_VER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/cuentas-cobrar/metodos-pago")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCuentaCobrar.PAGOS_CREAR
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.codigo == 'EFECTIVO')]").exists());

        Set<String> esperados = Set.of(
            PermisosCuentaCobrar.CUENTAS_VER,
            PermisosCuentaCobrar.CUENTAS_EDITAR,
            PermisosCuentaCobrar.PAGOS_CREAR
        );
        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Cuentas por cobrar")
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

    private org.springframework.test.web.servlet.ResultActions registrarPago(
        String monto,
        String referencia
    ) throws Exception {
        return mockMvc.perform(post(
                "/api/v1/cuentas-cobrar/{id}/pagos",
                cuenta.getId()
            )
            .header(HttpHeaders.AUTHORIZATION, bearer(
                PermisosCuentaCobrar.PAGOS_CREAR
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idMetodoPago": %d,
                  "monto": %s,
                  "referencia": "%s"
                }
                """.formatted(efectivo.getId(), monto, referencia)));
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
