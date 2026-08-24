package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.DiagnosticoSunatResponse.EstadoVerificacion;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.DiagnosticoSunatRepository;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.DiagnosticoSunatRepository.SerieConfigurada;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.security.CertificadoDigitalProvider;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.security.CertificadoDigitalProvider.CredencialFirma;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.SunatEndpointProbe;

class DiagnosticoSunatServiceTests {

    private final SunatProperties properties = new SunatProperties();
    private final CertificadoDigitalProvider certificadoProvider = mock(
        CertificadoDigitalProvider.class
    );
    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final DiagnosticoSunatRepository diagnosticoRepository = mock(
        DiagnosticoSunatRepository.class
    );
    private final SunatEndpointProbe endpointProbe = mock(SunatEndpointProbe.class);
    private final DiagnosticoSunatService service = new DiagnosticoSunatService(
        properties,
        certificadoProvider,
        empresaRepository,
        diagnosticoRepository,
        endpointProbe
    );

    @BeforeEach
    void configurarProduccionProtegida() {
        properties.setEnabled(true);
        properties.setAmbiente(AmbienteSunat.PRODUCCION);
        properties.setProductionEnabled(false);
        properties.setUsuarioSol("FACTURADOR");
        properties.setClaveSol("clave-segura");
        properties.setResumenDiarioAutomaticoEnabled(false);
        properties.setEndpointProduccion(URI.create(
            "https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService"
        ));
    }

    @Test
    void confirmaPreparacionSinDesbloquearEmisionNiConsumirCorrelativos() {
        Empresa empresa = empresaCompleta();
        X509Certificate certificate = certificate(empresa.getRuc());
        PrivateKey privateKey = mock(PrivateKey.class);
        when(certificadoProvider.cargar()).thenReturn(new CredencialFirma(
            privateKey,
            certificate
        ));
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(diagnosticoRepository.baseDatosDisponible()).thenReturn(true);
        when(diagnosticoRepository.listarSeriesProduccion()).thenReturn(seriesEnCero());
        when(diagnosticoRepository.contarComprobantesProduccion()).thenReturn(0L);
        when(diagnosticoRepository.contarComprobantesBetaPendientes()).thenReturn(0L);
        when(endpointProbe.verificar(properties.getEndpointProduccion())).thenReturn(
            new SunatEndpointProbe.Resultado(true, 200, "SUNAT respondió por HTTPS")
        );

        var resultado = service.diagnosticarProduccion();

        assertThat(resultado.listoParaPiloto()).isTrue();
        assertThat(resultado.emisionRealHabilitada()).isFalse();
        assertThat(resultado.bloqueos()).isZero();
        assertThat(resultado.series()).hasSize(6)
            .allSatisfy(serie -> {
                assertThat(serie.ultimoCorrelativo()).isZero();
                assertThat(serie.siguienteNumero()).isEqualTo("00000001");
            });
        assertThat(resultado.verificaciones())
            .filteredOn(item -> item.codigo().equals("PROTECCION"))
            .singleElement()
            .extracting(item -> item.estado())
            .isEqualTo(EstadoVerificacion.APROBADO);
        verifyNoMoreInteractions(privateKey);
    }

    @Test
    void bloqueaPilotoCuandoLasCredencialesConservanValoresDePrueba() {
        properties.setUsuarioSol("MODDATOS");
        properties.setClaveSol("MODDATOS");
        Empresa empresa = empresaCompleta();
        PrivateKey privateKey = mock(PrivateKey.class);
        X509Certificate certificate = certificate(empresa.getRuc());
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(certificadoProvider.cargar()).thenReturn(new CredencialFirma(
            privateKey,
            certificate
        ));
        when(diagnosticoRepository.baseDatosDisponible()).thenReturn(true);
        when(diagnosticoRepository.listarSeriesProduccion()).thenReturn(seriesEnCero());
        when(diagnosticoRepository.contarComprobantesProduccion()).thenReturn(0L);
        when(diagnosticoRepository.contarComprobantesBetaPendientes()).thenReturn(0L);
        when(endpointProbe.verificar(properties.getEndpointProduccion())).thenReturn(
            new SunatEndpointProbe.Resultado(true, 200, "SUNAT respondió por HTTPS")
        );

        var resultado = service.diagnosticarProduccion();

        assertThat(resultado.listoParaPiloto()).isFalse();
        assertThat(resultado.bloqueos()).isEqualTo(1);
        assertThat(resultado.verificaciones())
            .filteredOn(item -> item.codigo().equals("CREDENCIALES"))
            .singleElement()
            .extracting(item -> item.estado())
            .isEqualTo(EstadoVerificacion.BLOQUEO);
    }

    private Empresa empresaCompleta() {
        Empresa empresa = new Empresa();
        empresa.setRuc("20612296911");
        empresa.setRazonSocial("INVERSIONES PROVE PERU E.I.R.L.");
        empresa.setDireccion("JR. EJEMPLO 123");
        empresa.setUbigeo("150101");
        empresa.setDepartamento("LIMA");
        empresa.setProvincia("LIMA");
        empresa.setDistrito("LIMA");
        empresa.setCodigoPais("PE");
        empresa.setEstado("ACTIVO");
        return empresa;
    }

    private X509Certificate certificate(String ruc) {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal()).thenReturn(
            new X500Principal("CN=INVERSIONES PROVE PERU,OU=" + ruc + ",O=PROVEPERU,C=PE")
        );
        when(certificate.getIssuerX500Principal()).thenReturn(
            new X500Principal("CN=Entidad Certificadora,O=Proveedor,C=PE")
        );
        when(certificate.getNotBefore()).thenReturn(Date.from(
            Instant.parse("2026-08-23T00:00:00Z")
        ));
        when(certificate.getNotAfter()).thenReturn(Date.from(
            Instant.parse("2029-08-22T23:59:59Z")
        ));
        return certificate;
    }

    private List<SerieConfigurada> seriesEnCero() {
        return List.of(
            new SerieConfigurada("BOLETA", "B001", 0, true),
            new SerieConfigurada("FACTURA", "F001", 0, true),
            new SerieConfigurada("NOTA_CREDITO_BOLETA", "BC01", 0, true),
            new SerieConfigurada("NOTA_DEBITO_BOLETA", "BD01", 0, true),
            new SerieConfigurada("NOTA_CREDITO_FACTURA", "FC01", 0, true),
            new SerieConfigurada("NOTA_DEBITO_FACTURA", "FD01", 0, true)
        );
    }
}
