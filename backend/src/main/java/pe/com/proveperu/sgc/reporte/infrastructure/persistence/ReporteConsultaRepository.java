package pe.com.proveperu.sgc.reporte.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteCajaResponse.CajaMetodoPagoResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteCajaResponse.ResumenCajaResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteFinanzasResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteFinanzasResponse.SaldoPendienteResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteInventarioResponse.ProductoStockBajoResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteInventarioResponse.ResumenInventarioResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse.ProductoVendidoResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse.ResumenVentasResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse.VentaDiariaResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse.VentaVendedorResponse;

@Repository
@RequiredArgsConstructor
public class ReporteConsultaRepository {

    private static final BigDecimal CERO_DINERO = new BigDecimal("0.00");
    private static final BigDecimal CERO_CANTIDAD = new BigDecimal("0.000");

    private final NamedParameterJdbcTemplate jdbc;

    public ResumenVentasResponse consultarResumenVentas(
        Instant desde,
        Instant hastaExclusivo,
        Long idSede
    ) {
        String sql = """
            SELECT
                count(*) AS cantidad_ventas,
                coalesce(sum(v.subtotal), 0) AS subtotal,
                coalesce(sum(v.igv), 0) AS igv,
                coalesce(sum(v.descuento_total), 0) AS descuentos,
                coalesce(sum(v.total), 0) AS total_ventas,
                round(coalesce(avg(v.total), 0), 2) AS ticket_promedio
            FROM venta v
            WHERE v.estado <> 'ANULADA'
              AND v.id_sede = :idSede
              AND v.fecha_hora >= :desde
              AND v.fecha_hora < :hasta
            """;
        return Objects.requireNonNull(jdbc.queryForObject(
            sql,
            parametrosPeriodo(desde, hastaExclusivo, idSede),
            (rs, rowNum) -> new ResumenVentasResponse(
                rs.getLong("cantidad_ventas"),
                dinero(rs, "subtotal"),
                dinero(rs, "igv"),
                dinero(rs, "descuentos"),
                dinero(rs, "total_ventas"),
                dinero(rs, "ticket_promedio")
            )
        ));
    }

    public List<VentaDiariaResponse> consultarVentasDiarias(
        Instant desde,
        Instant hastaExclusivo,
        Long idSede
    ) {
        String sql = """
            SELECT
                (v.fecha_hora AT TIME ZONE 'America/Lima')::date AS fecha,
                count(*) AS cantidad_ventas,
                coalesce(sum(v.total), 0) AS total_ventas
            FROM venta v
            WHERE v.estado <> 'ANULADA'
              AND v.id_sede = :idSede
              AND v.fecha_hora >= :desde
              AND v.fecha_hora < :hasta
            GROUP BY (v.fecha_hora AT TIME ZONE 'America/Lima')::date
            ORDER BY fecha ASC
            """;
        return jdbc.query(
            sql,
            parametrosPeriodo(desde, hastaExclusivo, idSede),
            (rs, rowNum) -> new VentaDiariaResponse(
                rs.getObject("fecha", LocalDate.class),
                rs.getLong("cantidad_ventas"),
                dinero(rs, "total_ventas")
            )
        );
    }

    public List<VentaVendedorResponse> consultarVentasPorVendedor(
        Instant desde,
        Instant hastaExclusivo,
        Long idSede,
        int limite
    ) {
        String sql = """
            SELECT
                u.id_usuario AS id_vendedor,
                u.usuario_login,
                u.nombre_completo,
                count(*) AS cantidad_ventas,
                coalesce(sum(v.total), 0) AS total_ventas
            FROM venta v
            JOIN usuario u ON u.id_usuario = v.id_vendedor
            WHERE v.estado <> 'ANULADA'
              AND v.id_sede = :idSede
              AND v.fecha_hora >= :desde
              AND v.fecha_hora < :hasta
            GROUP BY u.id_usuario, u.usuario_login, u.nombre_completo
            ORDER BY total_ventas DESC, cantidad_ventas DESC, u.id_usuario ASC
            LIMIT :limite
            """;
        MapSqlParameterSource parametros = parametrosPeriodo(
            desde,
            hastaExclusivo,
            idSede
        ).addValue("limite", limite);
        return jdbc.query(
            sql,
            parametros,
            (rs, rowNum) -> new VentaVendedorResponse(
                rs.getLong("id_vendedor"),
                rs.getString("usuario_login"),
                rs.getString("nombre_completo"),
                rs.getLong("cantidad_ventas"),
                dinero(rs, "total_ventas")
            )
        );
    }

