package pe.com.proveperu.sgc.cliente.application.service;

import pe.com.proveperu.sgc.cliente.api.dto.ClienteResponse;

public record ClienteCreacionResultado(
    ClienteResponse cliente,
    boolean creado
) {
}
