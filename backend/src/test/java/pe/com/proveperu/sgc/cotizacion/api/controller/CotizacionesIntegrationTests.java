package pe.com.proveperu.sgc.cotizacion.api.controller;

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
import java.sql.Date;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
import pe.com.proveperu.sgc.cliente.domain.model.ClientePrecioEspecial;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClientePrecioEspecialRepository;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.cotizacion.application.service.PermisosCotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.Cotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;
import pe.com.proveperu.sgc.cotizacion.infrastructure.persistence.CotizacionRepository;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
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
class CotizacionesIntegrationTests {

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
    private ClientePrecioEspecialRepository precioEspecialRepository;

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
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        categoria.setNombre("Categoría cotización " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        unidadBase = nuevaUnidad("UC" + sufijo, "Unidad cotización " + sufijo, true);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidadBase);
        producto.setCodigoInterno("COT-" + sufijo);
        producto.setNombre("Producto cotización " + sufijo);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        PrecioProducto precio = new PrecioProducto();
        precio.setProducto(producto);
        precio.setTipoPrecio("MINORISTA");
        precio.setMonto(new BigDecimal("100.00"));
        precio.setVigenteDesde(hoy.minusDays(10));
        precio.setEstado(EstadoCatalogo.ACTIVO);
        precioRepository.save(precio);

        cliente = new Cliente();
        cliente.setTipoPersona(TipoPersona.NATURAL);
        cliente.setTipoDocumento(TipoDocumentoCliente.DNI);
        cliente.setNumeroDocumento(nuevoDni());
        cliente.setNombres("Cliente");
        cliente.setApellidos("Cotización " + sufijo);
        cliente.setEstado(EstadoCatalogo.ACTIVO);
        cliente = clienteRepository.save(cliente);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario cotización " + sufijo);
        usuario.setUsuarioLogin("cot-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
        inventario = inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElseGet(Inventario::new);
        inventario.setSede(sede);
        inventario.setProducto(producto);
        inventario.setStockFisico(new BigDecimal("10.000"));
        inventario.setStockReservado(BigDecimal.ZERO.setScale(3));
        inventario = inventarioRepository.save(inventario);
    }

    @Test
    void creaCotizacionCalculaImportesEInformaDisponibilidadSinReservarStock()
        throws Exception {
        MvcResult resultado = crearCotizacion(
            unidadBase.getId(),
            "2.000",
            "10.00",
            "34.20",
            PermisosCotizacion.COTIZACIONES_CREAR,
            PermisosCotizacion.DESCUENTOS_APLICAR
        )
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.cotizacion.idCliente").value(cliente.getId()))
            .andExpect(jsonPath("$.cotizacion.usuarioLogin").value(
                usuario.getUsuarioLogin()
            ))
            .andExpect(jsonPath("$.cotizacion.subtotal").value(190.0))
            .andExpect(jsonPath("$.cotizacion.igv").value(34.2))
            .andExpect(jsonPath("$.cotizacion.total").value(224.2))
            .andExpect(jsonPath("$.cotizacion.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.todosDisponibles").value(true))
            .andExpect(jsonPath("$.detalles[0].precioUnitario").value(100.0))
            .andExpect(jsonPath("$.detalles[0].descuento").value(10.0))
            .andExpect(jsonPath("$.detalles[0].cantidadBase").value(2.0))
            .andExpect(jsonPath("$.detalles[0].stockDisponible").value(10.0))
            .andReturn();
        long id = idCotizacion(resultado);

        Inventario sinCambios = inventarioRepository.findById(inventario.getId())
            .orElseThrow();
        assertThat(sinCambios.getStockFisico()).isEqualByComparingTo("10.000");
        assertThat(sinCambios.getStockReservado()).isEqualByComparingTo("0.000");

        mockMvc.perform(get("/api/v1/cotizaciones/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCotizacion.COTIZACIONES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cotizacion.id").value(id))
            .andExpect(jsonPath("$.idSedeConsulta").value(sede.getId()));
    }

