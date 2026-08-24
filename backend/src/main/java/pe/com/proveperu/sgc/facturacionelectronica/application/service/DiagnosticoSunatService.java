package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.DiagnosticoSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.DiagnosticoSunatResponse.Certificado;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.DiagnosticoSunatResponse.EstadoVerificacion;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.DiagnosticoSunatResponse.Serie;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.DiagnosticoSunatResponse.Verificacion;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.DiagnosticoSunatRepository;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.security.CertificadoDigitalProvider;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.SunatEndpointProbe;

@Service
@RequiredArgsConstructor
public class DiagnosticoSunatService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Set<String> SERIES_REQUERIDAS = Set.of(
        "BOLETA",
        "FACTURA",
        "NOTA_CREDITO_BOLETA",
        "NOTA_DEBITO_BOLETA",
        "NOTA_CREDITO_FACTURA",
        "NOTA_DEBITO_FACTURA"
    );
    private static final String ENDPOINT_PRODUCCION = "e-factura.sunat.gob.pe";

    private final SunatProperties properties;
    private final CertificadoDigitalProvider certificadoProvider;
    private final EmpresaRepository empresaRepository;
    private final DiagnosticoSunatRepository diagnosticoRepository;
    private final SunatEndpointProbe endpointProbe;

    public DiagnosticoSunatResponse diagnosticarProduccion() {
        List<Verificacion> verificaciones = new ArrayList<>();
        verificarConfiguracion(verificaciones);

        Empresa empresa = verificarEmpresa(verificaciones);
        Certificado certificado = verificarCertificado(verificaciones, empresa);
        List<Serie> series = verificarBaseDatosYSeries(verificaciones);
        verificarConectividad(verificaciones);
        verificarPendientes(verificaciones);

        int aprobados = contar(verificaciones, EstadoVerificacion.APROBADO);
        int advertencias = contar(verificaciones, EstadoVerificacion.ADVERTENCIA);
        int bloqueos = contar(verificaciones, EstadoVerificacion.BLOQUEO);
        return new DiagnosticoSunatResponse(
            Instant.now(),
            properties.getAmbiente(),
            bloqueos == 0,
            properties.isEnabled()
                && properties.getAmbiente() == AmbienteSunat.PRODUCCION
                && properties.isProductionEnabled(),
            aprobados,
            advertencias,
            bloqueos,
            List.copyOf(verificaciones),
            certificado,
            series
        );
    }

    private void verificarConfiguracion(List<Verificacion> verificaciones) {
        agregar(
            verificaciones,
            "INTEGRACION",
            "Integración SUNAT",
            properties.isEnabled() ? EstadoVerificacion.APROBADO : EstadoVerificacion.BLOQUEO,
            properties.isEnabled()
                ? "La integración electrónica está habilitada."
                : "SUNAT_ENABLED permanece desactivado.",
            properties.isEnabled() ? null : "Configura SUNAT_ENABLED=true."
        );
        agregar(
            verificaciones,
            "AMBIENTE",
            "Ambiente seleccionado",
            properties.getAmbiente() == AmbienteSunat.PRODUCCION
                ? EstadoVerificacion.APROBADO
                : EstadoVerificacion.BLOQUEO,
            "El backend está configurado para " + properties.getAmbiente() + ".",
            properties.getAmbiente() == AmbienteSunat.PRODUCCION
                ? null
                : "Para el piloto cambia SUNAT_AMBIENTE=PRODUCCION y reinicia el sistema."
        );
        agregar(
            verificaciones,
            "PROTECCION",
            "Protección de correlativos reales",
            properties.isProductionEnabled()
                ? EstadoVerificacion.ADVERTENCIA
                : EstadoVerificacion.APROBADO,
            properties.isProductionEnabled()
                ? "La emisión tributaria real está desbloqueada."
                : "La emisión real continúa bloqueada y el diagnóstico no consume correlativos.",
            properties.isProductionEnabled()
                ? "Desactiva SUNAT_PRODUCTION_ENABLED al terminar el piloto."
                : "Actívala únicamente al registrar una venta real supervisada."
        );
        boolean credenciales = configurada(properties.getUsuarioSol())
            && configurada(properties.getClaveSol())
            && !"MODDATOS".equalsIgnoreCase(properties.getUsuarioSol().strip())
            && !"MODDATOS".equalsIgnoreCase(properties.getClaveSol().strip());
        agregar(
            verificaciones,
            "CREDENCIALES",
            "Credenciales SOL",
            credenciales ? EstadoVerificacion.APROBADO : EstadoVerificacion.BLOQUEO,
            credenciales
                ? "El usuario secundario y su clave están configurados sin exponerlos."
                : "Las credenciales de producción no están completas o conservan valores de prueba.",
            credenciales ? null : "Configura SUNAT_SOL_USER y SUNAT_SOL_PASSWORD."
        );
        agregar(
            verificaciones,
            "RESUMEN_DIARIO",
            "Automatización del Resumen Diario",
            properties.isResumenDiarioAutomaticoEnabled()
                ? EstadoVerificacion.APROBADO
                : EstadoVerificacion.ADVERTENCIA,
            properties.isResumenDiarioAutomaticoEnabled()
                ? "La preparación automática de resúmenes está habilitada."
                : "El primer piloto podrá gestionarse manualmente desde Resúmenes SUNAT.",
            properties.isResumenDiarioAutomaticoEnabled()
                ? null
                : "Habilita la automatización después de validar el primer resumen real."
        );
    }

    private Empresa verificarEmpresa(List<Verificacion> verificaciones) {
        List<Empresa> empresas = empresaRepository.findAll();
        Empresa empresa = empresas.stream()
            .filter(item -> "ACTIVO".equalsIgnoreCase(item.getEstado()))
            .findFirst()
            .orElse(empresas.isEmpty() ? null : empresas.getFirst());
        boolean completa = empresa != null
            && empresa.getRuc() != null && empresa.getRuc().matches("\\d{11}")
            && configurada(empresa.getRazonSocial())
            && configurada(empresa.getDireccion())
            && empresa.getUbigeo() != null && empresa.getUbigeo().matches("\\d{6}")
            && configurada(empresa.getDepartamento())
            && configurada(empresa.getProvincia())
            && configurada(empresa.getDistrito())
            && "PE".equalsIgnoreCase(empresa.getCodigoPais())
            && "ACTIVO".equalsIgnoreCase(empresa.getEstado());
        agregar(
            verificaciones,
            "EMPRESA",
            "Datos fiscales del emisor",
            completa ? EstadoVerificacion.APROBADO : EstadoVerificacion.BLOQUEO,
            completa
                ? "RUC, razón social, domicilio fiscal y ubigeo están completos."
                : "Faltan datos fiscales obligatorios para construir el XML UBL.",
            completa ? null : "Completa la ficha de empresa antes de emitir."
        );
        return empresa;
    }

    private Certificado verificarCertificado(
        List<Verificacion> verificaciones,
        Empresa empresa
    ) {
        try {
            var credencial = certificadoProvider.cargar();
            X509Certificate certificate = credencial.certificate();
            String subject = certificate.getSubjectX500Principal().getName(
                X500Principal.RFC2253
            );
            boolean rucCoincide = empresa != null
                && subject.replace(" ", "").contains(empresa.getRuc());
            agregar(
                verificaciones,
                "CERTIFICADO",
                "Certificado digital",
                EstadoVerificacion.APROBADO,
                "El archivo PKCS#12 está vigente y contiene una clave privada utilizable.",
                null
            );
            agregar(
                verificaciones,
                "CERTIFICADO_RUC",
                "Identidad del certificado",
                rucCoincide ? EstadoVerificacion.APROBADO : EstadoVerificacion.ADVERTENCIA,
                rucCoincide
                    ? "El RUC del emisor aparece en la identidad del certificado."
                    : "El RUC no pudo confirmarse automáticamente desde el nombre del certificado.",
                rucCoincide
                    ? null
                    : "Contrasta manualmente el titular mostrado con el certificado registrado en SOL."
            );
            return new Certificado(
                true,
                true,
                credencial.privateKey() != null,
                rucCoincide,
                atributo(subject, "CN"),
                atributo(certificate.getIssuerX500Principal().getName(X500Principal.RFC2253), "CN"),
                certificate.getNotBefore().toInstant().atZone(LIMA).toLocalDate(),
                certificate.getNotAfter().toInstant().atZone(LIMA).toLocalDate()
            );
        } catch (RuntimeException exception) {
            agregar(
                verificaciones,
                "CERTIFICADO",
                "Certificado digital",
                EstadoVerificacion.BLOQUEO,
                exception.getMessage(),
                "Revisa la ruta, contraseña y vigencia del certificado P12."
            );
            return new Certificado(false, false, false, false, null, null, null, null);
        }
    }

    private List<Serie> verificarBaseDatosYSeries(List<Verificacion> verificaciones) {
        try {
            boolean disponible = diagnosticoRepository.baseDatosDisponible();
            agregar(
                verificaciones,
                "BASE_DATOS",
                "PostgreSQL",
                disponible ? EstadoVerificacion.APROBADO : EstadoVerificacion.BLOQUEO,
                disponible
                    ? "La base de datos respondió correctamente."
                    : "PostgreSQL no respondió a la comprobación.",
                disponible ? null : "Revisa Docker y la conexión del backend."
            );
            List<Serie> series = diagnosticoRepository.listarSeriesProduccion().stream()
                .map(item -> new Serie(
                    item.tipoDocumento(),
                    item.serie(),
                    item.ultimoCorrelativo(),
                    item.ultimoCorrelativo() >= 99_999_999L
                        ? "AGOTADO"
                        : "%08d".formatted(item.ultimoCorrelativo() + 1),
                    item.activa()
                ))
                .toList();
            Set<String> tiposActivos = series.stream()
                .filter(Serie::activa)
                .map(Serie::tipoDocumento)
                .collect(java.util.stream.Collectors.toSet());
            boolean completas = tiposActivos.containsAll(SERIES_REQUERIDAS)
                && series.stream().allMatch(item -> item.ultimoCorrelativo() <= 99_999_999L);
            agregar(
                verificaciones,
                "SERIES",
                "Series de producción",
                completas ? EstadoVerificacion.APROBADO : EstadoVerificacion.BLOQUEO,
                completas
                    ? "Las seis series tributarias están activas y con correlativos disponibles."
                    : "Falta una serie activa o existe un correlativo agotado.",
                completas ? null : "Corrige serie_comprobante antes del piloto."
            );
            return series;
        } catch (RuntimeException exception) {
            agregar(
                verificaciones,
                "BASE_DATOS",
                "PostgreSQL y series",
                EstadoVerificacion.BLOQUEO,
                "No se pudo consultar la configuración tributaria en PostgreSQL.",
                "Revisa Docker, Flyway y la migración V29."
            );
            return List.of();
        }
    }

    private void verificarConectividad(List<Verificacion> verificaciones) {
        boolean endpointCorrecto = properties.getEndpointProduccion() != null
            && "https".equalsIgnoreCase(properties.getEndpointProduccion().getScheme())
            && ENDPOINT_PRODUCCION.equalsIgnoreCase(properties.getEndpointProduccion().getHost());
        agregar(
            verificaciones,
            "ENDPOINT",
            "Receptor oficial de producción",
            endpointCorrecto ? EstadoVerificacion.APROBADO : EstadoVerificacion.BLOQUEO,
            endpointCorrecto
                ? "El receptor configurado utiliza el dominio HTTPS oficial de SUNAT."
                : "El endpoint de producción no coincide con el receptor oficial esperado.",
            endpointCorrecto ? null : "Revisa SUNAT_PRODUCTION_ENDPOINT."
        );
        if (!endpointCorrecto) {
            return;
        }
        SunatEndpointProbe.Resultado resultado = endpointProbe.verificar(
            properties.getEndpointProduccion()
        );
        agregar(
            verificaciones,
            "CONECTIVIDAD",
            "Conectividad HTTPS con SUNAT",
            resultado.alcanzable() ? EstadoVerificacion.APROBADO : EstadoVerificacion.BLOQUEO,
            resultado.alcanzable()
                ? resultado.detalle() + " (HTTP " + resultado.codigoHttp() + ")."
                : resultado.detalle() + ".",
            resultado.alcanzable()
                ? null
                : "Revisa Internet, DNS, firewall y fecha/hora del servidor."
        );
    }

    private void verificarPendientes(List<Verificacion> verificaciones) {
        try {
            long produccion = diagnosticoRepository.contarComprobantesProduccion();
            agregar(
                verificaciones,
                "HISTORIAL_PRODUCCION",
                "Historial de numeración real",
                produccion == 0 ? EstadoVerificacion.APROBADO : EstadoVerificacion.ADVERTENCIA,
                produccion == 0
                    ? "No existen comprobantes de producción; el piloto iniciará en el primer correlativo."
                    : "Ya existen " + produccion + " comprobantes registrados en producción.",
                produccion == 0 ? null : "Verifica el último correlativo antes de continuar."
            );
            long betaPendientes = diagnosticoRepository.contarComprobantesBetaPendientes();
            agregar(
                verificaciones,
                "PENDIENTES_BETA",
                "Documentos pendientes de BETA",
                betaPendientes == 0
                    ? EstadoVerificacion.APROBADO
                    : EstadoVerificacion.ADVERTENCIA,
                betaPendientes == 0
                    ? "No hay comprobantes BETA pendientes de envío o baja."
                    : "Hay " + betaPendientes + " documentos BETA pendientes; permanecerán aislados de producción.",
                betaPendientes == 0 ? null : "Revísalos al volver temporalmente al ambiente BETA."
            );
        } catch (RuntimeException exception) {
            agregar(
                verificaciones,
                "PENDIENTES",
                "Documentos pendientes",
                EstadoVerificacion.ADVERTENCIA,
                "No se pudo calcular el historial de documentos.",
                "Revisa la base de datos antes del piloto."
            );
        }
    }

    private int contar(List<Verificacion> verificaciones, EstadoVerificacion estado) {
        return (int) verificaciones.stream().filter(item -> item.estado() == estado).count();
    }

    private void agregar(
        List<Verificacion> verificaciones,
        String codigo,
        String nombre,
        EstadoVerificacion estado,
        String detalle,
        String accion
    ) {
        verificaciones.add(new Verificacion(codigo, nombre, estado, detalle, accion));
    }

    private boolean configurada(String value) {
        return value != null && !value.isBlank();
    }

    private String atributo(String distinguishedName, String attribute) {
        String prefix = attribute.toUpperCase(Locale.ROOT) + "=";
        for (String part : distinguishedName.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)")) {
            String value = part.strip();
            if (value.toUpperCase(Locale.ROOT).startsWith(prefix)) {
                return value.substring(prefix.length()).replace("\\,", ",");
            }
        }
        return "No disponible";
    }
}
