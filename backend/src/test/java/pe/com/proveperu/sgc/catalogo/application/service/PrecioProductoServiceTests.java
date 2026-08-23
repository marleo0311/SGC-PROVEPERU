package pe.com.proveperu.sgc.catalogo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class PrecioProductoServiceTests {

    @Mock
    private PrecioProductoRepository precioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private PrecioProductoService service;

    @Test
    void creaNuevaVigenciaYCierraElPrecioAnterior() {
        LocalDate hoy = LocalDate.of(2026, 8, 23);
        Producto producto = productoActivo();
        PrecioProducto anterior = precio(producto, "MINORISTA", "100.00", hoy.minusDays(30));
        when(precioRepository.buscarVigentes(1L, "MINORISTA", hoy, EstadoCatalogo.ACTIVO))
            .thenReturn(List.of(anterior));
        when(precioRepository.buscarSolapados(1L, "MINORISTA", hoy, null, EstadoCatalogo.ACTIVO))
            .thenReturn(List.of(anterior));

        service.actualizarPrecioVigente(producto, "MINORISTA", new BigDecimal("125.00"), hoy);

        assertThat(anterior.getVigenteHasta()).isEqualTo(hoy.minusDays(1));
        ArgumentCaptor<PrecioProducto> captor = ArgumentCaptor.forClass(PrecioProducto.class);
        verify(precioRepository).save(captor.capture());
        assertThat(captor.getValue())
            .extracting(
                PrecioProducto::getProducto,
                PrecioProducto::getTipoPrecio,
                PrecioProducto::getMonto,
                PrecioProducto::getVigenteDesde,
                PrecioProducto::getVigenteHasta,
                PrecioProducto::getEstado
            )
            .containsExactly(
                producto,
                "MINORISTA",
                new BigDecimal("125.00"),
                hoy,
                null,
                EstadoCatalogo.ACTIVO
            );
    }

    @Test
    void corrigeLaVigenciaDelMismoDiaSinDuplicarla() {
        LocalDate hoy = LocalDate.of(2026, 8, 23);
        Producto producto = productoActivo();
        PrecioProducto actual = precio(producto, "MAYORISTA", "110.00", hoy);
        when(precioRepository.buscarVigentes(1L, "MAYORISTA", hoy, EstadoCatalogo.ACTIVO))
            .thenReturn(List.of(actual));

        service.actualizarPrecioVigente(producto, "MAYORISTA", new BigDecimal("115.00"), hoy);

        assertThat(actual.getMonto()).isEqualByComparingTo("115.00");
        verify(precioRepository, never()).save(any(PrecioProducto.class));
    }

    private Producto productoActivo() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setEstado(EstadoCatalogo.ACTIVO);
        return producto;
    }

    private PrecioProducto precio(
        Producto producto,
        String tipo,
        String monto,
        LocalDate vigenteDesde
    ) {
        PrecioProducto precio = new PrecioProducto();
        precio.setProducto(producto);
        precio.setTipoPrecio(tipo);
        precio.setMonto(new BigDecimal(monto));
        precio.setVigenteDesde(vigenteDesde);
        precio.setEstado(EstadoCatalogo.ACTIVO);
        return precio;
    }
}
