package pe.com.proveperu.sgc.cliente.infrastructure.documento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.com.proveperu.sgc.cliente.application.dto.DatosDocumentoConsultado;
import pe.com.proveperu.sgc.cliente.application.port.ConsultaDocumentoGateway;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cliente.infrastructure.config.ConsultaDocumentoProperties;

@Component
public class DecolectaConsultaDocumentoClient implements ConsultaDocumentoGateway {

    private final ConsultaDocumentoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConsultaDocumentoMetricas metricas;
    private final Map<ClaveCache, EntradaCache> cache = new ConcurrentHashMap<>();
    private final AtomicInteger fallosConsecutivos = new AtomicInteger();
    private volatile Instant circuitoAbiertoHasta = Instant.EPOCH;

    @Autowired
    public DecolectaConsultaDocumentoClient(
        ConsultaDocumentoProperties properties,
        MeterRegistry meterRegistry
    ) {
        this(properties, JsonMapper.builder().build(), HttpClient.newBuilder()
            .connectTimeout(properties.getConnectTimeout())
            .build(), new ConsultaDocumentoMetricas(meterRegistry));
    }

    DecolectaConsultaDocumentoClient(
        ConsultaDocumentoProperties properties,
        ObjectMapper objectMapper,
        HttpClient httpClient,
        ConsultaDocumentoMetricas metricas
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.metricas = metricas;
    }

    @Override
    public boolean disponible(TipoDocumentoCliente tipoDocumento) {
        return properties.disponible(tipoDocumento);
    }

    @Override
    public Optional<DatosDocumentoConsultado> consultar(
        TipoDocumentoCliente tipoDocumento,
        String numeroDocumento
    ) {
        if (!disponible(tipoDocumento)) {
            return Optional.empty();
        }
        ClaveCache claveCache = new ClaveCache(tipoDocumento, numeroDocumento);
        EntradaCache entradaCache = cache.get(claveCache);
        if (entradaCache != null && Instant.now().isBefore(entradaCache.expira())) {
            metricas.registrarCache(true, entradaCache.datos().isPresent());
            return entradaCache.datos();
        }
        if (entradaCache != null) {
            cache.remove(claveCache, entradaCache);
        }
        metricas.registrarCache(false, false);
        verificarCircuito();

        String ruta = tipoDocumento == TipoDocumentoCliente.RUC
            ? "/sunat/ruc"
            : "/reniec/dni";
        URI uri = URI.create(quitarBarraFinal(properties.getBaseUrl().toString())
            + ruta
            + "?numero="
            + numeroDocumento);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(properties.getReadTimeout())
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + properties.getToken().strip())
            .header("User-Agent", "SGC-PROVEPERU/1.0")
            .GET()
            .build();

