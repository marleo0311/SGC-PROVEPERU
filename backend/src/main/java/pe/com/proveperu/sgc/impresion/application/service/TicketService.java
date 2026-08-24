package pe.com.proveperu.sgc.impresion.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.comprobante.api.dto.ClienteComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.ComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.EmpresaComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.RepresentacionComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.application.service.ComprobanteService;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.impresion.api.dto.TicketResponse;
import pe.com.proveperu.sgc.impresion.domain.model.FormatoTicket;
import pe.com.proveperu.sgc.venta.api.dto.VentaDetalleResponse;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZONA_NEGOCIO);

    private final ComprobanteService comprobanteService;
    private final QrComprobanteService qrComprobanteService;

    @Transactional(readOnly = true)
    public TicketResponse generar(Long idComprobante, FormatoTicket formato) {
        RepresentacionComprobanteResponse representacion = comprobanteService
            .obtenerRepresentacion(idComprobante);
        ComprobanteResponse comprobante = representacion.comprobante();
        int ancho = formato.getAnchoCaracteres();
        List<String> lineas = new ArrayList<>();

        agregarCabecera(lineas, representacion, ancho);
        agregarDocumento(lineas, representacion, ancho);
        agregarCliente(lineas, representacion.cliente(), ancho);
        agregarItems(lineas, comprobante.items(), ancho);
        agregarTotales(lineas, comprobante, ancho);
        agregarPie(lineas, representacion, ancho);

        var qr = qrComprobanteService.generar(representacion);
        return new TicketResponse(
            comprobante.id(),
            comprobante.idVenta(),
            comprobante.numeroCompleto(),
            comprobante.estado(),
            formato,
            ancho,
            "UTF-8",
            false,
            Instant.now(),
            String.join("\n", lineas),
            qr.contenido(),
            qr.imagenPngBase64()
        );
    }

    private void agregarCabecera(
        List<String> lineas,
        RepresentacionComprobanteResponse representacion,
        int ancho
    ) {
        EmpresaComprobanteResponse emisor = representacion.emisor();
        String nombre = emisor.nombreComercial() == null
            || emisor.nombreComercial().isBlank()
            ? emisor.razonSocial()
            : emisor.nombreComercial();
        agregarCentrado(lineas, nombre, ancho);
        agregarCentrado(lineas, emisor.razonSocial(), ancho);
        agregarCentrado(lineas, "RUC " + emisor.ruc(), ancho);
        agregarCentrado(lineas, emisor.sede(), ancho);
        String direccion = emisor.direccionSede() == null
            || emisor.direccionSede().isBlank()
            ? emisor.direccion()
            : emisor.direccionSede();
        if (direccion != null && !direccion.isBlank()) {
            agregarCentrado(lineas, direccion, ancho);
        }
        if (emisor.telefono() != null && !emisor.telefono().isBlank()) {
            agregarCentrado(lineas, "Tel. " + emisor.telefono(), ancho);
        }
    }

    private void agregarDocumento(
        List<String> lineas,
        RepresentacionComprobanteResponse representacion,
        int ancho
    ) {
        ComprobanteResponse comprobante = representacion.comprobante();
        lineas.add(separador(ancho));
        if (comprobante.estado() == EstadoComprobante.ANULADO) {
            agregarCentrado(lineas, "*** COMPROBANTE ANULADO ***", ancho);
        }
        agregarCentrado(lineas, representacion.titulo(), ancho);
        agregarCentrado(lineas, comprobante.numeroCompleto(), ancho);
        lineas.add(separador(ancho));
        agregarPar(lineas, "Fecha", FECHA_HORA.format(comprobante.fechaEmision()), ancho);
        agregarPar(
            lineas,
            "Vendedor",
            comprobante.venta().vendedorLogin(),
            ancho
        );
        if (comprobante.estado() == EstadoComprobante.ANULADO) {
            agregarPar(
                lineas,
                "Anulado",
                FECHA_HORA.format(comprobante.fechaAnulacion()),
                ancho
            );
            agregarTexto(
                lineas,
                "Motivo: " + comprobante.motivoAnulacion(),
                ancho
            );
        }
    }

    private void agregarCliente(
        List<String> lineas,
        ClienteComprobanteResponse cliente,
        int ancho
    ) {
        lineas.add(separador(ancho));
        agregarTexto(lineas, "Cliente: " + cliente.nombre(), ancho);
        if (cliente.tipoDocumento() != null) {
            agregarTexto(
                lineas,
                cliente.tipoDocumento() + ": " + cliente.numeroDocumento(),
                ancho
            );
        }
        if (cliente.direccion() != null && !cliente.direccion().isBlank()) {
            agregarTexto(lineas, "Dirección: " + cliente.direccion(), ancho);
        }
    }

    private void agregarItems(
        List<String> lineas,
        List<VentaDetalleResponse> items,
        int ancho
    ) {
        lineas.add(separador(ancho));
        agregarPar(lineas, "DESCRIPCIÓN", "IMPORTE", ancho);
        lineas.add(separador(ancho));
        for (VentaDetalleResponse item : items) {
            agregarTexto(
                lineas,
                item.codigoProducto() + " " + item.producto(),
                ancho
            );
            agregarPar(
                lineas,
                cantidad(item.cantidad()) + " " + item.unidadCodigo()
                    + " x " + dinero(item.precioUnitario()),
                dinero(item.subtotal()),
                ancho
            );
            if (item.descuento().compareTo(BigDecimal.ZERO) > 0) {
                agregarPar(
                    lineas,
                    "Descuento incluido",
                    "-" + dinero(item.descuento()),
                    ancho
                );
            }
        }
    }

    private void agregarTotales(
        List<String> lineas,
        ComprobanteResponse comprobante,
        int ancho
    ) {
        lineas.add(separador(ancho));
        agregarPar(lineas, "Subtotal neto", dinero(comprobante.subtotal()), ancho);
        BigDecimal descuento = comprobante.venta().descuentoTotal();
        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            agregarPar(
                lineas,
                "Descuentos incluidos",
                dinero(descuento),
                ancho
            );
        }
        agregarPar(lineas, "IGV", dinero(comprobante.igv()), ancho);
        agregarPar(lineas, "TOTAL PEN", dinero(comprobante.total()), ancho);
    }

    private void agregarPie(
        List<String> lineas,
        RepresentacionComprobanteResponse representacion,
        int ancho
    ) {
        lineas.add(separador(ancho));
        agregarCentrado(lineas, "Gracias por su compra", ancho);
        agregarCentrado(lineas, representacion.nota(), ancho);
    }

    private void agregarPar(
        List<String> lineas,
        String izquierda,
        String derecha,
        int ancho
    ) {
        String textoIzquierda = limpiar(izquierda);
        String textoDerecha = limpiar(derecha);
        int espacios = ancho - textoIzquierda.length() - textoDerecha.length();
        if (espacios >= 1) {
            lineas.add(textoIzquierda + " ".repeat(espacios) + textoDerecha);
            return;
        }
        agregarTexto(lineas, textoIzquierda, ancho);
        agregarDerecha(lineas, textoDerecha, ancho);
    }

    private void agregarCentrado(List<String> lineas, String texto, int ancho) {
        for (String parte : envolver(texto, ancho)) {
            int espacios = Math.max(0, (ancho - parte.length()) / 2);
            lineas.add(" ".repeat(espacios) + parte);
        }
    }

    private void agregarDerecha(List<String> lineas, String texto, int ancho) {
        for (String parte : envolver(texto, ancho)) {
            lineas.add(" ".repeat(Math.max(0, ancho - parte.length())) + parte);
        }
    }

    private void agregarTexto(List<String> lineas, String texto, int ancho) {
        lineas.addAll(envolver(texto, ancho));
    }

    private List<String> envolver(String texto, int ancho) {
        String limpio = limpiar(texto);
        if (limpio.isEmpty()) {
            return List.of("");
        }
        List<String> resultado = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabraOriginal : limpio.split(" ")) {
            String palabra = palabraOriginal;
            while (palabra.length() > ancho) {
                if (!actual.isEmpty()) {
                    resultado.add(actual.toString());
                    actual.setLength(0);
                }
                resultado.add(palabra.substring(0, ancho));
                palabra = palabra.substring(ancho);
            }
            if (palabra.isEmpty()) {
                continue;
            }
            if (actual.isEmpty()) {
                actual.append(palabra);
            } else if (actual.length() + 1 + palabra.length() <= ancho) {
                actual.append(' ').append(palabra);
            } else {
                resultado.add(actual.toString());
                actual.setLength(0);
                actual.append(palabra);
            }
        }
        if (!actual.isEmpty()) {
            resultado.add(actual.toString());
        }
        return resultado;
    }

    private String limpiar(String texto) {
        return texto == null
            ? ""
            : texto.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .strip();
    }

    private String separador(int ancho) {
        return "-".repeat(ancho);
    }

    private String dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cantidad(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
