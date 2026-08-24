package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.TipoNotaElectronica;

public record NotaElectronicaCrearRequest(
    @NotNull TipoNotaElectronica tipo,
    @NotBlank @Pattern(regexp = "\\d{2}") String codigoMotivo,
    @NotBlank @Size(min = 3, max = 300) String descripcionMotivo,
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal total
) {
}