        int maximoIntentos = Math.max(1, properties.getRetryMaxAttempts());
        for (int intento = 1; intento <= maximoIntentos; intento++) {
            long inicio = System.nanoTime();
            HttpResponse<String> response;
            try {
                response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                metricas.registrarProveedor("interrumpida", transcurrido(inicio));
                throw new IntegracionDocumentoException(
                    "La consulta del documento fue interrumpida",
                    exception
                );
            } catch (IOException exception) {
                metricas.registrarProveedor("error_red", transcurrido(inicio));
                if (intento < maximoIntentos) {
                    esperarReintento("error_red", intento);
                    continue;
                }
                registrarFalloTransitorio();
                throw new IntegracionDocumentoException(
                    "No se pudo comunicar con el proveedor de documentos",
                    exception
                );
            }

            int estadoHttp = response.statusCode();
            metricas.registrarProveedor("http_" + estadoHttp, transcurrido(inicio));
            if (estadoHttp == 404) {
                registrarExito();
                Optional<DatosDocumentoConsultado> vacio = Optional.empty();
                guardarCache(claveCache, vacio, properties.getNegativeCacheTtl());
                return vacio;
            }
            if (estadoHttp == 401 || estadoHttp == 403) {
                throw new IntegracionDocumentoException(
                    "El proveedor rechazó las credenciales de consulta; verifica DOCUMENT_LOOKUP_TOKEN"
                );
            }
            if (estadoHttp == 429) {
                throw new IntegracionDocumentoException(
                    "Se alcanzó temporalmente el límite de consultas del proveedor"
                );
            }
            if (esTransitorio(estadoHttp) && intento < maximoIntentos) {
                esperarReintento("http_" + estadoHttp, intento);
                continue;
            }
            if (estadoHttp < 200 || estadoHttp >= 300) {
                if (esTransitorio(estadoHttp)) {
                    registrarFalloTransitorio();
                }
                throw new IntegracionDocumentoException(
                    "El proveedor de documentos respondió con HTTP " + estadoHttp
                );
            }

            try {
                DatosDocumentoConsultado datos = parsear(
                    tipoDocumento,
                    numeroDocumento,
                    response.body()
                );
                registrarExito();
                Optional<DatosDocumentoConsultado> resultado = Optional.of(datos);
                guardarCache(claveCache, resultado, properties.getCacheTtl());
                return resultado;
            } catch (IntegracionDocumentoException exception) {
                registrarFalloTransitorio();
                throw exception;
            }
        }
        throw new IntegracionDocumentoException("No se pudo completar la consulta del documento");
    }

    private void verificarCircuito() {
        Instant ahora = Instant.now();
        if (ahora.isBefore(circuitoAbiertoHasta)) {
            metricas.registrarCircuitoAbierto();
            throw new IntegracionDocumentoException(
                "El proveedor de documentos está temporalmente protegido por fallos consecutivos; inténtalo nuevamente en unos segundos"
            );
        }
        if (!Instant.EPOCH.equals(circuitoAbiertoHasta)) {
            circuitoAbiertoHasta = Instant.EPOCH;
        }
    }

    private void registrarExito() {
        fallosConsecutivos.set(0);
        circuitoAbiertoHasta = Instant.EPOCH;
    }

    private void registrarFalloTransitorio() {
        int umbral = Math.max(1, properties.getCircuitBreakerFailures());
        if (fallosConsecutivos.incrementAndGet() >= umbral) {
            circuitoAbiertoHasta = Instant.now().plus(properties.getCircuitBreakerOpenDuration());
            fallosConsecutivos.set(0);
        }
    }

    private void esperarReintento(String motivo, int intento) {
        metricas.registrarReintento(motivo);
        Duration espera = properties.getRetryDelay().multipliedBy(intento);
        if (espera.isZero() || espera.isNegative()) {
            return;
        }
        try {
            Thread.sleep(espera.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IntegracionDocumentoException(
                "La consulta del documento fue interrumpida durante el reintento",
                exception
            );
        }
    }

    private boolean esTransitorio(int estadoHttp) {
        return estadoHttp == 408 || estadoHttp >= 500;
    }

    private Duration transcurrido(long inicioNanos) {
        return Duration.ofNanos(System.nanoTime() - inicioNanos);
    }

    private void guardarCache(
        ClaveCache clave,
        Optional<DatosDocumentoConsultado> datos,
        Duration vigencia
    ) {
        if (properties.getCacheMaxEntries() <= 0 || vigencia.isZero() || vigencia.isNegative()) {
            return;
        }
        Instant ahora = Instant.now();
        if (cache.size() >= properties.getCacheMaxEntries()) {
            cache.entrySet().removeIf(entry -> !ahora.isBefore(entry.getValue().expira()));
        }
        if (cache.size() >= properties.getCacheMaxEntries()) {
            cache.keySet().stream().findFirst().ifPresent(cache::remove);
        }
        cache.put(clave, new EntradaCache(datos, ahora.plus(vigencia)));
    }

    DatosDocumentoConsultado parsear(
        TipoDocumentoCliente tipoDocumento,
        String numeroDocumento,
        String json
    ) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (tipoDocumento == TipoDocumentoCliente.RUC) {
                String razonSocial = primerTexto(root, "razon_social", "razonSocial", "nombre");
                if (razonSocial == null) {
                    throw new IntegracionDocumentoException(
                        "La respuesta del proveedor no contiene la razón social"
                    );
                }
                return new DatosDocumentoConsultado(
                    TipoPersona.JURIDICA,
                    tipoDocumento,
                    numeroDocumento,
                    null,
                    null,
                    razonSocial,
                    null,
                    texto(root, "direccion"),
                    texto(root, "estado"),
                    texto(root, "condicion")
                );
            }
            String nombres = primerTexto(root, "first_name", "nombres");
            String apellidos = unir(
                primerTexto(root, "first_last_name", "apellidoPaterno"),
                primerTexto(root, "second_last_name", "apellidoMaterno")
            );
            if (nombres == null || apellidos == null) {
                throw new IntegracionDocumentoException(
                    "La respuesta del proveedor no contiene nombres y apellidos completos"
                );
            }
            return new DatosDocumentoConsultado(
                TipoPersona.NATURAL,
                tipoDocumento,
                numeroDocumento,
                nombres,
                apellidos,
                null,
                null,
                texto(root, "direccion"),
                texto(root, "estado"),
                texto(root, "condicion")
            );
        } catch (IntegracionDocumentoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IntegracionDocumentoException(
                "No se pudo interpretar la respuesta del proveedor de documentos",
                exception
            );
        }
    }

    private String primerTexto(JsonNode root, String... campos) {
        for (String campo : campos) {
            String valor = texto(root, campo);
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    private String texto(JsonNode root, String campo) {
        JsonNode node = root.get(campo);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip();
    }

    private String unir(String primero, String segundo) {
        String unido = ((primero == null ? "" : primero) + " " + (segundo == null ? "" : segundo))
            .strip();
        return unido.isEmpty() ? null : unido;
    }

    private String quitarBarraFinal(String valor) {
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    private record ClaveCache(TipoDocumentoCliente tipoDocumento, String numeroDocumento) {
    }

    private record EntradaCache(
        Optional<DatosDocumentoConsultado> datos,
        Instant expira
    ) {
    }
}
