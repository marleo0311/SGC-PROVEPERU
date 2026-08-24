package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoResumenDiarioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.ResumenDiarioSunatRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "app.sunat",
    name = "resumen-diario-automatico-enabled",
    havingValue = "true"
)
public class ResumenDiarioScheduler {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Set<EstadoResumenDiarioSunat> PENDIENTES = Set.of(
        EstadoResumenDiarioSunat.TICKET_RECIBIDO,
        EstadoResumenDiarioSunat.PROCESANDO
    );

    private final ResumenDiarioSunatService service;
    private final ResumenDiarioSunatRepository repository;
    private final SunatProperties properties;

    @Scheduled(cron = "${app.sunat.resumen-diario-cron:0 15 2 * * *}", zone = "America/Lima")
    public void ejecutar() {
        if (!properties.isEnabled()) {
            log.info("Automatización SUNAT omitida: integración deshabilitada");
            return;
        }
        consultarTicketsPendientes();
        LocalDate fecha = LocalDate.now(LIMA).minusDays(1);
        try {
            var resumenes = service.preparar(fecha);
            log.info("Resumen diario automático preparado para {}: {} lote(s)", fecha, resumenes.size());
            if (properties.isResumenDiarioAutoEnviar()) {
                resumenes.stream()
                    .filter(resumen -> resumen.estado() == EstadoResumenDiarioSunat.GENERADO)
                    .forEach(resumen -> enviarSeguro(resumen.id()));
            }
        } catch (RuntimeException exception) {
            log.error("No se pudo preparar el resumen diario automático para {}", fecha, exception);
        }
    }

    private void consultarTicketsPendientes() {
        repository.findByEstadoInOrderByFechaCreacionAsc(PENDIENTES)
            .forEach(resumen -> {
                try {
                    service.consultarEstado(resumen.getId());
                } catch (RuntimeException exception) {
                    log.warn("No se pudo consultar el ticket del resumen SUNAT {}", resumen.getId(), exception);
                }
            });
    }

    private void enviarSeguro(Long id) {
        try {
            service.enviar(id);
        } catch (RuntimeException exception) {
            log.error("No se pudo enviar automáticamente el resumen SUNAT {}", id, exception);
        }
    }
}
