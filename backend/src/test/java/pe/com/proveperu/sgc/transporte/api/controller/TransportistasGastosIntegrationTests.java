package pe.com.proveperu.sgc.transporte.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.transporte.application.service.PermisosTransporte;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
@Transactional
class TransportistasGastosIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;

    @BeforeEach
    void crearUsuarioResponsable() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();

        usuario = new Usuario();
        usuario.setRol(administrador);
        usuario.setNombreCompleto("Usuario transporte " + sufijo);
        usuario.setUsuarioLogin("transporte-" + sufijo);
        usuario.setPasswordHash("hash-solo-pruebas");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);
    }

    @Test
    void transportistasYGastosRequierenTokenYPermisosEspecificos() throws Exception {
        mockMvc.perform(get("/api/v1/transportistas"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/transportistas")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/transportistas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.TRANSPORTISTAS_VER
                )))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/gastos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_VER)))
            .andExpect(status().isOk());
    }

    @Test
    void registraConsultaYBuscaTransportista() throws Exception {
        String dni = nuevoDni();
        MvcResult creacion = crearTransportista(dni, "Carlos Mendoza")
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.tipoDocumento").value("DNI"))
            .andExpect(jsonPath("$.numeroDocumento").value(dni))
            .andExpect(jsonPath("$.nombreRazonSocial").value("Carlos Mendoza"))
            .andExpect(jsonPath("$.empresaTransporte").value("Transportes del Norte"))
            .andExpect(jsonPath("$.estado").value("ACTIVO"))
            .andReturn();
        Number id = idDe(creacion);

        mockMvc.perform(get("/api/v1/transportistas/{id}", id.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.TRANSPORTISTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numeroDocumento").value(dni));

        mockMvc.perform(get("/api/v1/transportistas")
                .param("buscar", "Mendoza")
                .param("estado", "ACTIVO")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.TRANSPORTISTAS_VER
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(id.longValue()));
    }

    @Test
    void actualizaInactivaYRechazaDocumentoDuplicado() throws Exception {
        String primerDni = nuevoDni();
        String segundoDni = nuevoDni();
        Number primerId = idDe(crearTransportista(primerDni, "Transportista Uno")
            .andExpect(status().isCreated())
            .andReturn());
        crearTransportista(segundoDni, "Transportista Dos")
            .andExpect(status().isCreated());

        crearTransportista(primerDni, "Transportista repetido")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Ya existe un transportista con ese documento"
            ));

        mockMvc.perform(put("/api/v1/transportistas/{id}", primerId.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.TRANSPORTISTAS_EDITAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyTransportista(primerDni, "Transportista Actualizado")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreRazonSocial").value("Transportista Actualizado"));

        mockMvc.perform(patch("/api/v1/transportistas/{id}/estado", primerId.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.TRANSPORTISTAS_ESTADO
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void validaCorrespondenciaEntreTipoYNumeroDeDocumento() throws Exception {
        mockMvc.perform(post("/api/v1/transportistas")
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.TRANSPORTISTAS_CREAR
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoDocumento": "RUC",
                      "numeroDocumento": "12345678",
                      "nombreRazonSocial": " "
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.documentoValido").exists())
            .andExpect(jsonPath("$.errores.nombreRazonSocial").exists());
    }

    @Test
    void registraGastoConTransportistaYUsuarioAutenticado() throws Exception {
        Number idTransportista = idDe(crearTransportista(nuevoDni(), "Transportista gasto")
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(post("/api/v1/gastos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyGasto(idTransportista.longValue(), "TRANSPORTE", "150.50")))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.idCompra").isEmpty())
            .andExpect(jsonPath("$.idTransportista").value(idTransportista.longValue()))
            .andExpect(jsonPath("$.usuarioLogin").value(usuario.getUsuarioLogin()))
            .andExpect(jsonPath("$.tipoGasto").value("TRANSPORTE"))
            .andExpect(jsonPath("$.importe").value(150.5));

        mockMvc.perform(get("/api/v1/gastos")
                .param("idTransportista", idTransportista.toString())
                .param("tipoGasto", "TRANSPORTE")
                .param("desde", "2026-08-01")
                .param("hasta", "2026-08-31")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].importe").value(150.5));

        mockMvc.perform(get("/api/v1/transportistas/{id}/gastos", idTransportista)
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_VER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].usuarioLogin").value(usuario.getUsuarioLogin()));
    }

    @Test
    void exigeTransportistaParaGastoDeTransporteYValidaFechas() throws Exception {
        mockMvc.perform(post("/api/v1/gastos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idTransportista": null,
                      "tipoGasto": "TRANSPORTE",
                      "descripcion": "Flete sin transportista",
                      "importe": 20.00,
                      "fecha": "2026-08-22"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.transportistaRequerido").exists());

        mockMvc.perform(get("/api/v1/gastos")
                .param("desde", "2026-09-01")
                .param("hasta", "2026-08-01")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_VER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(
                "La fecha inicial no puede ser posterior a la fecha final"
            ));
    }

    @Test
    void impideGastoConTransportistaInactivo() throws Exception {
        Number idTransportista = idDe(crearTransportista(
            nuevoDni(),
            "Transportista inactivo"
        ).andExpect(status().isCreated()).andReturn());
        mockMvc.perform(patch("/api/v1/transportistas/{id}/estado", idTransportista)
                .header(HttpHeaders.AUTHORIZATION, bearer(
                    PermisosTransporte.TRANSPORTISTAS_ESTADO
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/gastos")
                .header(HttpHeaders.AUTHORIZATION, bearer(PermisosTransporte.GASTOS_CREAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyGasto(idTransportista.longValue(), "TRANSPORTE", "50.00")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "No se puede registrar un gasto con un transportista inactivo"
            ));
    }

    @Test
    void migracionRegistraYAsignaPermisosDeTransporteAlAdministrador() {
        Set<String> esperados = Set.of(
            PermisosTransporte.TRANSPORTISTAS_VER,
            PermisosTransporte.TRANSPORTISTAS_CREAR,
            PermisosTransporte.TRANSPORTISTAS_EDITAR,
            PermisosTransporte.TRANSPORTISTAS_ESTADO,
            PermisosTransporte.GASTOS_VER,
            PermisosTransporte.GASTOS_CREAR
        );
        Set<String> registrados = permisoRepository.findAllByModuloOrderByCodigoAsc("Transportistas")
            .stream()
            .map(permiso -> permiso.getCodigo())
            .collect(java.util.stream.Collectors.toSet());
        assertThat(registrados).containsExactlyInAnyOrderElementsOf(esperados);

        Rol administrador = rolRepository.findByNombreIgnoreCase("Administrador").orElseThrow();
        Set<String> asignados = rolRepository.findByIdWithPermisos(administrador.getId()).orElseThrow()
            .getPermisos().stream()
            .map(permiso -> permiso.getCodigo())
            .filter(esperados::contains)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(asignados).containsExactlyInAnyOrderElementsOf(esperados);
    }

    private org.springframework.test.web.servlet.ResultActions crearTransportista(
        String dni,
        String nombre
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/transportistas")
            .header(HttpHeaders.AUTHORIZATION, bearer(
                PermisosTransporte.TRANSPORTISTAS_CREAR
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .content(bodyTransportista(dni, nombre)));
    }

    private String bodyTransportista(String dni, String nombre) {
        return """
            {
              "tipoDocumento": "DNI",
              "numeroDocumento": "%s",
              "nombreRazonSocial": "%s",
              "empresaTransporte": "Transportes del Norte",
              "telefono": "987654321",
              "direccion": "Av. Logística 789"
            }
            """.formatted(dni, nombre);
    }

    private String bodyGasto(
        long idTransportista,
        String tipoGasto,
        String importe
    ) {
        return """
            {
              "idTransportista": %d,
              "tipoGasto": "%s",
              "descripcion": "Servicio de traslado de mercadería",
              "importe": %s,
              "fecha": "2026-08-22",
              "numeroComprobante": "F001-123"
            }
            """.formatted(idTransportista, tipoGasto, importe);
    }

    private Number idDe(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }

    private String nuevoDni() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000));
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
