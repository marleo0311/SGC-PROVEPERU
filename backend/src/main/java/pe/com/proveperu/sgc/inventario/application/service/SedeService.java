package pe.com.proveperu.sgc.inventario.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.inventario.api.dto.SedeResponse;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;

@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public List<SedeResponse> listarActivas() {
        return sedeRepository.findAllByEstadoIgnoreCaseOrderByNombreAsc("ACTIVO")
            .stream()
            .map(SedeResponse::from)
            .toList();
    }
}
