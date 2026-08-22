package pe.com.proveperu.sgc.transporte.application.service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.transporte.api.dto.GastoCrearRequest;
import pe.com.proveperu.sgc.transporte.api.dto.GastoResponse;
import pe.com.proveperu.sgc.transporte.domain.model.Gasto;
import pe.com.proveperu.sgc.transporte.domain.model.TipoGasto;
import pe.com.proveperu.sgc.transporte.domain.model.Transportista;
import pe.com.proveperu.sgc.transporte.infrastructure.persistence.GastoRepository;
import pe.com.proveperu.sgc.transporte.infrastructure.persistence.TransportistaRepository;

@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepository gastoRepository;
    private final TransportistaRepository transportistaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<GastoResponse> listar(
        Long idTransportista,
        TipoGasto tipoGasto,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        validarRango(desde, hasta);
        Specification<Gasto> filtros = crearFiltros(
            idTransportista,
            tipoGasto,
            desde,
            hasta
        );
        Page<GastoResponse> pagina = gastoRepository.findAll(filtros, pageable)
            .map(GastoResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional
    public GastoResponse crear(GastoCrearRequest request, String usuarioLogin) {
        Transportista transportista = buscarTransportistaActivo(request.idTransportista());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);

        Gasto gasto = new Gasto();
        gasto.setTransportista(transportista);
        gasto.setUsuario(usuario);
        gasto.setTipoGasto(request.tipoGasto());
        gasto.setDescripcion(normalizarTextoOpcional(request.descripcion()));
        gasto.setImporte(request.importe());
        gasto.setFecha(request.fecha());
        gasto.setNumeroComprobante(normalizarTextoOpcional(request.numeroComprobante()));
        return GastoResponse.from(gastoRepository.save(gasto));
    }

    private Specification<Gasto> crearFiltros(
        Long idTransportista,
        TipoGasto tipoGasto,
        LocalDate desde,
        LocalDate hasta
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idTransportista != null) {
                condiciones.add(builder.equal(root.get("transportista").get("id"), idTransportista));
            }
            if (tipoGasto != null) {
                condiciones.add(builder.equal(root.get("tipoGasto"), tipoGasto));
            }
            if (desde != null) {
                condiciones.add(builder.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                condiciones.add(builder.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private Transportista buscarTransportistaActivo(Long idTransportista) {
        if (idTransportista == null) {
            return null;
        }
        Transportista transportista = transportistaRepository.findById(idTransportista)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el transportista solicitado"
            ));
        if (transportista.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede registrar un gasto con un transportista inactivo"
            );
        }
        return transportista;
    }

    private Usuario buscarUsuarioActivo(String usuarioLogin) {
        Usuario usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(usuarioLogin)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el usuario autenticado"
            ));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "El usuario autenticado no está activo"
            );
        }
        return usuario;
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                "La fecha inicial no puede ser posterior a la fecha final"
            );
        }
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
