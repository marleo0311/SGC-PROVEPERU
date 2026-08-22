package pe.com.proveperu.sgc.cliente.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.cliente.api.dto.ClienteGuardarRequest;
import pe.com.proveperu.sgc.cliente.api.dto.ClienteHistorialResponse;
import pe.com.proveperu.sgc.cliente.api.dto.ClientePrecioEspecialResponse;
import pe.com.proveperu.sgc.cliente.api.dto.ClienteResponse;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClientePrecioEspecialRepository;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClientePrecioEspecialRepository precioEspecialRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResponse> listar(
        String buscar,
        EstadoCatalogo estado,
        TipoPersona tipoPersona,
        Boolean permiteCredito,
        Pageable pageable
    ) {
        Specification<Cliente> filtros = crearFiltros(buscar, estado, tipoPersona, permiteCredito);
        Page<ClienteResponse> pagina = clienteRepository.findAll(filtros, pageable)
            .map(ClienteResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return ClienteResponse.from(buscarCliente(id));
    }

    @Transactional
    public ClienteCreacionResultado crear(ClienteGuardarRequest request) {
        validarDatosPersona(request);
        String documento = request.numeroDocumento().strip();
        return clienteRepository.findByNumeroDocumento(documento)
            .map(cliente -> new ClienteCreacionResultado(ClienteResponse.from(cliente), false))
            .orElseGet(() -> {
                Cliente cliente = new Cliente();
                aplicarDatos(cliente, request, documento);
                cliente.setEstado(EstadoCatalogo.ACTIVO);
                Cliente guardado = clienteRepository.save(cliente);
                return new ClienteCreacionResultado(ClienteResponse.from(guardado), true);
            });
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteGuardarRequest request) {
        validarDatosPersona(request);
        Cliente cliente = buscarCliente(id);
        String documento = request.numeroDocumento().strip();
        if (clienteRepository.existsByNumeroDocumentoAndIdNot(documento, id)) {
            throw new ConflictoNegocioException("Ya existe un cliente con ese número de documento");
        }
        aplicarDatos(cliente, request, documento);
        return ClienteResponse.from(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional
    public ClienteResponse cambiarEstado(Long id, EstadoCatalogo estado) {
        Cliente cliente = buscarCliente(id);
        cliente.setEstado(estado);
        return ClienteResponse.from(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteHistorialResponse obtenerHistorial(Long id) {
        Cliente cliente = buscarCliente(id);
        List<ClientePrecioEspecialResponse> precios = precioEspecialRepository
            .findAllByClienteIdOrderByProductoNombreAscVigenteDesdeDesc(id)
            .stream()
            .map(ClientePrecioEspecialResponse::from)
            .toList();
        ClienteHistorialResponse.Resumen resumen = new ClienteHistorialResponse.Resumen(
            0,
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            null
        );
        return new ClienteHistorialResponse(
            ClienteResponse.from(cliente),
            resumen,
            List.of(),
            precios
        );
    }

    private Specification<Cliente> crearFiltros(
        String buscar,
        EstadoCatalogo estado,
        TipoPersona tipoPersona,
        Boolean permiteCredito
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            String criterio = buscar == null ? "" : buscar.strip().toLowerCase(Locale.ROOT);
            if (!criterio.isEmpty()) {
                String patron = "%" + criterio + "%";
                condiciones.add(builder.or(
                    builder.like(builder.lower(root.get("numeroDocumento")), patron),
                    builder.like(builder.lower(root.get("nombres")), patron),
                    builder.like(builder.lower(root.get("apellidos")), patron),
                    builder.like(builder.lower(root.get("razonSocial")), patron),
                    builder.like(builder.lower(root.get("nombreComercial")), patron)
                ));
            }
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            if (tipoPersona != null) {
                condiciones.add(builder.equal(root.get("tipoPersona"), tipoPersona));
            }
            if (permiteCredito != null) {
                condiciones.add(builder.equal(root.get("permiteCredito"), permiteCredito));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private void aplicarDatos(
        Cliente cliente,
        ClienteGuardarRequest request,
        String documento
    ) {
        cliente.setTipoPersona(request.tipoPersona());
        cliente.setTipoDocumento(request.tipoDocumento());
        cliente.setNumeroDocumento(documento);
        if (request.tipoPersona() == TipoPersona.NATURAL) {
            cliente.setNombres(request.nombres().strip());
            cliente.setApellidos(request.apellidos().strip());
            cliente.setRazonSocial(null);
        } else {
            cliente.setNombres(null);
            cliente.setApellidos(null);
            cliente.setRazonSocial(request.razonSocial().strip());
        }
        cliente.setNombreComercial(normalizarTextoOpcional(request.nombreComercial()));
        cliente.setDireccion(normalizarTextoOpcional(request.direccion()));
        cliente.setTelefono(normalizarTextoOpcional(request.telefono()));
        cliente.setWhatsapp(normalizarTextoOpcional(request.whatsapp()));
        cliente.setCorreo(normalizarCorreo(request.correo()));
        cliente.setPermiteCredito(request.permiteCredito());
    }

    private void validarDatosPersona(ClienteGuardarRequest request) {
        if (request.tipoPersona() == TipoPersona.NATURAL) {
            if (request.tipoDocumento() != TipoDocumentoCliente.DNI
                || !request.numeroDocumento().matches("^[0-9]{8}$")
                || !tieneTexto(request.nombres())
                || !tieneTexto(request.apellidos())
                || tieneTexto(request.razonSocial())) {
                throw new SolicitudInvalidaException(
                    "Una persona natural requiere DNI, nombres y apellidos"
                );
            }
            return;
        }
        if (request.tipoPersona() != TipoPersona.JURIDICA
            || request.tipoDocumento() != TipoDocumentoCliente.RUC
            || !request.numeroDocumento().matches("^[0-9]{11}$")
            || !tieneTexto(request.razonSocial())
            || tieneTexto(request.nombres())
            || tieneTexto(request.apellidos())) {
            throw new SolicitudInvalidaException(
                "Una persona jurídica requiere RUC y razón social"
            );
        }
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente solicitado"));
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private String normalizarCorreo(String correo) {
        String normalizado = normalizarTextoOpcional(correo);
        return normalizado == null ? null : normalizado.toLowerCase(Locale.ROOT);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
