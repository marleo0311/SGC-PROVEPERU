package pe.com.proveperu.sgc.transporte.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.com.proveperu.sgc.transporte.domain.model.TipoDocumentoTransportista;
import pe.com.proveperu.sgc.transporte.domain.model.Transportista;

public interface TransportistaRepository
    extends JpaRepository<Transportista, Long>, JpaSpecificationExecutor<Transportista> {

    boolean existsByTipoDocumentoAndNumeroDocumento(
        TipoDocumentoTransportista tipoDocumento,
        String numeroDocumento
    );

    boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
        TipoDocumentoTransportista tipoDocumento,
        String numeroDocumento,
        Long id
    );
}
