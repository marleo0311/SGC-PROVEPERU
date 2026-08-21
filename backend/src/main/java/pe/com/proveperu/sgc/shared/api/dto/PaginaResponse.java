package pe.com.proveperu.sgc.shared.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
    List<T> contenido,
    int pagina,
    int tamanio,
    long totalElementos,
    int totalPaginas,
    boolean ultima
) {
    public static <T> PaginaResponse<T> from(Page<T> page) {
        return new PaginaResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}