    @Test
    void priorizaPrecioEspecialYConviertePrecioYStockALaUnidadCotizada()
        throws Exception {
        ClientePrecioEspecial especial = new ClientePrecioEspecial();
        especial.setCliente(cliente);
        especial.setProducto(producto);
        especial.setPrecio(new BigDecimal("80.00"));
        especial.setVigenteDesde(hoy.minusDays(1));
        especial.setEstado(EstadoCatalogo.ACTIVO);
        precioEspecialRepository.save(especial);

        UnidadMedida caja = nuevaUnidad(
            "CJ" + UUID.randomUUID().toString().substring(0, 8),
            "Caja cotización",
            false
        );
        ProductoUnidadConversion conversion = new ProductoUnidadConversion();
        conversion.setProducto(producto);
        conversion.setUnidadOrigen(caja);
        conversion.setUnidadDestino(unidadBase);
        conversion.setFactorConversion(new BigDecimal("5.000000"));
        conversion.setEstado(EstadoCatalogo.ACTIVO);
        conversionRepository.save(conversion);

        crearCotizacion(
            caja.getId(),
            "2.000",
            "0.00",
            "0.00",
            PermisosCotizacion.COTIZACIONES_CREAR
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cotizacion.subtotal").value(800.0))
            .andExpect(jsonPath("$.detalles[0].precioUnitario").value(400.0))
            .andExpect(jsonPath("$.detalles[0].cantidadBase").value(10.0))
            .andExpect(jsonPath("$.detalles[0].stockDisponibleBase").value(10.0))
            .andExpect(jsonPath("$.detalles[0].stockDisponible").value(2.0))
            .andExpect(jsonPath("$.detalles[0].disponible").value(true));
    }

    @Test
    void permiteCotizarSinStockPeroAdvierteLaFaltaDeDisponibilidad()
        throws Exception {
        crearCotizacion(
            unidadBase.getId(),
            "11.000",
            "0.00",
            "0.00",
            PermisosCotizacion.COTIZACIONES_CREAR
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.todosDisponibles").value(false))
            .andExpect(jsonPath("$.detalles[0].disponible").value(false))
            .andExpect(jsonPath("$.detalles[0].stockDisponible").value(10.0));
        assertThat(inventarioRepository.findById(inventario.getId()).orElseThrow()
            .getStockReservado()).isEqualByComparingTo("0.000");
    }

    @Test
    void rechazaDescuentoSinPermisoYNoGuardaLaCotizacion() throws Exception {
        long antes = cotizacionRepository.count();

        crearCotizacion(
            unidadBase.getId(),
            "2.000",
            "10.00",
            "0.00",
            PermisosCotizacion.COTIZACIONES_CREAR
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "No tiene permiso para aplicar descuentos en cotizaciones"
            ));

        assertThat(cotizacionRepository.count()).isEqualTo(antes);
    }

    @Test
    void editaCotizacionPendienteYAceptadaYaNoPermiteCambios() throws Exception {
        long id = crearCotizacionBase();

        mockMvc.perform(put("/api/v1/cotizaciones/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCotizacion.COTIZACIONES_EDITAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyCotizacion(unidadBase.getId(), "3.000", "0.00", "0.00")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cotizacion.subtotal").value(300.0))
            .andExpect(jsonPath("$.detalles.length()").value(1));

        mockMvc.perform(patch("/api/v1/cotizaciones/{id}/estado", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCotizacion.COTIZACIONES_ESTADO
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ACEPTADA\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cotizacion.estado").value("ACEPTADA"));

        mockMvc.perform(put("/api/v1/cotizaciones/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCotizacion.COTIZACIONES_EDITAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyCotizacion(unidadBase.getId(), "4.000", "0.00", "0.00")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Solo se puede editar una cotización PENDIENTE y vigente"
            ));
    }

    @Test
    void identificaCotizacionVencidaAutomaticamente() throws Exception {
        long id = crearCotizacionBase();
        jdbcTemplate.update(
            "UPDATE cotizacion SET fecha = ?, fecha_vencimiento = ? WHERE id_cotizacion = ?",
            Date.valueOf(hoy.minusDays(10)),
            Date.valueOf(hoy.minusDays(1)),
            id
        );

        mockMvc.perform(get("/api/v1/cotizaciones/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCotizacion.COTIZACIONES_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cotizacion.estado").value("VENCIDA"));

        Cotizacion vencida = cotizacionRepository.findById(id).orElseThrow();
        assertThat(vencida.getEstado()).isEqualTo(EstadoCotizacion.VENCIDA);
    }

    @Test
    void protegeEndpointsYRegistraPermisosEnLaMigracion() throws Exception {
        mockMvc.perform(get("/api/v1/cotizaciones")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCompra.COMPRAS_VER
                )))
            .andExpect(status().isForbidden());

        Set<String> esperados = Set.of(
            PermisosCotizacion.COTIZACIONES_VER,
            PermisosCotizacion.COTIZACIONES_CREAR,
            PermisosCotizacion.COTIZACIONES_EDITAR,
            PermisosCotizacion.COTIZACIONES_ESTADO,
            PermisosCotizacion.DESCUENTOS_APLICAR
        );
        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Cotizaciones")
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

    private long crearCotizacionBase() throws Exception {
        MvcResult resultado = crearCotizacion(
            unidadBase.getId(),
            "2.000",
            "0.00",
            "0.00",
            PermisosCotizacion.COTIZACIONES_CREAR
        )
            .andExpect(status().isCreated())
            .andReturn();
        return idCotizacion(resultado);
    }

    private org.springframework.test.web.servlet.ResultActions crearCotizacion(
        Long idUnidad,
        String cantidad,
        String descuento,
        String igv,
        String... authorities
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/cotizaciones")
            .header(HttpHeaders.AUTHORIZATION, bearer(authorities))
            .contentType(MediaType.APPLICATION_JSON)
            .content(bodyCotizacion(idUnidad, cantidad, descuento, igv)));
    }

    private String bodyCotizacion(
        Long idUnidad,
        String cantidad,
        String descuento,
        String igv
    ) {
        return """
            {
              "idCliente": %d,
              "fecha": "%s",
              "fechaVencimiento": "%s",
              "igv": %s,
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
                hoy,
                hoy.plusDays(7),
                igv,
                producto.getId(),
                idUnidad,
                cantidad,
                descuento
            );
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

    private long idCotizacion(MvcResult resultado) throws Exception {
        return ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.cotizacion.id"
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
