package pe.com.proveperu.sgc.comprobante.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.comprobante.api.dto.ClienteComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.ComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.EmpresaComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.RepresentacionComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

@Service
@RequiredArgsConstructor
public class ComprobanteService {

    private static final BigDecimal LIMITE_BOLETA_SIN_DOCUMENTO = new BigDecimal("700.00");

    private final ComprobanteRepository comprobanteRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public Comprobante emitirParaVenta(Venta venta) {
        Comprobante existente = comprobanteRepository.findByVentaId(venta.getId())
            .orElse(null);
        if (existente != null) {
            venta.setComprobante(existente);
            return existente;
        }
        validarEmision(venta);
        Comprobante comprobante = new Comprobante();
        comprobante.setVenta(venta);
        comprobante.setTipo(venta.getTipoComprobante());
        comprobante.setSerie(serie(venta.getTipoComprobante()));
        comprobante.setNumero("%08d".formatted(siguienteNumero(
            venta.getTipoComprobante()
        )));
        comprobante.setSubtotal(venta.getSubtotal());
        comprobante.setIgv(venta.getIgv());
        comprobante.setTotal(venta.getTotal());
        comprobante.setEstado(venta.getTipoComprobante() == TipoComprobanteVenta.NOTA_VENTA
            ? EstadoComprobante.EMITIDO
            : EstadoComprobante.PENDIENTE_ENVIO);
        comprobante = comprobanteRepository.saveAndFlush(comprobante);
        venta.setComprobante(comprobante);
        return comprobante;
    }

    public void validarEmision(Venta venta) {
        Cliente cliente = venta.getCliente();
        if (venta.getTipoComprobante() == TipoComprobanteVenta.FACTURA
            && (cliente == null || cliente.getTipoDocumento() != TipoDocumentoCliente.RUC)) {
            throw new OperacionNoPermitidaException(
                "Una factura requiere un cliente identificado con RUC"
            );
        }
        if (venta.getTipoComprobante() == TipoComprobanteVenta.BOLETA
            && venta.getTotal().compareTo(LIMITE_BOLETA_SIN_DOCUMENTO) > 0
            && cliente == null) {
            throw new OperacionNoPermitidaException(
                "Una boleta mayor a S/ 700.00 requiere un cliente identificado con DNI o RUC"
            );
        }
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse obtener(Long id) {
        return ComprobanteResponse.from(buscarDetalle(id));
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse obtenerPorVenta(Long idVenta) {
        return ComprobanteResponse.from(buscarDetallePorVenta(idVenta));
    }

    @Transactional(readOnly = true)
    public RepresentacionComprobanteResponse obtenerRepresentacion(Long id) {
        Comprobante comprobante = buscarDetalle(id);
        Venta venta = comprobante.getVenta();
        Empresa empresa = empresaRepository.findById(venta.getSede().getIdEmpresa())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la empresa emisora del comprobante"
            ));
        return new RepresentacionComprobanteResponse(
            titulo(comprobante.getTipo()),
            "PEN",
            EmpresaComprobanteResponse.from(empresa, venta.getSede()),
            ClienteComprobanteResponse.from(venta.getCliente()),
            ComprobanteResponse.from(comprobante),
            "Representación interna; no acredita envío ni aceptación por SUNAT"
        );
    }

    @Transactional(readOnly = true)
    public Long obtenerIdVenta(Long id) {
        return comprobanteRepository.findById(id)
            .map(comprobante -> comprobante.getVenta().getId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el comprobante solicitado"
            ));
    }

    @Transactional
    public void anularPorVenta(Venta venta, Usuario usuario, String motivo) {
        Comprobante comprobante = comprobanteRepository
            .findForUpdateByVentaId(venta.getId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "La venta no tiene un comprobante asociado"
            ));
        if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
            return;
        }
        if (comprobante.getEnvioSunat() != null
            && comprobante.getEnvioSunat().getEstado().aceptado()) {
            throw new OperacionNoPermitidaException(
                "Un comprobante aceptado por SUNAT no puede anularse únicamente de forma local"
            );
        }
        comprobante.setEstado(EstadoComprobante.ANULADO);
        comprobante.setFechaAnulacion(Instant.now());
        comprobante.setMotivoAnulacion(motivo.strip());
        comprobante.setUsuarioAnulacion(usuario);
        comprobanteRepository.saveAndFlush(comprobante);
    }

    private Comprobante buscarDetalle(Long id) {
        return comprobanteRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el comprobante solicitado"
            ));
    }

    private Comprobante buscarDetallePorVenta(Long idVenta) {
        return comprobanteRepository.findDetalleByVentaId(idVenta)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "La venta no tiene un comprobante asociado"
            ));
    }

    private Long siguienteNumero(TipoComprobanteVenta tipo) {
        return switch (tipo) {
            case NOTA_VENTA -> comprobanteRepository.siguienteNotaVenta();
            case BOLETA -> comprobanteRepository.siguienteBoleta();
            case FACTURA -> comprobanteRepository.siguienteFactura();
        };
    }

    private String serie(TipoComprobanteVenta tipo) {
        return switch (tipo) {
            case NOTA_VENTA -> "NV01";
            case BOLETA -> "B001";
            case FACTURA -> "F001";
        };
    }

    private String titulo(TipoComprobanteVenta tipo) {
        return switch (tipo) {
            case NOTA_VENTA -> "NOTA DE VENTA";
            case BOLETA -> "BOLETA DE VENTA";
            case FACTURA -> "FACTURA";
        };
    }
}
