package pe.com.proveperu.sgc.reporte.application.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteCajaResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteFinanzasResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteInventarioResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class ReporteExportService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final org.openpdf.text.Font PDF_TITLE = new org.openpdf.text.Font(
        org.openpdf.text.Font.HELVETICA,
        17,
        org.openpdf.text.Font.BOLD,
        new Color(26, 38, 70)
    );
    private static final org.openpdf.text.Font PDF_SUBTITLE = new org.openpdf.text.Font(
        org.openpdf.text.Font.HELVETICA,
        9,
        org.openpdf.text.Font.NORMAL,
        new Color(105, 116, 143)
    );
    private static final org.openpdf.text.Font PDF_CELL = new org.openpdf.text.Font(
        org.openpdf.text.Font.HELVETICA,
        8
    );
    private static final org.openpdf.text.Font PDF_HEADER = new org.openpdf.text.Font(
        org.openpdf.text.Font.HELVETICA,
        8,
        org.openpdf.text.Font.BOLD,
        Color.WHITE
    );

    private final ReporteService reporteService;

    @Transactional(readOnly = true)
    public ArchivoReporte exportar(
        String tipo,
        String formato,
        LocalDate desde,
        LocalDate hasta,
        Long idSede,
        int limite
    ) {
        TipoReporte tipoReporte = parseTipo(tipo);
        FormatoReporte formatoReporte = parseFormato(formato);
        Object data = consultar(tipoReporte, desde, hasta, idSede, limite);
        byte[] contenido = formatoReporte == FormatoReporte.XLSX
            ? crearExcel(tipoReporte, data)
            : crearPdf(tipoReporte, data);
        String fecha = LocalDate.now().format(FILE_DATE);
        String extension = formatoReporte.name().toLowerCase();
        String mediaType = formatoReporte == FormatoReporte.XLSX
            ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            : "application/pdf";
        return new ArchivoReporte(
            "reporte-" + tipoReporte.name().toLowerCase() + "-" + fecha + "." + extension,
            mediaType,
            contenido
        );
    }

    private Object consultar(
        TipoReporte tipo,
        LocalDate desde,
        LocalDate hasta,
        Long idSede,
        int limite
    ) {
        return switch (tipo) {
            case VENTAS -> reporteService.obtenerVentas(desde, hasta, idSede, limite);
            case INVENTARIO -> reporteService.obtenerInventario(idSede, limite);
            case FINANZAS -> reporteService.obtenerFinanzas();
            case CAJA -> reporteService.obtenerCaja(desde, hasta, idSede);
        };
    }

    private byte[] crearExcel(TipoReporte tipo, Object data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(titulo(tipo));
            CellStyle title = titleStyle(workbook);
            CellStyle header = headerStyle(workbook);
            CellStyle money = moneyStyle(workbook);
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SGC PROVEPERÚ · " + titulo(tipo));
            titleCell.setCellStyle(title);

            switch (tipo) {
                case VENTAS -> escribirVentasExcel(sheet, (ReporteVentasResponse) data, header, money);
                case INVENTARIO -> escribirInventarioExcel(sheet, (ReporteInventarioResponse) data, header);
                case FINANZAS -> escribirFinanzasExcel(sheet, (ReporteFinanzasResponse) data, header, money);
                case CAJA -> escribirCajaExcel(sheet, (ReporteCajaResponse) data, header, money);
            }
            for (int column = 0; column < Math.max(8, sheet.getRow(3).getLastCellNum()); column++) {
                sheet.autoSizeColumn(column);
                sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 800, 16000));
            }
            sheet.createFreezePane(0, 4);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", exception);
        }
    }

    private void escribirVentasExcel(Sheet sheet, ReporteVentasResponse data, CellStyle header, CellStyle money) {
        row(sheet, 1, "Periodo", data.periodo().desde() + " al " + data.periodo().hasta());
        row(sheet, 2, "Sede", data.periodo().nombreSede());
        header(sheet, 3, header, "Fecha", "Operaciones", "Total");
        int index = 4;
        for (var item : data.ventasDiarias()) {
            Row row = sheet.createRow(index++);
            text(row, 0, item.fecha().toString()); number(row, 1, item.cantidadVentas()); money(row, 2, item.totalVentas(), money);
        }
        index += 2;
        header(sheet, index++, header, "Vendedor", "Usuario", "Operaciones", "Total");
        for (var item : data.ventasPorVendedor()) {
            Row row = sheet.createRow(index++);
            text(row, 0, item.nombreCompleto()); text(row, 1, item.usuarioLogin()); number(row, 2, item.cantidadVentas()); money(row, 3, item.totalVentas(), money);
        }
        index += 2;
        header(sheet, index++, header, "Código", "Producto", "Cantidad base", "Importe vendido");
        for (var item : data.productosMasVendidos()) {
            Row row = sheet.createRow(index++);
            text(row, 0, item.codigoInterno()); text(row, 1, item.nombreProducto()); decimal(row, 2, item.cantidadBaseVendida()); money(row, 3, item.subtotalVendido(), money);
        }
    }

    private void escribirInventarioExcel(Sheet sheet, ReporteInventarioResponse data, CellStyle header) {
        row(sheet, 1, "Sede", data.nombreSede());
        row(sheet, 2, "Resumen", data.resumen().productosActivos() + " activos · " + data.resumen().productosStockBajo() + " stock bajo · " + data.resumen().productosAgotados() + " agotados");
        header(sheet, 3, header, "Código", "Producto", "Unidad", "Físico", "Reservado", "Disponible", "Mínimo", "Estado");
        int index = 4;
        for (var item : data.productosStockBajo()) {
            Row row = sheet.createRow(index++);
            text(row, 0, item.codigoInterno()); text(row, 1, item.nombreProducto()); text(row, 2, item.unidadBase());
            decimal(row, 3, item.stockFisico()); decimal(row, 4, item.stockReservado()); decimal(row, 5, item.stockDisponible()); decimal(row, 6, item.stockMinimo()); text(row, 7, item.estadoStock());
        }
    }

    private void escribirFinanzasExcel(Sheet sheet, ReporteFinanzasResponse data, CellStyle header, CellStyle money) {
        row(sheet, 1, "Balance pendiente", data.balancePendiente().toPlainString());
        row(sheet, 2, "Generado", LocalDate.now().toString());
        header(sheet, 3, header, "Concepto", "Cuentas", "Saldo pendiente", "Vencidas", "Saldo vencido");
        escribirSaldo(sheet.createRow(4), "Cuentas por cobrar", data.cuentasCobrar(), money);
        escribirSaldo(sheet.createRow(5), "Cuentas por pagar", data.cuentasPagar(), money);
    }

    private void escribirSaldo(Row row, String label, ReporteFinanzasResponse.SaldoPendienteResponse data, CellStyle money) {
        text(row, 0, label); number(row, 1, data.cantidadCuentas()); money(row, 2, data.saldoPendiente(), money); number(row, 3, data.cantidadVencidas()); money(row, 4, data.saldoVencido(), money);
    }

    private void escribirCajaExcel(Sheet sheet, ReporteCajaResponse data, CellStyle header, CellStyle money) {
        row(sheet, 1, "Periodo", data.periodo().desde() + " al " + data.periodo().hasta());
        row(sheet, 2, "Sede", data.periodo().nombreSede());
        header(sheet, 3, header, "Código", "Método", "Ingresos", "Egresos", "Neto");
        int index = 4;
        for (var item : data.metodosPago()) {
            Row row = sheet.createRow(index++);
            text(row, 0, item.codigo()); text(row, 1, item.nombre()); money(row, 2, item.ingresos(), money); money(row, 3, item.egresos(), money); money(row, 4, item.neto(), money);
        }
    }

    private byte[] crearPdf(TipoReporte tipo, Object data) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            PdfWriter.getInstance(document, output);
            document.open();
            Paragraph title = new Paragraph("SGC PROVEPERÚ · " + titulo(tipo), PDF_TITLE);
            title.setSpacingAfter(4);
            document.add(title);
            Paragraph generated = new Paragraph("Reporte generado el " + LocalDate.now(), PDF_SUBTITLE);
            generated.setSpacingAfter(14);
            document.add(generated);
            switch (tipo) {
                case VENTAS -> pdfVentas(document, (ReporteVentasResponse) data);
                case INVENTARIO -> pdfInventario(document, (ReporteInventarioResponse) data);
                case FINANZAS -> pdfFinanzas(document, (ReporteFinanzasResponse) data);
                case CAJA -> pdfCaja(document, (ReporteCajaResponse) data);
            }
            document.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("No se pudo generar el archivo PDF", exception);
        }
    }

    private void pdfVentas(Document document, ReporteVentasResponse data) throws DocumentException {
        document.add(subtitle("Periodo: " + data.periodo().desde() + " al " + data.periodo().hasta() + " · " + data.periodo().nombreSede()));
        document.add(table(
            new String[] { "Fecha", "Operaciones", "Total" },
            data.ventasDiarias().stream().map(item -> new String[] { item.fecha().toString(), String.valueOf(item.cantidadVentas()), soles(item.totalVentas()) }).toList()
        ));
        document.add(section("Ventas por vendedor"));
        document.add(table(
            new String[] { "Vendedor", "Usuario", "Operaciones", "Total" },
            data.ventasPorVendedor().stream().map(item -> new String[] { item.nombreCompleto(), item.usuarioLogin(), String.valueOf(item.cantidadVentas()), soles(item.totalVentas()) }).toList()
        ));
        document.add(section("Productos más vendidos"));
        document.add(table(
            new String[] { "Código", "Producto", "Cantidad", "Importe" },
            data.productosMasVendidos().stream().map(item -> new String[] { item.codigoInterno(), item.nombreProducto(), item.cantidadBaseVendida().toPlainString(), soles(item.subtotalVendido()) }).toList()
        ));
    }

    private void pdfInventario(Document document, ReporteInventarioResponse data) throws DocumentException {
        document.add(subtitle("Sede: " + data.nombreSede() + " · " + data.resumen().productosStockBajo() + " productos con stock bajo"));
        document.add(table(
            new String[] { "Código", "Producto", "Unidad", "Físico", "Reservado", "Disponible", "Mínimo", "Estado" },
            data.productosStockBajo().stream().map(item -> new String[] { item.codigoInterno(), item.nombreProducto(), item.unidadBase(), item.stockFisico().toPlainString(), item.stockReservado().toPlainString(), item.stockDisponible().toPlainString(), item.stockMinimo().toPlainString(), item.estadoStock() }).toList()
        ));
    }

    private void pdfFinanzas(Document document, ReporteFinanzasResponse data) throws DocumentException {
        document.add(subtitle("Balance pendiente: " + soles(data.balancePendiente())));
        document.add(table(
            new String[] { "Concepto", "Cuentas", "Saldo pendiente", "Vencidas", "Saldo vencido" },
            List.of(
                saldoRow("Cuentas por cobrar", data.cuentasCobrar()),
                saldoRow("Cuentas por pagar", data.cuentasPagar())
            )
        ));
    }

    private void pdfCaja(Document document, ReporteCajaResponse data) throws DocumentException {
        document.add(subtitle("Periodo: " + data.periodo().desde() + " al " + data.periodo().hasta() + " · " + data.periodo().nombreSede()));
        document.add(table(
            new String[] { "Código", "Método", "Ingresos", "Egresos", "Neto" },
            data.metodosPago().stream().map(item -> new String[] { item.codigo(), item.nombre(), soles(item.ingresos()), soles(item.egresos()), soles(item.neto()) }).toList()
        ));
    }

    private String[] saldoRow(String label, ReporteFinanzasResponse.SaldoPendienteResponse data) {
        return new String[] { label, String.valueOf(data.cantidadCuentas()), soles(data.saldoPendiente()), String.valueOf(data.cantidadVencidas()), soles(data.saldoVencido()) };
    }

    private PdfPTable table(String[] headers, List<String[]> rows) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, PDF_HEADER));
            cell.setBackgroundColor(new Color(68, 101, 246));
            cell.setPadding(7);
            cell.setBorderColor(new Color(215, 222, 235));
            table.addCell(cell);
        }
        for (String[] row : rows) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, PDF_CELL));
                cell.setPadding(6);
                cell.setBorderColor(new Color(224, 229, 238));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }
        }
        return table;
    }

    private Paragraph subtitle(String value) {
        Paragraph paragraph = new Paragraph(value, PDF_SUBTITLE);
        paragraph.setSpacingAfter(10);
        return paragraph;
    }

    private Paragraph section(String value) {
        Paragraph paragraph = new Paragraph(value, new org.openpdf.text.Font(org.openpdf.text.Font.HELVETICA, 11, org.openpdf.text.Font.BOLD));
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(7);
        return paragraph;
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true); font.setFontHeightInPoints((short) 16); font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font); style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font); style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex()); style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN); style.setBorderTop(BorderStyle.THIN); style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("S/ #,##0.00"));
        return style;
    }

    private void header(Sheet sheet, int index, CellStyle style, String... values) {
        Row row = sheet.createRow(index);
        for (int column = 0; column < values.length; column++) {
            Cell cell = row.createCell(column); cell.setCellValue(values[column]); cell.setCellStyle(style);
        }
    }

    private void row(Sheet sheet, int index, String label, String value) {
        Row row = sheet.createRow(index); text(row, 0, label); text(row, 1, value);
    }

    private void text(Row row, int column, String value) { row.createCell(column).setCellValue(value == null ? "" : value); }
    private void number(Row row, int column, long value) { row.createCell(column).setCellValue(value); }
    private void decimal(Row row, int column, BigDecimal value) { row.createCell(column).setCellValue(value == null ? 0 : value.doubleValue()); }
    private void money(Row row, int column, BigDecimal value, CellStyle style) { Cell cell = row.createCell(column); cell.setCellValue(value == null ? 0 : value.doubleValue()); cell.setCellStyle(style); }
    private String soles(BigDecimal value) { return "S/ " + (value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()); }
    private String titulo(TipoReporte tipo) { return switch (tipo) { case VENTAS -> "Reporte de ventas"; case INVENTARIO -> "Reporte de inventario"; case FINANZAS -> "Reporte financiero"; case CAJA -> "Reporte de caja"; }; }

    private TipoReporte parseTipo(String value) {
        try { return TipoReporte.valueOf(value.trim().toUpperCase()); }
        catch (RuntimeException exception) { throw new SolicitudInvalidaException("Tipo de reporte inválido. Use VENTAS, INVENTARIO, FINANZAS o CAJA"); }
    }

    private FormatoReporte parseFormato(String value) {
        try { return FormatoReporte.valueOf(value.trim().toUpperCase()); }
        catch (RuntimeException exception) { throw new SolicitudInvalidaException("Formato inválido. Use XLSX o PDF"); }
    }

    private enum TipoReporte { VENTAS, INVENTARIO, FINANZAS, CAJA }
    private enum FormatoReporte { XLSX, PDF }
    public record ArchivoReporte(String nombre, String mediaType, byte[] contenido) { }
}
