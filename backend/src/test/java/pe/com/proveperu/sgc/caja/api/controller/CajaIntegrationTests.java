package pe.com.proveperu.sgc.caja.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
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
import pe.com.proveperu.sgc.caja.domain.model.EstadoCaja;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.CajaRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
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
class CajaIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private SedeRepository sedeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    private Usuario usuario;
    private Caja caja;
    private MetodoPago efectivo;
    private MetodoPago transferencia;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador")
            .orElseThrow();
        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Cajero " + sufijo);
        usuario.setUsuarioLogin("cajero-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.saveAndFlush(usuario);

        Sede sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow();
        caja = new Caja();
        caja.setSede(sede);
        caja.setNombre("Caja prueba " + sufijo);
        caja.setEstado(EstadoCaja.ACTIVO);
        caja = cajaRepository.saveAndFlush(caja);

        efectivo = metodoPagoRepository.findByCodigoIgnoreCase("EFECTIVO")
            .orElseThrow();
        transferencia = metodoPagoRepository
            .findByCodigoIgnoreCase("TRANSFERENCIA")
            .orElseThrow();
    }

    @Test
    void abreRegistraResumeYCierraCajaConArqueo() throws Exception {
        mockMvc.perform(get("/api/v1/cajas")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCaja.CAJAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == %d)]".formatted(caja.getId())).exists());

        MvcResult apertura = mockMvc.perform(post(
                "/api/v1/cajas/{id}/aperturas",
                caja.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCaja.SESIONES_ABRIR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"saldoInicial": 100.00}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                "/api/v1/cajas/" + caja.getId() + "/sesion-activa"
            ))
            .andExpect(jsonPath("$.estado").value("ABIERTA"))
            .andExpect(jsonPath("$.saldoInicial").value(100.0))
            .andExpect(jsonPath("$.usuarioApertura")
                .value(usuario.getUsuarioLogin()))
            .andReturn();
        long idSesion = ((Number) JsonPath.read(
            apertura.getResponse().getContentAsString(),
            "$.id"
        )).longValue();

        mockMvc.perform(post("/api/v1/cajas/{id}/aperturas", caja.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCaja.SESIONES_ABRIR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"saldoInicial\": 0.00}"))
            .andExpect(status().isConflict());

        registrarMovimiento(
            idSesion,
            "INGRESO",
            "INGRESO_MANUAL",
            efectivo.getId(),
            "50.00",
            "REF-INGRESO"
        ).andExpect(status().isCreated());
        registrarMovimiento(
            idSesion,
            "EGRESO",
            "EGRESO_MANUAL",
            efectivo.getId(),
            "20.00",
            "REF-EGRESO"
        ).andExpect(status().isCreated());
        registrarMovimiento(
            idSesion,
            "INGRESO",
            "INGRESO_MANUAL",
            transferencia.getId(),
            "30.00",
            "REF-TRANSFERENCIA"
        ).andExpect(status().isCreated());

        mockMvc.perform(get(
                "/api/v1/sesiones-caja/{id}/movimientos",
                idSesion
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCaja.MOVIMIENTOS_VER
                ))
                .param("tipo", "INGRESO")
                .param("idMetodoPago", efectivo.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].referencia")
                .value("REF-INGRESO"));

        mockMvc.perform(get("/api/v1/sesiones-caja/{id}/resumen", idSesion)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCaja.RESUMEN_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIngresos").value(80.0))
            .andExpect(jsonPath("$.totalEgresos").value(20.0))
            .andExpect(jsonPath("$.neto").value(60.0))
            .andExpect(jsonPath("$.saldoEsperado").value(130.0))
            .andExpect(jsonPath(
                "$.metodosPago[?(@.codigo == 'EFECTIVO')].neto"
            ).value(30.0));

        mockMvc.perform(post("/api/v1/sesiones-caja/{id}/cierre", idSesion)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCaja.SESIONES_CERRAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "saldoReal": 128.00,
                      "observacion": "Faltante verificado"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CERRADA"))
            .andExpect(jsonPath("$.saldoEsperado").value(130.0))
            .andExpect(jsonPath("$.saldoReal").value(128.0))
            .andExpect(jsonPath("$.diferencia").value(-2.0))
            .andExpect(jsonPath("$.usuarioCierre")
                .value(usuario.getUsuarioLogin()));

        mockMvc.perform(get("/api/v1/cajas/{id}/sesion-activa", caja.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCaja.CAJAS_VER)))
            .andExpect(status().isNotFound());

        registrarMovimiento(
            idSesion,
            "INGRESO",
            "INGRESO_MANUAL",
            efectivo.getId(),
            "1.00",
            "NO-ACEPTAR"
        ).andExpect(status().isConflict());
    }

    @Test
    void validaMovimientosManualesYProtegePermisos() throws Exception {
        mockMvc.perform(get("/api/v1/cajas"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/cajas")
                .header(HttpHeaders.AUTHORIZATION, bearer("VEN_VENTAS_VER")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/cajas/metodos-pago")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosCaja.CAJAS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.codigo == 'EFECTIVO')]").exists())
            .andExpect(jsonPath("$[?(@.codigo == 'TRANSFERENCIA')]").exists());

        long idSesion = abrirCaja();
        registrarMovimiento(
            idSesion,
            "EGRESO",
            "INGRESO_MANUAL",
            efectivo.getId(),
            "10.00",
            "TIPO-INVALIDO"
        ).andExpect(status().isBadRequest());
        registrarMovimiento(
            idSesion,
            "INGRESO",
            "VENTA",
            efectivo.getId(),
            "10.00",
            "ORIGEN-AUTOMATICO"
        ).andExpect(status().isBadRequest());

        Set<String> esperados = Set.of(
            PermisosCaja.CAJAS_VER,
            PermisosCaja.SESIONES_ABRIR,
            PermisosCaja.MOVIMIENTOS_VER,
            PermisosCaja.MOVIMIENTOS_CREAR,
            PermisosCaja.SESIONES_CERRAR,
            PermisosCaja.RESUMEN_VER
        );
        Set<String> registrados = permisoRepository
            .findAllByModuloOrderByCodigoAsc("Caja")
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

    private long abrirCaja() throws Exception {
        MvcResult resultado = mockMvc.perform(post(
                "/api/v1/cajas/{id}/aperturas",
                caja.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosCaja.SESIONES_ABRIR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"saldoInicial\": 0.00}"))
            .andExpect(status().isCreated())
            .andReturn();
        return ((Number) JsonPath.read(
            resultado.getResponse().getContentAsString(),
            "$.id"
        )).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions registrarMovimiento(
        long idSesion,
        String tipo,
        String concepto,
        Long idMetodoPago,
        String importe,
        String referencia
    ) throws Exception {
        return mockMvc.perform(post(
                "/api/v1/sesiones-caja/{id}/movimientos",
                idSesion
            )
            .header(HttpHeaders.AUTHORIZATION, bearer(
                PermisosCaja.MOVIMIENTOS_CREAR
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tipo": "%s",
                  "concepto": "%s",
                  "idMetodoPago": %d,
                  "importe": %s,
                  "referencia": "%s"
                }
                """.formatted(
                    tipo,
                    concepto,
                    idMetodoPago,
                    importe,
                    referencia
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
