package pe.com.proveperu.sgc.inventario.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
import pe.com.proveperu.sgc.inventario.application.service.PermisosInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
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
class InventarioIntegrationTests {

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
    private SedeRepository sedeRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Sede sede;
    private Producto producto;
    private UnidadMedida unidadBase;
    private UnidadMedida unidadAlterna;
    private Usuario usuario;

    @BeforeEach
    void crearDatosBase() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO").orElseThrow();

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría inventario " + sufijo);
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        categoria = categoriaRepository.save(categoria);

        unidadBase = crearUnidad("G" + sufijo, "Gramo " + sufijo, true);
        unidadAlterna = crearUnidad("K" + sufijo, "Kilogramo " + sufijo, true);

        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadBase(unidadBase);
        producto.setCodigoInterno(("INV-" + sufijo).toUpperCase());
        producto.setNombre("Producto inventario " + sufijo);
        producto.setStockMinimo(new BigDecimal("5.000"));
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario inventario " + sufijo);
        usuario.setUsuarioLogin(("inventario-" + sufijo).toLowerCase());
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);
    }

    @Test
    void inventarioRequiereTokenYPermisoEspecifico() throws Exception {
        mockMvc.perform(get("/api/v1/inventario"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/inventario")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/inventario")
                .param("buscar", producto.getCodigoInterno())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.STOCK_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listaSedesActivasConPermisoDeConsultaDeStock() throws Exception {
        mockMvc.perform(get("/api/v1/sedes"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/sedes")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.STOCK_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(sede.getId()))
            .andExpect(jsonPath("$[0].nombre").value(sede.getNombre()))
            .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
    }

    @Test
    void consultaStockInicialYDetectaProductoAgotado() throws Exception {
        mockMvc.perform(get("/api/v1/inventario/{idProducto}", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.STOCK_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idSede").value(sede.getId()))
            .andExpect(jsonPath("$.stockFisico").value(0.0))
            .andExpect(jsonPath("$.stockReservado").value(0.0))
            .andExpect(jsonPath("$.stockDisponible").value(0.0))
            .andExpect(jsonPath("$.estadoStock").value("AGOTADO"));

        mockMvc.perform(get("/api/v1/inventario/stock-bajo")
                .param("buscar", producto.getCodigoInterno())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.STOCK_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].idProducto").value(producto.getId()))
            .andExpect(jsonPath("$.contenido[0].estadoStock").value("AGOTADO"));
    }

    @Test
    void registraEntradaYSalidaConTrazabilidad() throws Exception {
        ajustar("ENTRADA", "10.000", unidadBase.getId(), "Conteo físico inicial")
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.movimiento.tipoMovimiento").value("AJUSTE_ENTRADA"))
            .andExpect(jsonPath("$.movimiento.cantidadBase").value(10.0))
            .andExpect(jsonPath("$.movimiento.usuarioLogin").value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.inventario.stockFisico").value(10.0))
            .andExpect(jsonPath("$.inventario.estadoStock").value("NORMAL"));

        ajustar("SALIDA", "4.000", unidadBase.getId(), "Merma verificada")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.movimiento.tipoMovimiento").value("AJUSTE_SALIDA"))
            .andExpect(jsonPath("$.movimiento.cantidadBase").value(-4.0))
            .andExpect(jsonPath("$.inventario.stockFisico").value(6.0));

        Inventario inventario = inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElseThrow();
        assertThat(inventario.getStockFisico()).isEqualByComparingTo("6.000");
        assertThat(movimientoRepository.count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void convierteLaUnidadAlternaAntesDeAfectarStock() throws Exception {
        ProductoUnidadConversion conversion = new ProductoUnidadConversion();
        conversion.setProducto(producto);
        conversion.setUnidadOrigen(unidadAlterna);
        conversion.setUnidadDestino(unidadBase);
        conversion.setFactorConversion(new BigDecimal("1000.000000"));
        conversion.setEstado(EstadoCatalogo.ACTIVO);
        conversionRepository.save(conversion);

        ajustar("ENTRADA", "1.500", unidadAlterna.getId(), "Ingreso por pesaje")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.movimiento.cantidad").value(1.5))
            .andExpect(jsonPath("$.movimiento.cantidadBase").value(1500.0))
            .andExpect(jsonPath("$.movimiento.codigoUnidadBase").value(unidadBase.getCodigo()))
            .andExpect(jsonPath("$.inventario.stockFisico").value(1500.0));
    }

    @Test
    void bloqueaSalidaSuperiorAlStockDisponible() throws Exception {
        ajustar("ENTRADA", "10.000", unidadBase.getId(), "Carga inicial")
            .andExpect(status().isCreated());

        Inventario inventario = inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElseThrow();
        inventario.setStockReservado(new BigDecimal("8.000"));
        inventarioRepository.save(inventario);

        ajustar("SALIDA", "3.000", unidadBase.getId(), "Salida no permitida")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Regla de negocio"))
            .andExpect(jsonPath("$.detail").value("Stock insuficiente. Disponible: 2.000"));

        assertThat(inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElseThrow()
            .getStockFisico()).isEqualByComparingTo("10.000");
    }

    @Test
    void validaMotivoYConversionConfigurada() throws Exception {
        mockMvc.perform(post("/api/v1/inventario/ajustes")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.AJUSTES_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idProducto": %d,
                      "idUnidadMedida": %d,
                      "tipoAjuste": "ENTRADA",
                      "cantidad": 1.000,
                      "motivo": " "
                    }
                    """.formatted(producto.getId(), unidadBase.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.motivo").exists());

        ajustar("ENTRADA", "1.000", unidadAlterna.getId(), "Unidad sin equivalencia")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "No existe una conversión activa entre la unidad indicada y la unidad base"
            ));
    }

    @Test
    void consultaMovimientosFiltradosYKardexCronologico() throws Exception {
        ajustar("ENTRADA", "5.000", unidadBase.getId(), "Entrada para Kardex")
            .andExpect(status().isCreated());
        ajustar("SALIDA", "2.000", unidadBase.getId(), "Salida para Kardex")
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inventario/movimientos")
                .param("idProducto", producto.getId().toString())
                .param("tipo", "AJUSTE_SALIDA")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.MOVIMIENTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].tipoMovimiento").value("AJUSTE_SALIDA"))
            .andExpect(jsonPath("$.contenido[0].stockResultante").value(3.0));

        mockMvc.perform(get("/api/v1/inventario/movimientos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.MOVIMIENTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2));

        mockMvc.perform(get("/api/v1/kardex/{idProducto}", producto.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.KARDEX_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2))
            .andExpect(jsonPath("$.contenido[0].tipoMovimiento").value("AJUSTE_ENTRADA"))
            .andExpect(jsonPath("$.contenido[0].stockResultante").value(5.0))
            .andExpect(jsonPath("$.contenido[1].tipoMovimiento").value("AJUSTE_SALIDA"))
            .andExpect(jsonPath("$.contenido[1].stockResultante").value(3.0));
    }

    @Test
    void migracionAsignaPermisosDeInventarioAlAdministrador() {
        Set<String> esperados = Set.of(
            PermisosInventario.STOCK_VER,
            PermisosInventario.AJUSTES_CREAR,
            PermisosInventario.MOVIMIENTOS_VER,
            PermisosInventario.KARDEX_VER
        );
        Set<String> registrados = permisoRepository.findAllByModuloOrderByCodigoAsc("Inventario")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(java.util.stream.Collectors.toSet());
        assertThat(registrados).containsExactlyInAnyOrderElementsOf(esperados);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        Rol rolConPermisos = rolRepository.findByIdWithPermisos(administrador.getId()).orElseThrow();
        Set<String> asignados = rolConPermisos.getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .filter(esperados::contains)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(asignados).containsExactlyInAnyOrderElementsOf(esperados);
    }

    private UnidadMedida crearUnidad(String codigo, String nombre, boolean permiteDecimales) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setCodigo(codigo.toUpperCase());
        unidad.setNombre(nombre);
        unidad.setPermiteDecimales(permiteDecimales);
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        return unidadMedidaRepository.save(unidad);
    }

    private org.springframework.test.web.servlet.ResultActions ajustar(
        String tipo,
        String cantidad,
        Long idUnidad,
        String motivo
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/inventario/ajustes")
            .header(HttpHeaders.AUTHORIZATION, bearer(PermisosInventario.AJUSTES_CREAR))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idSede": %d,
                  "idProducto": %d,
                  "idUnidadMedida": %d,
                  "tipoAjuste": "%s",
                  "cantidad": %s,
                  "motivo": "%s"
                }
                """.formatted(
                    sede.getId(),
                    producto.getId(),
                    idUnidad,
                    tipo,
                    cantidad,
                    motivo
                )));
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