    public List<ProductoVendidoResponse> consultarProductosMasVendidos(
        Instant desde,
        Instant hastaExclusivo,
        Long idSede,
        int limite
    ) {
        String sql = """
            SELECT
                p.id_producto,
                p.codigo_interno,
                p.nombre AS nombre_producto,
                coalesce(sum(dv.cantidad_base), 0) AS cantidad_base_vendida,
                coalesce(sum(dv.subtotal), 0) AS subtotal_vendido
            FROM detalle_venta dv
            JOIN venta v ON v.id_venta = dv.id_venta
            JOIN producto p ON p.id_producto = dv.id_producto
            WHERE v.estado <> 'ANULADA'
              AND v.id_sede = :idSede
              AND v.fecha_hora >= :desde
              AND v.fecha_hora < :hasta
            GROUP BY p.id_producto, p.codigo_interno, p.nombre
            ORDER BY cantidad_base_vendida DESC, subtotal_vendido DESC,
                p.id_producto ASC
            LIMIT :limite
            """;
        MapSqlParameterSource parametros = parametrosPeriodo(
            desde,
            hastaExclusivo,
            idSede
        ).addValue("limite", limite);
        return jdbc.query(
            sql,
            parametros,
            (rs, rowNum) -> new ProductoVendidoResponse(
                rs.getLong("id_producto"),
                rs.getString("codigo_interno"),
                rs.getString("nombre_producto"),
                cantidad(rs, "cantidad_base_vendida"),
                dinero(rs, "subtotal_vendido")
            )
        );
    }

    public ResumenInventarioResponse consultarResumenInventario(Long idSede) {
        String sql = """
            SELECT
                count(*) AS productos_activos,
                count(*) FILTER (
                    WHERE coalesce(i.stock_fisico, 0)
                        - coalesce(i.stock_reservado, 0)
                        <= coalesce(i.stock_minimo, p.stock_minimo)
                ) AS productos_stock_bajo,
                count(*) FILTER (
                    WHERE coalesce(i.stock_fisico, 0)
                        - coalesce(i.stock_reservado, 0) <= 0
                ) AS productos_agotados
            FROM producto p
            LEFT JOIN inventario i
              ON i.id_producto = p.id_producto
             AND i.id_sede = :idSede
            WHERE p.estado = 'ACTIVO'
            """;
        return Objects.requireNonNull(jdbc.queryForObject(
            sql,
            new MapSqlParameterSource("idSede", idSede),
            (rs, rowNum) -> new ResumenInventarioResponse(
                rs.getLong("productos_activos"),
                rs.getLong("productos_stock_bajo"),
                rs.getLong("productos_agotados")
            )
        ));
    }

    public List<ProductoStockBajoResponse> consultarProductosStockBajo(
        Long idSede,
        int limite
    ) {
        String sql = """
            SELECT
                p.id_producto,
                p.codigo_interno,
                p.nombre AS nombre_producto,
                um.codigo AS unidad_base,
                coalesce(i.stock_fisico, 0) AS stock_fisico,
                coalesce(i.stock_reservado, 0) AS stock_reservado,
                coalesce(i.stock_fisico, 0)
                    - coalesce(i.stock_reservado, 0) AS stock_disponible,
                coalesce(i.stock_minimo, p.stock_minimo) AS stock_minimo,
                CASE
                    WHEN coalesce(i.stock_fisico, 0)
                        - coalesce(i.stock_reservado, 0) <= 0 THEN 'AGOTADO'
                    ELSE 'BAJO'
                END AS estado_stock
            FROM producto p
            JOIN unidad_medida um ON um.id_unidad_medida = p.id_unidad_base
            LEFT JOIN inventario i
              ON i.id_producto = p.id_producto
             AND i.id_sede = :idSede
            WHERE p.estado = 'ACTIVO'
              AND coalesce(i.stock_fisico, 0)
                    - coalesce(i.stock_reservado, 0)
                    <= coalesce(i.stock_minimo, p.stock_minimo)
            ORDER BY stock_disponible ASC, p.nombre ASC, p.id_producto ASC
            LIMIT :limite
            """;
        MapSqlParameterSource parametros = new MapSqlParameterSource()
            .addValue("idSede", idSede)
            .addValue("limite", limite);
        return jdbc.query(
            sql,
            parametros,
            (rs, rowNum) -> new ProductoStockBajoResponse(
                rs.getLong("id_producto"),
                rs.getString("codigo_interno"),
                rs.getString("nombre_producto"),
                rs.getString("unidad_base"),
                cantidad(rs, "stock_fisico"),
                cantidad(rs, "stock_reservado"),
                cantidad(rs, "stock_disponible"),
                cantidad(rs, "stock_minimo"),
                rs.getString("estado_stock")
            )
        );
    }

    public ReporteFinanzasResponse consultarFinanzas(LocalDate hoy) {
        SaldoPendienteResponse cuentasCobrar = consultarSaldoPendiente(
            "cuenta_cobrar",
            hoy
        );
        SaldoPendienteResponse cuentasPagar = consultarSaldoPendiente(
            "cuenta_pagar",
            hoy
        );
        return new ReporteFinanzasResponse(
            cuentasCobrar,
            cuentasPagar,
            cuentasCobrar.saldoPendiente().subtract(
                cuentasPagar.saldoPendiente()
            )
        );
    }

