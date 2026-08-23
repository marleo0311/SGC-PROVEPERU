package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

class GeneradorUblServiceTests {

    private final GeneradorUblService service = new GeneradorUblService();

    @Test
    void generaBoletaUblConPrecioFinalQueYaIncluyeIgv() {
        Comprobante comprobante = comprobante(TipoComprobanteVenta.BOLETA, null);

        String xml = new String(
            service.generar(comprobante, empresa()).xml(),
            StandardCharsets.UTF_8
        );

        assertThat(xml)
            .contains("<cbc:ProfileID")
            .contains(">0101</cbc:ProfileID>")
            .contains("<cbc:InvoiceTypeCode")
            .contains("listID=\"0101\"")
            .contains("listName=\"Tipo de Documento\"")
            .contains("listSchemeURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo51\"")
            .contains(">03</cbc:InvoiceTypeCode>")
            .contains("<cbc:ID>B001-00000001</cbc:ID>")
            .contains("<cbc:TaxableAmount currencyID=\"PEN\">118.64</cbc:TaxableAmount>")
            .contains("<cbc:TaxAmount currencyID=\"PEN\">21.36</cbc:TaxAmount>")
            .contains("<cbc:PayableAmount currencyID=\"PEN\">140.00</cbc:PayableAmount>")
            .contains("SON CIENTO CUARENTA CON 00/100 SOLES")
            .contains("<cac:PartyLegalEntity>")
            .contains(
                "<cbc:AddressTypeCode listAgencyName=\"PE:SUNAT\" "
                    + "listName=\"Establecimientos anexos\">0000</cbc:AddressTypeCode>"
            )
            .contains("unitCode=\"NIU\"");
    }

    @Test
    void generaCodigoDeLocalPrincipalAunqueNoExistanDireccionNiUbigeo() {
        Comprobante comprobante = comprobante(TipoComprobanteVenta.BOLETA, null);
        comprobante.getVenta().getSede().setDireccion(null);
        Empresa empresa = empresa();
        empresa.setDireccion(null);
        empresa.setUbigeo(null);

        String xml = new String(
            service.generar(comprobante, empresa).xml(),
            StandardCharsets.UTF_8
        );

        assertThat(xml)
            .containsSubsequence(
                "<cac:PartyLegalEntity>",
                "<cac:RegistrationAddress>",
                "<cbc:AddressTypeCode listAgencyName=\"PE:SUNAT\" "
                    + "listName=\"Establecimientos anexos\">0000</cbc:AddressTypeCode>",
                "</cac:RegistrationAddress>"
            );
    }

    @Test
    void rechazaFacturaSinClienteRuc() {
        Cliente cliente = cliente(TipoDocumentoCliente.DNI, "12345678");
        Comprobante comprobante = comprobante(TipoComprobanteVenta.FACTURA, cliente);

        assertThatThrownBy(() -> service.generar(comprobante, empresa()))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("RUC");
    }

    @Test
    void generaFacturaConClienteRuc() {
        Cliente cliente = cliente(TipoDocumentoCliente.RUC, "20512345678");
        cliente.setRazonSocial("CLIENTE DE PRUEBA S.A.C.");
        Comprobante comprobante = comprobante(TipoComprobanteVenta.FACTURA, cliente);
        comprobante.setSerie("F001");

        String xml = new String(
            service.generar(comprobante, empresa()).xml(),
            StandardCharsets.UTF_8
        );

        assertThat(xml)
            .contains(">01</cbc:InvoiceTypeCode>")
            .contains(">20512345678</cbc:ID>")
            .contains("CLIENTE DE PRUEBA S.A.C.");
    }

    private Comprobante comprobante(TipoComprobanteVenta tipo, Cliente cliente) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setCodigo("001");
        unidad.setCodigoSunat("NIU");
        unidad.setNombre("UNIDAD");
        Producto producto = new Producto();
        producto.setCodigoInterno("PROD-001");
        producto.setNombre("PRODUCTO DE PRUEBA");
        producto.setUnidadBase(unidad);
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidad);
        detalle.setCantidad(new BigDecimal("10.000"));
        detalle.setCantidadBase(new BigDecimal("10.000"));
        detalle.setPrecioUnitario(new BigDecimal("14.00"));
        detalle.setDescuento(BigDecimal.ZERO.setScale(2));
        detalle.setSubtotal(new BigDecimal("140.00"));
        Sede sede = new Sede();
        sede.setIdEmpresa(1L);
        sede.setNombre("Sede Principal");
        sede.setDireccion("AV. PRUEBA 123");
        sede.setCodigoEstablecimientoSunat("0000");
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setSede(sede);
        venta.setSubtotal(new BigDecimal("118.64"));
        venta.setIgv(new BigDecimal("21.36"));
        venta.setTotal(new BigDecimal("140.00"));
        venta.agregarDetalle(detalle);
        Comprobante comprobante = new Comprobante();
        comprobante.setVenta(venta);
        comprobante.setTipo(tipo);
        comprobante.setSerie("B001");
        comprobante.setNumero("00000001");
        comprobante.setFechaEmision(Instant.parse("2026-08-22T15:30:00Z"));
        comprobante.setSubtotal(venta.getSubtotal());
        comprobante.setIgv(venta.getIgv());
        comprobante.setTotal(venta.getTotal());
        return comprobante;
    }

    private Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setRuc("20612296911");
        empresa.setRazonSocial("INVERSIONES PROVEPERU S.R.L.");
        empresa.setNombreComercial("PROVEPERU");
        empresa.setDireccion("AV. PRUEBA 123");
        empresa.setUbigeo("150101");
        empresa.setDepartamento("LIMA");
        empresa.setProvincia("LIMA");
        empresa.setDistrito("LIMA");
        empresa.setCodigoPais("PE");
        return empresa;
    }

    private Cliente cliente(TipoDocumentoCliente tipo, String numero) {
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(tipo);
        cliente.setNumeroDocumento(numero);
        cliente.setNombres("CLIENTE");
        cliente.setApellidos("PRUEBA");
        return cliente;
    }
}
