package pe.com.proveperu.sgc.impresion.application.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.comprobante.api.dto.ClienteComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.ComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.EmpresaComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.RepresentacionComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.application.service.ComprobanteService;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.EnvioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.venta.api.dto.VentaDetalleResponse;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
@RequiredArgsConstructor
public class ComprobantePdfService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZONA_NEGOCIO);
    private static final Color AZUL = new Color(48, 78, 184);
    private static final Color AZUL_SUAVE = new Color(237, 241, 255);
    private static final Color TEXTO = new Color(23, 33, 58);
    private static final Color SECUNDARIO = new Color(105, 116, 143);
    private static final Color BORDE = new Color(218, 224, 236);
    private static final Color FONDO = new Color(247, 249, 252);
    private static final Color VERDE = new Color(47, 116, 99);
    private static final Color AMBAR = new Color(151, 98, 18);
    private static final Color ROJO = new Color(171, 54, 66);

    private static final Font EMPRESA = fuente(18, Font.BOLD, TEXTO);
    private static final Font RAZON_SOCIAL = fuente(9, Font.NORMAL, SECUNDARIO);
    private static final Font DOCUMENTO = fuente(13, Font.BOLD, AZUL);
    private static final Font NUMERO = fuente(16, Font.BOLD, TEXTO);
    private static final Font ETIQUETA = fuente(7.5f, Font.BOLD, SECUNDARIO);
    private static final Font VALOR = fuente(9, Font.NORMAL, TEXTO);
    private static final Font VALOR_NEGRITA = fuente(9, Font.BOLD, TEXTO);
    private static final Font CABECERA_TABLA = fuente(7.5f, Font.BOLD, Color.WHITE);
    private static final Font CELDA = fuente(8, Font.NORMAL, TEXTO);
    private static final Font CELDA_NEGRITA = fuente(8, Font.BOLD, TEXTO);
    private static final Font TOTAL = fuente(14, Font.BOLD, AZUL);
    private static final Font PIE = fuente(7.5f, Font.NORMAL, SECUNDARIO);

    private final ComprobanteService comprobanteService;
    private final QrComprobanteService qrComprobanteService;

    @Transactional(readOnly = true)
    public ArchivoComprobantePdf generar(Long idComprobante) {
        RepresentacionComprobanteResponse representacion = comprobanteService
            .obtenerRepresentacion(idComprobante);
        byte[] contenido = crearPdf(representacion);
        String nombre = representacion.comprobante().tipo().name().toLowerCase()
            + "-" + representacion.comprobante().numeroCompleto() + ".pdf";
        return new ArchivoComprobantePdf(nombre, contenido);
    }

    private byte[] crearPdf(RepresentacionComprobanteResponse representacion) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 34, 34, 30, 30);
            PdfWriter.getInstance(document, output);
            document.addTitle(representacion.comprobante().numeroCompleto());
            document.addSubject("Representación PDF del comprobante electrónico");
            document.addAuthor("SGC PROVEPERÚ");
            document.open();
            document.add(cabecera(representacion));
            if (representacion.comprobante().ambiente() == AmbienteSunat.BETA) {
                document.add(avisoBeta());
            }
            document.add(datosOperacion(representacion));
            document.add(detalle(representacion.comprobante()));
            document.add(totales(representacion.comprobante()));
            document.add(pieFiscal(representacion));
            document.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException(
                "No se pudo generar la representación PDF del comprobante",
                exception
            );
        }
    }

    private PdfPTable cabecera(RepresentacionComprobanteResponse representacion)
        throws DocumentException {
        EmpresaComprobanteResponse emisor = representacion.emisor();
        ComprobanteResponse comprobante = representacion.comprobante();
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.65f, 1f });
        table.setSpacingAfter(14);

        PdfPCell empresa = celdaSinBorde();
        String nombre = texto(emisor.nombreComercial()).isBlank()
            ? emisor.razonSocial()
            : emisor.nombreComercial();
        empresa.addElement(parrafo(nombre, EMPRESA, 2));
        if (!texto(nombre).equalsIgnoreCase(texto(emisor.razonSocial()))) {
            empresa.addElement(parrafo(emisor.razonSocial(), RAZON_SOCIAL, 2));
        }
        empresa.addElement(parrafo("RUC " + emisor.ruc(), VALOR_NEGRITA, 5));
        String direccion = texto(emisor.direccionSede()).isBlank()
            ? emisor.direccion()
            : emisor.direccionSede();
        empresa.addElement(parrafo(direccion, RAZON_SOCIAL, 2));
        if (!texto(emisor.telefono()).isBlank()) {
            empresa.addElement(parrafo("Teléfono: " + emisor.telefono(), RAZON_SOCIAL, 0));
        }
        table.addCell(empresa);

        PdfPCell documento = new PdfPCell();
        documento.setBorder(Rectangle.BOX);
        documento.setBorderColor(AZUL);
        documento.setBorderWidth(1.2f);
        documento.setBackgroundColor(AZUL_SUAVE);
        documento.setPadding(14);
        documento.setVerticalAlignment(Element.ALIGN_MIDDLE);
        String titulo = representacion.titulo()
            + (comprobante.tipo() == TipoComprobanteVenta.NOTA_VENTA ? "" : " ELECTRÓNICA");
        documento.addElement(parrafo(titulo, DOCUMENTO, 5));
        documento.addElement(parrafo(comprobante.numeroCompleto(), NUMERO, 7));
        documento.addElement(parrafo("RUC " + emisor.ruc(), VALOR_NEGRITA, 0));
        table.addCell(documento);
        return table;
    }

    private PdfPTable avisoBeta() {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        PdfPCell cell = new PdfPCell(new Phrase(
            "DOCUMENTO DE PRUEBA - AMBIENTE BETA - SIN VALIDEZ TRIBUTARIA",
            fuente(8.5f, Font.BOLD, ROJO)
        ));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(233, 177, 183));
        cell.setBackgroundColor(new Color(253, 241, 242));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        return table;
    }

    private PdfPTable datosOperacion(RepresentacionComprobanteResponse representacion)
        throws DocumentException {
        ComprobanteResponse comprobante = representacion.comprobante();
        ClienteComprobanteResponse cliente = representacion.cliente();
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 0.7f, 1.8f, 0.7f, 1.8f });
        table.setSpacingAfter(14);

        agregarDato(table, "FECHA DE EMISIÓN", FECHA_HORA.format(comprobante.fechaEmision()));
        agregarDato(table, "MONEDA", "PEN - SOLES");
        agregarDato(table, "CLIENTE", cliente.nombre());
        String documento = cliente.tipoDocumento() == null
            ? "SIN DOCUMENTO"
            : cliente.tipoDocumento() + " " + cliente.numeroDocumento();
        agregarDato(table, "DOCUMENTO", documento);
        agregarDato(table, "DIRECCIÓN", valorOpcional(cliente.direccion()), true);
        agregarDato(
            table,
            "CONDICIÓN DE PAGO",
            comprobante.venta().condicionPago().name().replace('_', ' ')
        );
        agregarDato(table, "VENDEDOR", "@" + comprobante.venta().vendedorLogin());
        return table;
    }

    private PdfPTable detalle(ComprobanteResponse comprobante) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.1f, 3.4f, 0.9f, 1.2f, 1.1f, 1.25f });
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSpacingAfter(12);
        agregarCabecera(table, "CÓDIGO");
        agregarCabecera(table, "DESCRIPCIÓN");
        agregarCabecera(table, "CANT.");
        agregarCabecera(table, "P. UNITARIO");
        agregarCabecera(table, "DESCUENTO");
        agregarCabecera(table, "IMPORTE");

        for (VentaDetalleResponse item : comprobante.items()) {
            agregarCelda(table, item.codigoProducto(), Element.ALIGN_LEFT, CELDA);
            agregarCelda(
                table,
                item.producto() + "\n" + item.unidadCodigo() + " - " + item.unidadMedida(),
                Element.ALIGN_LEFT,
                CELDA
            );
            agregarCelda(
                table,
                cantidad(item.cantidad()),
                Element.ALIGN_RIGHT,
                CELDA
            );
            agregarCelda(table, soles(item.precioUnitario()), Element.ALIGN_RIGHT, CELDA);
            agregarCelda(table, soles(item.descuento()), Element.ALIGN_RIGHT, CELDA);
            agregarCelda(table, soles(item.subtotal()), Element.ALIGN_RIGHT, CELDA_NEGRITA);
        }
        return table;
    }

    private PdfPTable totales(ComprobanteResponse comprobante) throws DocumentException {
        PdfPTable container = new PdfPTable(2);
        container.setWidthPercentage(100);
        container.setWidths(new float[] { 1.45f, 1f });
        container.setSpacingAfter(16);
        PdfPCell resumen = celdaSinBorde();
        resumen.setPadding(12);
        resumen.setBackgroundColor(FONDO);
        resumen.addElement(parrafo("RESUMEN DE LA OPERACIÓN", ETIQUETA, 5));
        resumen.addElement(parrafo(
            "Tipo de venta: " + comprobante.venta().tipoVenta().name().replace('_', ' '),
            VALOR,
            3
        ));
        resumen.addElement(parrafo(
            "Importe pagado: " + soles(comprobante.venta().importePagado()),
            VALOR,
            3
        ));
        resumen.addElement(parrafo(
            "Saldo pendiente: " + soles(comprobante.venta().saldoPendiente()),
            VALOR,
            0
        ));
        container.addCell(resumen);

        PdfPTable values = new PdfPTable(2);
        values.setWidthPercentage(100);
        agregarTotal(values, "Valor de venta", comprobante.subtotal(), false);
        BigDecimal descuento = comprobante.venta().descuentoTotal();
        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            agregarTotal(values, "Descuentos incluidos", descuento, false);
        }
        agregarTotal(values, "IGV incluido", comprobante.igv(), false);
        agregarTotal(values, "TOTAL", comprobante.total(), true);
        PdfPCell totalCell = celdaSinBorde();
        totalCell.setPaddingLeft(18);
        totalCell.addElement(values);
        container.addCell(totalCell);
        return container;
    }

    private PdfPTable pieFiscal(RepresentacionComprobanteResponse representacion)
        throws IOException, DocumentException {
        ComprobanteResponse comprobante = representacion.comprobante();
        var qr = qrComprobanteService.generar(representacion);
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 0.7f, 2.3f });
        table.setSpacingBefore(2);

        PdfPCell qrCell = celdaSinBorde();
        byte[] imagenBytes = Base64.getDecoder().decode(qr.imagenPngBase64());
        Image imagen = Image.getInstance(imagenBytes);
        imagen.scaleToFit(100, 100);
        imagen.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(imagen);
        table.addCell(qrCell);

        PdfPCell info = celdaSinBorde();
        info.setPaddingLeft(14);
        info.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Color colorEstado = colorEstado(comprobante);
        info.addElement(parrafo(estadoFiscal(comprobante), fuente(10, Font.BOLD, colorEstado), 6));
        info.addElement(parrafo(
            "Representación impresa del comprobante electrónico.",
            VALOR_NEGRITA,
            4
        ));
        info.addElement(parrafo(
            "El XML firmado y la constancia de recepción CDR son los archivos tributarios asociados a la operación.",
            PIE,
            4
        ));
        if (!texto(representacion.nota()).isBlank()) {
            info.addElement(parrafo(representacion.nota(), PIE, 4));
        }
        EnvioSunatResponse envio = comprobante.envioSunat();
        if (envio != null && !texto(envio.hashXml()).isBlank()) {
            info.addElement(parrafo("Hash XML: " + envio.hashXml(), PIE, 0));
        }
        table.addCell(info);
        return table;
    }

    private void agregarDato(PdfPTable table, String label, String value) {
        agregarDato(table, label, value, false);
    }

    private void agregarDato(PdfPTable table, String label, String value, boolean anchoCompleto) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, ETIQUETA));
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(BORDE);
        labelCell.setBackgroundColor(FONDO);
        labelCell.setPadding(7);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(valorOpcional(value), VALOR));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(BORDE);
        valueCell.setPadding(7);
        if (anchoCompleto) {
            valueCell.setColspan(3);
        }
        table.addCell(valueCell);
    }

    private void agregarCabecera(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, CABECERA_TABLA));
        cell.setBackgroundColor(AZUL);
        cell.setBorderColor(AZUL);
        cell.setPadding(7);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void agregarCelda(PdfPTable table, String value, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(valorOpcional(value), font));
        cell.setBorderColor(BORDE);
        cell.setPadding(7);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void agregarTotal(
        PdfPTable table,
        String label,
        BigDecimal value,
        boolean destacado
    ) {
        Font labelFont = destacado ? VALOR_NEGRITA : VALOR;
        Font valueFont = destacado ? TOTAL : VALOR_NEGRITA;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell valueCell = new PdfPCell(new Phrase(soles(value), valueFont));
        for (PdfPCell cell : new PdfPCell[] { labelCell, valueCell }) {
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(5);
            if (destacado) {
                cell.setBackgroundColor(AZUL_SUAVE);
                cell.setPaddingTop(8);
                cell.setPaddingBottom(8);
            }
        }
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String estadoFiscal(ComprobanteResponse comprobante) {
        if (comprobante.estado() == EstadoComprobante.ANULADO) {
            return "COMPROBANTE ANULADO";
        }
        if (comprobante.tipo() == TipoComprobanteVenta.NOTA_VENTA) {
            return "DOCUMENTO INTERNO - NO SE ENVÍA A SUNAT";
        }
        EnvioSunatResponse envio = comprobante.envioSunat();
        if (envio != null && envio.estado().aceptado()) {
            return envio.estado().name().replace('_', ' ') + " POR SUNAT";
        }
        if (envio != null && envio.estado().name().equals("RECHAZADO")) {
            return "RECHAZADO POR SUNAT";
        }
        if (comprobante.tipo() == TipoComprobanteVenta.BOLETA
            && comprobante.ambiente() == AmbienteSunat.PRODUCCION) {
            return "EMITIDA - PENDIENTE DE INFORMAR EN RESUMEN DIARIO";
        }
        return "EMITIDA - PENDIENTE DE VALIDACIÓN SUNAT";
    }

    private Color colorEstado(ComprobanteResponse comprobante) {
        if (comprobante.estado() == EstadoComprobante.ANULADO) {
            return ROJO;
        }
        EnvioSunatResponse envio = comprobante.envioSunat();
        if (envio != null && envio.estado().aceptado()) {
            return VERDE;
        }
        if (envio != null && envio.estado().name().equals("RECHAZADO")) {
            return ROJO;
        }
        return AMBAR;
    }

    private static Font fuente(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private PdfPCell celdaSinBorde() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private Paragraph parrafo(String value, Font font, float spacingAfter) {
        Paragraph paragraph = new Paragraph(valorOpcional(value), font);
        paragraph.setLeading(font.getSize() * 1.3f);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private String valorOpcional(String value) {
        return texto(value).isBlank() ? "-" : texto(value);
    }

    private static String texto(String value) {
        return value == null ? "" : value.strip();
    }

    private String soles(BigDecimal value) {
        return "S/ " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cantidad(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    public record ArchivoComprobantePdf(String nombre, byte[] contenido) {
    }
}
