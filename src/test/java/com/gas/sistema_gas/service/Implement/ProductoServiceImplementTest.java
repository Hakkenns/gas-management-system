package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.ProductoMapper;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.CategoriaRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.ProductoDTO;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductoServiceImplementTest {

    @Mock
    private ProductoMapper productoMapper;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private InventarioLoteRepository inventarioLoteRepository;

    @InjectMocks
    private ProductoServiceImplement productoService;

    private Producto crearProductoConCategoria(Long id) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Gases");

        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Gas 10kg");
        producto.setDescripcion("Balón de gas");
        producto.setEstado(1);
        producto.setCategoria(categoria);
        producto.setPrecioCompra(BigDecimal.ZERO);
        producto.setPrecioVenta(BigDecimal.ZERO);
        producto.setGananciaProducto(BigDecimal.TEN);
        producto.setStockLlenos(BigDecimal.ZERO);
        producto.setStockVacios(5);
        producto.setStockMinimo(BigDecimal.ONE);
        producto.setStockReservado(BigDecimal.ZERO);
        producto.setRequiereEnvase(true);
        return producto;
    }

    private ProductoDTO.SimpleResponse crearSimpleResponseBase(Producto p) {
        return new ProductoDTO.SimpleResponse(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getUrlImagen(),
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getCapacidad(),
                p.getUnidadMedida(),
                BigDecimal.ZERO,
                p.getGananciaProducto(),
                BigDecimal.ZERO,
                p.getRequiereEnvase() != null ? p.getRequiereEnvase() : false,
                p.getStockLlenos(),
                p.getStockVacios(),
                p.getStockMinimo(),
                p.getStockReservado(),
                p.getStockDisponible(),
                false,
                p.getEstado());
    }

    // PRUEBA 1: listAll usa la consulta PEPS de solo lectura y nunca la de bloqueo

    @Test
    void listAll_usaPepsLectura_yNuncaUsaPepsConBloqueo() {
        Producto producto = crearProductoConCategoria(1L);
        ProductoDTO.SimpleResponse base = crearSimpleResponseBase(producto);

        InventarioLote lote = new InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setPrecioCompra(BigDecimal.valueOf(100));
        lote.setPrecioVenta(BigDecimal.valueOf(120));
        lote.setCantidadActual(BigDecimal.valueOf(50));

        when(productoRepository.findAll()).thenReturn(List.of(producto));
        when(productoMapper.toSimpleResponse(producto)).thenReturn(base);
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L))
                .thenReturn(BigDecimal.valueOf(50));
        when(inventarioLoteRepository.findLotesDisponiblesPEPSLectura(1L))
                .thenReturn(List.of(lote));

        List<ProductoDTO.SimpleResponse> respuesta = productoService.listAll();

        assertEquals(1, respuesta.size());
        ProductoDTO.SimpleResponse item = respuesta.get(0);
        // El stock devuelto coincide con el valor de la consulta SUM
        assertEquals(BigDecimal.valueOf(50), item.stockLlenos());
        // Los precios provienen del lote PEPS de lectura
        assertEquals(BigDecimal.valueOf(100), item.precioCompra());
        assertEquals(BigDecimal.valueOf(120), item.precioVenta());
        assertEquals(BigDecimal.valueOf(20), item.gananciaProducto());

        verify(inventarioLoteRepository).sumCantidadActualByProductoId(1L);
        verify(inventarioLoteRepository).findLotesDisponiblesPEPSLectura(1L);
        // Nunca se usa la consulta PEPS con bloqueo pesimista en listAll
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
    }

    // PRUEBA 2: deleteProduct rechaza eliminación si el producto tiene historial de lotes

    @Test
    void deleteProduct_conHistorialDeLotesAgotados_rechazaEliminacion() {
        Producto producto = crearProductoConCategoria(1L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioLoteRepository.existsByProducto_Id(1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productoService.deleteProduct(1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        // Nunca se guarda el producto
        verify(productoRepository, never()).save(any(Producto.class));
        // No se carga ninguna lista de lotes
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPSLectura(any());
        verify(inventarioLoteRepository, never()).findHistorialCompletoByProductoIdOrderByCreatedAtDesc(any());
    }

    // PRUEBA 3: listAll sin lote activo usa el último lote del historial DESC

    @Test
    void listAll_sinLoteActivo_usaUltimoHistorialDesc() {
        Producto producto = crearProductoConCategoria(1L);
        ProductoDTO.SimpleResponse base = crearSimpleResponseBase(producto);

        // Historial DESC: el primer elemento es el lote más reciente
        InventarioLote loteReciente = new InventarioLote();
        loteReciente.setId(2L);
        loteReciente.setProducto(producto);
        loteReciente.setPrecioCompra(BigDecimal.valueOf(90));
        loteReciente.setPrecioVenta(BigDecimal.valueOf(115));
        loteReciente.setCantidadActual(BigDecimal.ZERO);

        InventarioLote loteAntiguo = new InventarioLote();
        loteAntiguo.setId(1L);
        loteAntiguo.setProducto(producto);
        loteAntiguo.setPrecioCompra(BigDecimal.valueOf(80));
        loteAntiguo.setPrecioVenta(BigDecimal.valueOf(110));
        loteAntiguo.setCantidadActual(BigDecimal.ZERO);

        when(productoRepository.findAll()).thenReturn(List.of(producto));
        when(productoMapper.toSimpleResponse(producto)).thenReturn(base);
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L))
                .thenReturn(BigDecimal.ZERO);
        when(inventarioLoteRepository.findLotesDisponiblesPEPSLectura(1L))
                .thenReturn(List.of());
        when(inventarioLoteRepository.findHistorialCompletoByProductoIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(loteReciente, loteAntiguo));

        List<ProductoDTO.SimpleResponse> respuesta = productoService.listAll();

        assertEquals(1, respuesta.size());
        ProductoDTO.SimpleResponse item = respuesta.get(0);
        // La respuesta utiliza los precios históricos más recientes (primer lote del historial DESC)
        assertEquals(BigDecimal.valueOf(90), item.precioCompra());
        assertEquals(BigDecimal.valueOf(115), item.precioVenta());
        assertEquals(BigDecimal.valueOf(25), item.gananciaProducto());

        verify(inventarioLoteRepository).findLotesDisponiblesPEPSLectura(1L);
        verify(inventarioLoteRepository).findHistorialCompletoByProductoIdOrderByCreatedAtDesc(1L);
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
    }
}