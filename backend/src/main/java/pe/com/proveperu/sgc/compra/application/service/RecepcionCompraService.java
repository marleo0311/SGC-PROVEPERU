package pe.com.proveperu.sgc.compra.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.compra.api.dto.RecepcionCompraItemRequest;
import pe.com.proveperu.sgc.compra.api.dto.RecepcionCompraRequest;
import pe.com.proveperu.sgc.compra.api.dto.RecepcionCompraResponse;
import pe.com.proveperu.sgc.compra.domain.model.Compra;
import pe.com.proveperu.sgc.compra.domain.model.DetalleCompra;
import pe.com.proveperu.sgc.compra.domain.model.DetalleRecepcionCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoRecepcionCompra;
import pe.com.proveperu.sgc.compra.domain.model.RecepcionCompra;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.CompraRepository;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.DetalleRecepcionCompraRepository;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.RecepcionCompraRepository;
import pe.com.proveperu.sgc.cuentapagar.application.service.CuentaPagarService;
import pe.com.proveperu.sgc.inventario.application.service.InventarioService;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class RecepcionCompraService {

    private static final int ESCALA_CANTIDAD = 3;

    private final CompraRepository compraRepository;
    private final RecepcionCompraRepository recepcionRepository;
    private final DetalleRecepcionCompraRepository detalleRecepcionRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final CuentaPagarService cuentaPagarService;

    @Transactional
    public RecepcionCompraResponse crear(
        Long idCompra,
        RecepcionCompraRequest request,
        String usuarioLogin
    ) {
        Compra compra = compraRepository.findForUpdate(idCompra)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la compra solicitada"
            ));
        validarEstadoRecepcionable(compra);
        Sede sede = resolverSede(request.idSede());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        Map<Long, BigDecimal> cantidadesAcumuladas = cantidadesRecibidas(idCompra);
        List<ItemResuelto> items = resolverItems(
            compra,
            request,
            cantidadesAcumuladas
        );

        RecepcionCompra recepcion = new RecepcionCompra();
        recepcion.setCompra(compra);
        recepcion.setSede(sede);
        recepcion.setUsuario(usuario);
        recepcion.setObservacion(normalizarTexto(request.observacion()));
        recepcion.setEstado(items.stream().anyMatch(item -> !item.conforme())
            ? EstadoRecepcionCompra.CON_INCIDENCIA
            : EstadoRecepcionCompra.CONFIRMADA);
        recepcion = recepcionRepository.saveAndFlush(recepcion);

        items.sort(Comparator
            .comparing((ItemResuelto item) -> item.detalleCompra().getProducto().getId())
            .thenComparing(item -> item.detalleCompra().getId()));
        for (ItemResuelto item : items) {
            DetalleCompra detalleCompra = item.detalleCompra();
            DetalleRecepcionCompra detalleRecepcion = new DetalleRecepcionCompra();
            detalleRecepcion.setDetalleCompra(detalleCompra);
            detalleRecepcion.setProducto(detalleCompra.getProducto());
            detalleRecepcion.setUnidadMedida(detalleCompra.getUnidadMedida());
            detalleRecepcion.setCantidadEsperada(detalleCompra.getCantidad());
            detalleRecepcion.setCantidadRecibida(item.cantidadRecibida());
            detalleRecepcion.setCantidadAcumulada(item.cantidadAcumulada());
            detalleRecepcion.setCantidadPendiente(item.cantidadPendiente());
            detalleRecepcion.setConforme(item.conforme());
            detalleRecepcion.setObservacion(normalizarTexto(item.observacion()));
            recepcion.agregarDetalle(detalleRecepcion);

            inventarioService.registrarEntradaCompra(
                sede,
                detalleCompra.getProducto(),
                detalleCompra.getUnidadMedida(),
                item.cantidadRecibida(),
                usuario,
                recepcion.getId(),
                compra.getId()
            );
        }

        recepcion = recepcionRepository.saveAndFlush(recepcion);
        compra.setEstado(compraCompleta(compra, cantidadesAcumuladas)
            ? EstadoCompra.RECIBIDA
            : EstadoCompra.PARCIALMENTE_RECIBIDA);
        compraRepository.saveAndFlush(compra);
        cuentaPagarService.generarParaCompra(compra);
        return RecepcionCompraResponse.from(recepcion);
    }

    @Transactional(readOnly = true)
    public List<RecepcionCompraResponse> listar(Long idCompra) {
        if (!compraRepository.existsById(idCompra)) {
            throw new RecursoNoEncontradoException("No existe la compra solicitada");
        }
        return recepcionRepository.findAllByCompraIdOrderByFechaHoraDescIdDesc(idCompra)
            .stream()
            .map(RecepcionCompraResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> cantidadesRecibidas(Long idCompra) {
        return detalleRecepcionRepository.sumarCantidadesPorCompra(idCompra)
            .stream()
            .collect(Collectors.toMap(
                DetalleRecepcionCompraRepository.CantidadRecibidaPorDetalle::getIdDetalleCompra,
                DetalleRecepcionCompraRepository.CantidadRecibidaPorDetalle::getCantidadRecibida
            ));
    }

    private List<ItemResuelto> resolverItems(
        Compra compra,
        RecepcionCompraRequest request,
        Map<Long, BigDecimal> cantidadesAcumuladas
    ) {
        Map<Long, DetalleCompra> detallesPorId = compra.getDetalles().stream()
            .collect(Collectors.toMap(DetalleCompra::getId, Function.identity()));
        Map<Long, List<DetalleCompra>> detallesPorProducto = compra.getDetalles().stream()
            .collect(Collectors.groupingBy(detalle -> detalle.getProducto().getId()));
        Set<Long> detallesIncluidos = new HashSet<>();
        List<ItemResuelto> items = new ArrayList<>();

        for (RecepcionCompraItemRequest item : request.items()) {
            DetalleCompra detalle = resolverDetalle(
                item,
                detallesPorId,
                detallesPorProducto
            );
            if (!detallesIncluidos.add(detalle.getId())) {
                throw new SolicitudInvalidaException(
                    "No se puede repetir un detalle de compra en la misma recepción"
                );
            }
            validarCantidadPermitida(detalle, item.cantidadRecibida());
            BigDecimal cantidadRecibida = item.cantidadRecibida().setScale(
                ESCALA_CANTIDAD,
                RoundingMode.UNNECESSARY
            );
            BigDecimal recibidaAntes = cantidadesAcumuladas.getOrDefault(
                detalle.getId(),
                BigDecimal.ZERO.setScale(ESCALA_CANTIDAD)
            );
            BigDecimal pendienteAntes = detalle.getCantidad().subtract(recibidaAntes);
            if (pendienteAntes.compareTo(BigDecimal.ZERO) <= 0) {
                throw new OperacionNoPermitidaException(
                    "El producto " + detalle.getProducto().getCodigoInterno()
                        + " ya fue recibido completamente"
                );
            }
            if (cantidadRecibida.compareTo(pendienteAntes) > 0) {
                throw new ReglaNegocioException(
                    "La cantidad recibida del producto "
                        + detalle.getProducto().getCodigoInterno()
                        + " supera la cantidad pendiente: "
                        + pendienteAntes.toPlainString()
                );
            }
            boolean conforme = item.conformeEfectivo();
            if (!conforme
                && textoVacio(item.observacion())
                && textoVacio(request.observacion())) {
                throw new SolicitudInvalidaException(
                    "Una cantidad no conforme requiere una observación"
                );
            }
            BigDecimal acumulada = recibidaAntes.add(cantidadRecibida);
            BigDecimal pendiente = detalle.getCantidad().subtract(acumulada);
            cantidadesAcumuladas.put(detalle.getId(), acumulada);
            items.add(new ItemResuelto(
                detalle,
                cantidadRecibida,
                acumulada,
                pendiente,
                conforme,
                item.observacion()
            ));
        }
        return items;
    }

    private DetalleCompra resolverDetalle(
        RecepcionCompraItemRequest item,
        Map<Long, DetalleCompra> detallesPorId,
        Map<Long, List<DetalleCompra>> detallesPorProducto
    ) {
        if (item.idDetalleCompra() != null) {
            DetalleCompra detalle = detallesPorId.get(item.idDetalleCompra());
            if (detalle == null) {
                throw new SolicitudInvalidaException(
                    "El detalle indicado no pertenece a la compra"
                );
            }
            if (item.idProducto() != null
                && !detalle.getProducto().getId().equals(item.idProducto())) {
                throw new SolicitudInvalidaException(
                    "El producto no corresponde al detalle de compra indicado"
                );
            }
            return detalle;
        }

        List<DetalleCompra> coincidencias = detallesPorProducto.getOrDefault(
            item.idProducto(),
            List.of()
        );
        if (coincidencias.isEmpty()) {
            throw new SolicitudInvalidaException(
                "El producto indicado no pertenece a la compra"
            );
        }
        if (coincidencias.size() > 1) {
            throw new SolicitudInvalidaException(
                "El producto aparece en varias unidades; indique idDetalleCompra"
            );
        }
        return coincidencias.getFirst();
    }

    private boolean compraCompleta(
        Compra compra,
        Map<Long, BigDecimal> cantidadesAcumuladas
    ) {
        return compra.getDetalles().stream().allMatch(detalle ->
            cantidadesAcumuladas.getOrDefault(
                detalle.getId(),
                BigDecimal.ZERO
            ).compareTo(detalle.getCantidad()) == 0
        );
    }

    private void validarCantidadPermitida(
        DetalleCompra detalle,
        BigDecimal cantidad
    ) {
        if (!detalle.getUnidadMedida().isPermiteDecimales()
            && cantidad.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(
                "La unidad " + detalle.getUnidadMedida().getCodigo()
                    + " no admite cantidades decimales"
            );
        }
    }

    private void validarEstadoRecepcionable(Compra compra) {
        if (compra.getEstado() != EstadoCompra.REGISTRADA
            && compra.getEstado() != EstadoCompra.PARCIALMENTE_RECIBIDA) {
            throw new OperacionNoPermitidaException(
                "La compra " + compra.getEstado() + " no admite nuevas recepciones"
            );
        }
    }

    private Sede resolverSede(Long idSede) {
        Sede sede = idSede == null
            ? sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe una sede activa"
                ))
            : sedeRepository.findById(idSede)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe la sede solicitada"
                ));
        if (!"ACTIVO".equalsIgnoreCase(sede.getEstado())) {
            throw new OperacionNoPermitidaException(
                "No se puede recibir mercadería en una sede inactiva"
            );
        }
        return sede;
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

    private String normalizarTexto(String texto) {
        return textoVacio(texto) ? null : texto.strip();
    }

    private boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private record ItemResuelto(
        DetalleCompra detalleCompra,
        BigDecimal cantidadRecibida,
        BigDecimal cantidadAcumulada,
        BigDecimal cantidadPendiente,
        boolean conforme,
        String observacion
    ) {
    }
}
