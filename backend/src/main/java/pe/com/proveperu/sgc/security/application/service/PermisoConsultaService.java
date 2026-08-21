package pe.com.proveperu.sgc.security.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.security.api.dto.PermisoResponse;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;

@Service
@RequiredArgsConstructor
public class PermisoConsultaService {

    private final PermisoRepository permisoRepository;

    @Transactional(readOnly = true)
    public List<PermisoResponse> listar(String modulo) {
        return permisoRepository.findAllByOrderByModuloAscCodigoAsc().stream()
            .filter(permiso -> modulo == null
                || modulo.isBlank()
                || permiso.getModulo().equalsIgnoreCase(modulo.strip()))
            .map(PermisoResponse::from)
            .toList();
    }
}
