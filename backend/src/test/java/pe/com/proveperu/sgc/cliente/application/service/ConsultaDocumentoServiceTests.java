package pe.com.proveperu.sgc.cliente.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.cliente.application.dto.DatosDocumentoConsultado;
import pe.com.proveperu.sgc.cliente.application.port.ConsultaDocumentoGateway;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.cliente.infrastructure.documento.ConsultaDocumentoRateLimiter;
import pe.com.proveperu.sgc.cliente.infrastructure.documento.IntegracionDocumentoException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@ExtendWith(MockitoExtension.class)
class ConsultaDocumentoServiceTests {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ConsultaDocumentoGateway gateway;

    @Mock
    private ConsultaDocumentoRateLimiter rateLimiter;

    @InjectMocks
    private ConsultaDocumentoService service;

    @Test
    void priorizaElClienteRegistradoSinConsumirElProveedor() {
        Cliente cliente = new Cliente();
        cliente.setId(9L);
        cliente.setTipoPersona(TipoPersona.JURIDICA);
        cliente.setTipoDocumento(TipoDocumentoCliente.RUC);
        cliente.setNumeroDocumento("20601030013");
        cliente.setRazonSocial("REXTIE S.A.C.");
        cliente.setEstado(EstadoCatalogo.ACTIVO);
        when(gateway.disponible(TipoDocumentoCliente.RUC)).thenReturn(true);
        when(clienteRepository.findByNumeroDocumento("20601030013"))
            .thenReturn(Optional.of(cliente));

        var response = service.consultar(TipoDocumentoCliente.RUC, "20601030013", "marco");

        assertThat(response.encontrado()).isTrue();
        assertThat(response.origen()).isEqualTo("LOCAL");
        assertThat(response.idCliente()).isEqualTo(9L);
        assertThat(response.nombreMostrar()).isEqualTo("REXTIE S.A.C.");
        verify(gateway, never()).consultar(TipoDocumentoCliente.RUC, "20601030013");
        verify(rateLimiter, never()).verificar("marco");
    }

    @Test
    void devuelveDatosExternosListosParaRegistrar() {
        DatosDocumentoConsultado datos = new DatosDocumentoConsultado(
            TipoPersona.NATURAL,
            TipoDocumentoCliente.DNI,
            "46027897",
            "ERACLEO JUAN",
            "HUAMANI MENDOZA",
            null,
            null,
            null,
            null,
            null
        );
        when(gateway.disponible(TipoDocumentoCliente.DNI)).thenReturn(true);
        when(clienteRepository.findByNumeroDocumento("46027897")).thenReturn(Optional.empty());
        when(gateway.consultar(TipoDocumentoCliente.DNI, "46027897"))
            .thenReturn(Optional.of(datos));

        var response = service.consultar(TipoDocumentoCliente.DNI, "46027897", "marco");

        assertThat(response.encontrado()).isTrue();
        assertThat(response.origen()).isEqualTo("EXTERNO");
        assertThat(response.idCliente()).isNull();
        assertThat(response.nombreMostrar()).isEqualTo("ERACLEO JUAN HUAMANI MENDOZA");
        verify(rateLimiter).verificar("marco");
    }

    @Test
    void explicaCuandoLaConsultaExternaNoEstaConfigurada() {
        when(gateway.disponible(TipoDocumentoCliente.RUC)).thenReturn(false);
        when(clienteRepository.findByNumeroDocumento("20601030013")).thenReturn(Optional.empty());

        var response = service.consultar(TipoDocumentoCliente.RUC, "20601030013", "marco");

        assertThat(response.encontrado()).isFalse();
        assertThat(response.origen()).isEqualTo("NO_CONFIGURADO");
        assertThat(response.mensaje()).contains("DOCUMENT_LOOKUP_TOKEN");
        verify(rateLimiter, never()).verificar("marco");
    }

    @Test
    void rechazaRucConDigitoVerificadorIncorrecto() {
        assertThatThrownBy(() -> service.consultar(
            TipoDocumentoCliente.RUC,
            "20601030014",
            "marco"
        ))
            .isInstanceOf(SolicitudInvalidaException.class)
            .hasMessageContaining("dígito verificador");

        verify(gateway, never()).consultar(TipoDocumentoCliente.RUC, "20601030014");
    }

    @Test
    void informaCuandoElProveedorNoEncuentraElRuc() {
        when(gateway.disponible(TipoDocumentoCliente.RUC)).thenReturn(true);
        when(clienteRepository.findByNumeroDocumento("20601030013")).thenReturn(Optional.empty());
        when(gateway.consultar(TipoDocumentoCliente.RUC, "20601030013"))
            .thenReturn(Optional.empty());

        var response = service.consultar(TipoDocumentoCliente.RUC, "20601030013", "marco");

        assertThat(response.encontrado()).isFalse();
        assertThat(response.origen()).isEqualTo("NO_ENCONTRADO");
    }

    @Test
    void conservaEstadoYCondicionNoHabidaParaImpedirElAltaEnLaInterfaz() {
        DatosDocumentoConsultado datos = new DatosDocumentoConsultado(
            TipoPersona.JURIDICA,
            TipoDocumentoCliente.RUC,
            "20601030013",
            null,
            null,
            "EMPRESA DE PRUEBA S.A.C.",
            null,
            "LIMA",
            "BAJA DE OFICIO",
            "NO HABIDO"
        );
        when(gateway.disponible(TipoDocumentoCliente.RUC)).thenReturn(true);
        when(clienteRepository.findByNumeroDocumento("20601030013")).thenReturn(Optional.empty());
        when(gateway.consultar(TipoDocumentoCliente.RUC, "20601030013"))
            .thenReturn(Optional.of(datos));

        var response = service.consultar(TipoDocumentoCliente.RUC, "20601030013", "marco");

        assertThat(response.estadoContribuyente()).isEqualTo("BAJA DE OFICIO");
        assertThat(response.condicionDomicilio()).isEqualTo("NO HABIDO");
    }

    @Test
    void propagaLaFallaDeComunicacionSinConvertirlaEnClienteNoEncontrado() {
        when(gateway.disponible(TipoDocumentoCliente.RUC)).thenReturn(true);
        when(clienteRepository.findByNumeroDocumento("20601030013")).thenReturn(Optional.empty());
        when(gateway.consultar(TipoDocumentoCliente.RUC, "20601030013"))
            .thenThrow(new IntegracionDocumentoException("Proveedor desconectado"));

        assertThatThrownBy(() -> service.consultar(
            TipoDocumentoCliente.RUC,
            "20601030013",
            "marco"
        ))
            .isInstanceOf(IntegracionDocumentoException.class)
            .hasMessageContaining("desconectado");
    }
}