    public ResumenCajaResponse consultarResumenCaja(
        Instant desde,
        Instant hastaExclusivo,
        Long idSede
    ) {
        String sql = """
            SELECT
                count(*) AS cantidad_movimientos,
                coalesce(sum(CASE WHEN mc.tipo = 'INGRESO'
                    THEN mc.importe ELSE 0 END), 0) AS total_ingresos,
                coalesce(sum(CASE WHEN mc.tipo = 'EGRESO'
                    THEN mc.importe ELSE 0 END), 0) AS total_egresos
            FROM movimiento_caja mc
            JOIN sesion_caja sc ON sc.id_sesion_caja = mc.id_sesion_caja
            JOIN caja c ON c.id_caja = sc.id_caja
            WHERE c.id_sede = :idSede
              AND mc.fecha_hora >= :desde
              AND mc.fecha_hora < :hasta
            """;
        return Objects.requireNonNull(jdbc.queryForObject(
            sql,
            parametrosPeriodo(desde, hastaExclusivo, idSede),
            (rs, rowNum) -> {
                BigDecimal ingresos = dinero(rs, "total_ingresos");
                BigDecimal egresos = dinero(rs, "total_egresos");
                return new ResumenCajaResponse(
                    rs.getLong("cantidad_movimientos"),
                    ingresos,
                    egresos,
                    ingresos.subtract(egresos)
                );
            }
        ));
    }

    public List<CajaMetodoPagoResponse> consultarCajaPorMetodoPago(
        Instant desde,
        Instant hastaExclusivo,
        Long idSede
    ) {
        String sql = """
            SELECT
                mp.id_metodo_pago,
                mp.codigo,
                mp.nombre,
                coalesce(sum(CASE WHEN mc.tipo = 'INGRESO'
                    THEN mc.importe ELSE 0 END), 0) AS ingresos,
                coalesce(sum(CASE WHEN mc.tipo = 'EGRESO'
                    THEN mc.importe ELSE 0 END), 0) AS egresos
            FROM movimiento_caja mc
            JOIN sesion_caja sc ON sc.id_sesion_caja = mc.id_sesion_caja
            JOIN caja c ON c.id_caja = sc.id_caja
            JOIN metodo_pago mp ON mp.id_metodo_pago = mc.id_metodo_pago
            WHERE c.id_sede = :idSede
              AND mc.fecha_hora >= :desde
              AND mc.fecha_hora < :hasta
            GROUP BY mp.id_metodo_pago, mp.codigo, mp.nombre
            ORDER BY mp.nombre ASC, mp.id_metodo_pago ASC
            """;
        return jdbc.query(
            sql,
            parametrosPeriodo(desde, hastaExclusivo, idSede),
            (rs, rowNum) -> {
                BigDecimal ingresos = dinero(rs, "ingresos");
                BigDecimal egresos = dinero(rs, "egresos");
                return new CajaMetodoPagoResponse(
                    rs.getLong("id_metodo_pago"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    ingresos,
                    egresos,
                    ingresos.subtract(egresos)
                );
            }
        );
    }

    private SaldoPendienteResponse consultarSaldoPendiente(
        String tabla,
        LocalDate hoy
    ) {
        String sql = """
            SELECT
                count(*) AS cantidad_cuentas,
                coalesce(sum(saldo_pendiente), 0) AS saldo_pendiente,
                count(*) FILTER (
                    WHERE fecha_vencimiento IS NOT NULL
                      AND fecha_vencimiento < :hoy
                ) AS cantidad_vencidas,
                coalesce(sum(saldo_pendiente) FILTER (
                    WHERE fecha_vencimiento IS NOT NULL
                      AND fecha_vencimiento < :hoy
                ), 0) AS saldo_vencido
            FROM %s
            WHERE saldo_pendiente > 0
              AND estado <> 'ANULADO'
            """.formatted(tabla);
        return Objects.requireNonNull(jdbc.queryForObject(
            sql,
            new MapSqlParameterSource("hoy", hoy),
            (rs, rowNum) -> new SaldoPendienteResponse(
                rs.getLong("cantidad_cuentas"),
                dinero(rs, "saldo_pendiente"),
                rs.getLong("cantidad_vencidas"),
                dinero(rs, "saldo_vencido")
            )
        ));
    }

    private MapSqlParameterSource parametrosPeriodo(
        Instant desde,
        Instant hastaExclusivo,
        Long idSede
    ) {
        return new MapSqlParameterSource()
            .addValue("desde", OffsetDateTime.ofInstant(desde, ZoneOffset.UTC))
            .addValue(
                "hasta",
                OffsetDateTime.ofInstant(hastaExclusivo, ZoneOffset.UTC)
            )
            .addValue("idSede", idSede);
    }

    private BigDecimal dinero(ResultSet rs, String columna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor == null ? CERO_DINERO : valor;
    }

    private BigDecimal cantidad(ResultSet rs, String columna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor == null ? CERO_CANTIDAD : valor;
    }
}
