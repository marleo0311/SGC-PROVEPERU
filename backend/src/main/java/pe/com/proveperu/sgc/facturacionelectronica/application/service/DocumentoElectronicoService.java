package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoFirmado;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoUbl;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.security.FirmaDigitalService;

@Service
public class DocumentoElectronicoService {

    private final GeneradorUblService generadorUblService;
    private final FirmaDigitalService firmaDigitalService;

    public DocumentoElectronicoService(
        GeneradorUblService generadorUblService,
        FirmaDigitalService firmaDigitalService
    ) {
        this.generadorUblService = generadorUblService;
        this.firmaDigitalService = firmaDigitalService;
    }

    public DocumentoFirmado preparar(Comprobante comprobante, Empresa empresa) {
        DocumentoUbl ubl = generadorUblService.generar(comprobante, empresa);
        byte[] firmado = firmaDigitalService.firmar(ubl.xml());
        return new DocumentoFirmado(
            ubl.nombreBase(),
            firmado,
            comprimir(ubl.nombreXml(), firmado),
            sha256(firmado)
        );
    }

    private byte[] comprimir(String nombreXml, byte[] xml) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                ZipEntry entry = new ZipEntry(nombreXml);
                zip.putNextEntry(entry);
                zip.write(xml);
                zip.closeEntry();
            }
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo comprimir el XML firmado", exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo calcular el hash del XML", exception);
        }
    }
}
