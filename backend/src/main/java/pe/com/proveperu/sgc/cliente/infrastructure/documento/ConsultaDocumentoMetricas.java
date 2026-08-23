package pe.com.proveperu.sgc.cliente.infrastructure.documento;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ConsultaDocumentoMetricas {

    private final MeterRegistry meterRegistry;

    public ConsultaDocumentoMetricas(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void registrarCache(boolean encontrada, boolean contieneDatos) {
        Counter.builder("sgc.documento.cache")
            .tag("resultado", encontrada ? contieneDatos ? "hit" : "hit_negativo" : "miss")
            .register(meterRegistry)
            .increment();
    }

    public void registrarProveedor(String resultado, Duration duracion) {
        Timer.builder("sgc.documento.proveedor")
            .tag("resultado", resultado)
            .register(meterRegistry)
            .record(duracion);
    }

    public void registrarReintento(String motivo) {
        Counter.builder("sgc.documento.reintentos")
            .tag("motivo", motivo)
            .register(meterRegistry)
            .increment();
    }

    public void registrarCircuitoAbierto() {
        meterRegistry.counter("sgc.documento.circuito.abierto").increment();
    }

    public void registrarLimiteExcedido() {
        meterRegistry.counter("sgc.documento.limite.excedido").increment();
    }
}
