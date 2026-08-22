package pe.com.proveperu.sgc.impresion.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormatoTicket {
    MM58(32),
    MM80(48);

    private final int anchoCaracteres;
}
