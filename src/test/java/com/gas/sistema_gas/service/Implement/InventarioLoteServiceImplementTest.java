package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.gas.sistema_gas.Model.AsignacionLotePedido;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Repository.AsignacionLotePedidoRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.ProveedorRepository;

import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventarioLoteServiceImplementTest {

    @Mock
    private InventarioLoteRepository inventarioLoteRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private AsignacionLotePedidoRepository asignacionLotePedidoRepository;

    @InjectMocks
    private InventarioLoteServiceImplement inventarioLoteService;

    // PRUEBA 1: descontarStockPorPEPS un solo lote crea asignación exacta

    @Test
    void descontarStockPorPEPS_unSoloLote_creaAsignacionExacta() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setProducto(producto);
        detalle.setCantidad(4);
        detalle.setPedido(pedido);

        InventarioLote lote = new InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(10));

        when(inventarioLoteRepository.findLotesDisponiblesPEPS(1L)).thenReturn(List.of(lote));
        when(asignacionLotePedidoRepository.existsByIdDetallePedidoAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(false);
        when(inventarioLoteRepository.save(any(InventarioLote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionLotePedidoRepository.save(any(AsignacionLotePedido.class))).thenAnswer(inv -> inv.getArgument(0));

        inventarioLoteService.descontarStockPorPEPS(detalle);

        assertEquals(BigDecimal.valueOf(6), lote.getCantidadActual());
        verify(inventarioLoteRepository).save(lote);
        verify(asignacionLotePedidoRepository).save(any(AsignacionLotePedido.class));
    }

    // PRUEBA 2: descontarStockPorPEPS varios lotes divide asignaciones

    @Test
    void descontarStockPorPEPS_variosLotes_divideAsignaciones() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        detalle.setPedido(pedido);

        InventarioLote lote1 = new InventarioLote();
        lote1.setId(1L);
        lote1.setProducto(producto);
        lote1.setCantidadActual(BigDecimal.valueOf(3));

        InventarioLote lote2 = new InventarioLote();
        lote2.setId(2L);
        lote2.setProducto(producto);
        lote2.setCantidadActual(BigDecimal.valueOf(7));

        when(inventarioLoteRepository.findLotesDisponiblesPEPS(1L)).thenReturn(List.of(lote1, lote2));
        when(asignacionLotePedidoRepository.existsByIdDetallePedidoAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(false);
        when(inventarioLoteRepository.save(any(InventarioLote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionLotePedidoRepository.save(any(AsignacionLotePedido.class))).thenAnswer(inv -> inv.getArgument(0));

        inventarioLoteService.descontarStockPorPEPS(detalle);

        assertEquals(BigDecimal.ZERO, lote1.getCantidadActual());
        assertEquals(BigDecimal.valueOf(5), lote2.getCantidadActual());
        verify(inventarioLoteRepository, times(2)).save(any(InventarioLote.class));
        verify(asignacionLotePedidoRepository, times(2)).save(any(AsignacionLotePedido.class));
    }

    // PRUEBA 3: descontarStockPorPEPS stock insuficiente no modifica nada

    @Test
    void descontarStockPorPEPS_stockInsuficiente_noModificaNada() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        detalle.setPedido(pedido);

        InventarioLote lote = new InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(4));

        when(inventarioLoteRepository.findLotesDisponiblesPEPS(1L)).thenReturn(List.of(lote));
        when(asignacionLotePedidoRepository.existsByIdDetallePedidoAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.descontarStockPorPEPS(detalle)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Stock insuficiente en los lotes para cubrir la venta.", exception.getReason());
        verify(inventarioLoteRepository, never()).save(any(InventarioLote.class));
        verify(asignacionLotePedidoRepository, never()).save(any(AsignacionLotePedido.class));
    }

    // PRUEBA 4: descontarStockPorPEPS asignación activa lanza CONFLICT

    @Test
    void descontarStockPorPEPS_asignacionActiva_lanzaConflict() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        detalle.setPedido(pedido);

        when(asignacionLotePedidoRepository.existsByIdDetallePedidoAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.descontarStockPorPEPS(detalle)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El detalle del pedido ya tiene una asignación de lotes activa", exception.getReason());
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
        verify(inventarioLoteRepository, never()).save(any(InventarioLote.class));
        verify(asignacionLotePedidoRepository, never()).save(any(AsignacionLotePedido.class));
    }

    // PRUEBA 5: descontarStockPorPEPS rechaza detalle sin id

    @Test
    void descontarStockPorPEPS_rechazaDetalleSinId() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(null);
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        detalle.setPedido(pedido);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.descontarStockPorPEPS(detalle)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El detalle del pedido debe estar guardado antes de descontar stock", exception.getReason());
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
    }

    // PRUEBA 6: descontarStockPorPEPS rechaza cantidad inválida

    @Test
    void descontarStockPorPEPS_rechazaCantidadInvalida() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setProducto(producto);
        detalle.setCantidad(0);
        detalle.setPedido(pedido);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.descontarStockPorPEPS(detalle)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("La cantidad del detalle debe ser mayor a cero", exception.getReason());
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
    }

    // PRUEBA 7: descontarStockPorPEPS rechaza detalle nulo

    @Test
    void descontarStockPorPEPS_rechazaDetalleNulo() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.descontarStockPorPEPS(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El detalle del pedido no puede ser nulo", exception.getReason());
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
    }

    // PRUEBA 8: descontarStockPorPEPS rechaza producto nulo

    @Test
    void descontarStockPorPEPS_rechazaProductoNulo() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setProducto(null);
        detalle.setCantidad(5);
        detalle.setPedido(pedido);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.descontarStockPorPEPS(detalle)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El producto del detalle no es válido", exception.getReason());
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
    }

    // PRUEBA 9: descontarStockPorPEPS rechaza pedido nulo

    @Test
    void descontarStockPorPEPS_rechazaPedidoNulo() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        detalle.setPedido(null);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.descontarStockPorPEPS(detalle)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El pedido del detalle no es válido", exception.getReason());
        verify(inventarioLoteRepository, never()).findLotesDisponiblesPEPS(any());
    }

    // =========================================================================
    // PRUEBAS FASE 2A: devolverStockDePedido
    // =========================================================================

    // PRUEBA 10: devolverStockDePedido rechaza ID nulo

    @Test
    void devolverStockDePedido_rechazaIdNulo() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.devolverStockDePedido(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El ID del pedido es obligatorio", exception.getReason());
        verify(asignacionLotePedidoRepository, never()).findByPedidoIdAndEstadoForUpdate(any(), any());
    }

    // PRUEBA 11: devolverStockDePedido a un solo lote

    @Test
    void devolverStockDePedido_unLote_devuelveStockCorrectamente() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");
        producto.setStockLlenos(BigDecimal.valueOf(6));

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        InventarioLote lote = new InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(6));

        AsignacionLotePedido asignacion = new AsignacionLotePedido();
        asignacion.setIdAsignacion(1L);
        asignacion.setPedido(pedido);
        asignacion.setProducto(producto);
        asignacion.setLote(lote);
        asignacion.setCantidadDescontada(BigDecimal.valueOf(4));
        asignacion.setCantidadDevuelta(BigDecimal.ZERO);
        asignacion.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of(asignacion));
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DEVUELTA))
            .thenReturn(false);
        when(inventarioLoteRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of(lote));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of(producto));
        when(inventarioLoteRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L))
            .thenReturn(BigDecimal.valueOf(10));

        inventarioLoteService.devolverStockDePedido(1L);

        assertEquals(BigDecimal.valueOf(10), lote.getCantidadActual());
        assertEquals(AsignacionLotePedido.EstadoAsignacion.DEVUELTA, asignacion.getEstado());
        assertEquals(BigDecimal.valueOf(4), asignacion.getCantidadDevuelta());
        verify(inventarioLoteRepository).saveAll(any());
        verify(productoRepository).saveAll(any());
    }

    // PRUEBA 12: devolverStockDePedido a varios lotes

    @Test
    void devolverStockDePedido_variosLotes_devuelveStockCorrectamente() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");
        producto.setStockLlenos(BigDecimal.valueOf(3));

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        InventarioLote lote1 = new InventarioLote();
        lote1.setId(1L);
        lote1.setProducto(producto);
        lote1.setCantidadActual(BigDecimal.valueOf(3));

        InventarioLote lote2 = new InventarioLote();
        lote2.setId(2L);
        lote2.setProducto(producto);
        lote2.setCantidadActual(BigDecimal.valueOf(2));

        AsignacionLotePedido asignacion1 = new AsignacionLotePedido();
        asignacion1.setIdAsignacion(1L);
        asignacion1.setPedido(pedido);
        asignacion1.setProducto(producto);
        asignacion1.setLote(lote1);
        asignacion1.setCantidadDescontada(BigDecimal.valueOf(3));
        asignacion1.setCantidadDevuelta(BigDecimal.ZERO);
        asignacion1.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        AsignacionLotePedido asignacion2 = new AsignacionLotePedido();
        asignacion2.setIdAsignacion(2L);
        asignacion2.setPedido(pedido);
        asignacion2.setProducto(producto);
        asignacion2.setLote(lote2);
        asignacion2.setCantidadDescontada(BigDecimal.valueOf(2));
        asignacion2.setCantidadDevuelta(BigDecimal.ZERO);
        asignacion2.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of(asignacion1, asignacion2));
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DEVUELTA))
            .thenReturn(false);
        when(inventarioLoteRepository.findAllByIdInForUpdate(List.of(1L, 2L)))
            .thenReturn(List.of(lote1, lote2));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of(producto));
        when(inventarioLoteRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L))
            .thenReturn(BigDecimal.valueOf(10));

        inventarioLoteService.devolverStockDePedido(1L);

        assertEquals(BigDecimal.valueOf(6), lote1.getCantidadActual());
        assertEquals(BigDecimal.valueOf(4), lote2.getCantidadActual());
        assertEquals(AsignacionLotePedido.EstadoAsignacion.DEVUELTA, asignacion1.getEstado());
        assertEquals(AsignacionLotePedido.EstadoAsignacion.DEVUELTA, asignacion2.getEstado());
    }

    // PRUEBA 13: devolverStockDePedido varias asignaciones del mismo lote

    @Test
    void devolverStockDePedido_variasAsignacionesMismoLote_devuelveStockCorrectamente() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        InventarioLote lote = new InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(5));

        AsignacionLotePedido asignacion1 = new AsignacionLotePedido();
        asignacion1.setIdAsignacion(1L);
        asignacion1.setPedido(pedido);
        asignacion1.setProducto(producto);
        asignacion1.setLote(lote);
        asignacion1.setCantidadDescontada(BigDecimal.valueOf(3));
        asignacion1.setCantidadDevuelta(BigDecimal.ZERO);
        asignacion1.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        AsignacionLotePedido asignacion2 = new AsignacionLotePedido();
        asignacion2.setIdAsignacion(2L);
        asignacion2.setPedido(pedido);
        asignacion2.setProducto(producto);
        asignacion2.setLote(lote);
        asignacion2.setCantidadDescontada(BigDecimal.valueOf(2));
        asignacion2.setCantidadDevuelta(BigDecimal.ZERO);
        asignacion2.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of(asignacion1, asignacion2));
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DEVUELTA))
            .thenReturn(false);
        when(inventarioLoteRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of(lote));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of(producto));
        when(inventarioLoteRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L))
            .thenReturn(BigDecimal.valueOf(10));

        inventarioLoteService.devolverStockDePedido(1L);

        // El lote debe recibir 3 + 2 = 5 unidades
        assertEquals(BigDecimal.valueOf(10), lote.getCantidadActual());
        assertEquals(AsignacionLotePedido.EstadoAsignacion.DEVUELTA, asignacion1.getEstado());
        assertEquals(AsignacionLotePedido.EstadoAsignacion.DEVUELTA, asignacion2.getEstado());
    }

    // PRUEBA 14: devolverStockDePedido parcialmente procesada (cantidadDevuelta > 0)

    @Test
    void devolverStockDePedido_parcialmenteProcesada_devuelveSoloFaltante() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        InventarioLote lote = new InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(2));

        AsignacionLotePedido asignacion = new AsignacionLotePedido();
        asignacion.setIdAsignacion(1L);
        asignacion.setPedido(pedido);
        asignacion.setProducto(producto);
        asignacion.setLote(lote);
        asignacion.setCantidadDescontada(BigDecimal.valueOf(4));
        asignacion.setCantidadDevuelta(BigDecimal.valueOf(2)); // Ya devolvió 2
        asignacion.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of(asignacion));
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DEVUELTA))
            .thenReturn(false);
        when(inventarioLoteRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of(lote));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of(producto));
        when(inventarioLoteRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L))
            .thenReturn(BigDecimal.valueOf(6));

        inventarioLoteService.devolverStockDePedido(1L);

        // Solo debe devolver 4 - 2 = 2 unidades
        assertEquals(BigDecimal.valueOf(4), lote.getCantidadActual());
        assertEquals(AsignacionLotePedido.EstadoAsignacion.DEVUELTA, asignacion.getEstado());
        assertEquals(BigDecimal.valueOf(4), asignacion.getCantidadDevuelta());
    }

    // PRUEBA 15: devolverStockDePedido pedido ya devuelto

    @Test
    void devolverStockDePedido_pedidoYaDevuelto_lanzaConflict() {
        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of());
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DEVUELTA))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.devolverStockDePedido(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El stock de este pedido ya fue devuelto", exception.getReason());
        verify(inventarioLoteRepository, never()).findAllByIdInForUpdate(any());
        verify(productoRepository, never()).findAllByIdInForUpdate(any());
    }

    // PRUEBA 16: devolverStockDePedido pedido legacy sin asignaciones

    @Test
    void devolverStockDePedido_pedidoLegacy_sinAsignaciones_lanzaConflict() {
        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of());
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DEVUELTA))
            .thenReturn(false);
        when(asignacionLotePedidoRepository.existsByPedido_Id(1L))
            .thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.devolverStockDePedido(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El pedido no tiene trazabilidad de lotes; requiere ajuste manual de inventario", exception.getReason());
        verify(inventarioLoteRepository, never()).findAllByIdInForUpdate(any());
    }

    // PRUEBA 17: devolverStockDePedido asignación inconsistente (lote nulo)

    @Test
    void devolverStockDePedido_asignacionInconsistente_loteNulo_lanzaConflict() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);

        AsignacionLotePedido asignacion = new AsignacionLotePedido();
        asignacion.setIdAsignacion(1L);
        asignacion.setPedido(pedido);
        asignacion.setProducto(new Producto());
        asignacion.setLote(null); // Lote nulo
        asignacion.setCantidadDescontada(BigDecimal.valueOf(4));
        asignacion.setCantidadDevuelta(BigDecimal.ZERO);
        asignacion.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of(asignacion));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.devolverStockDePedido(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("La trazabilidad de lotes del pedido es inconsistente", exception.getReason());
        verify(inventarioLoteRepository, never()).findAllByIdInForUpdate(any());
    }

    // PRUEBA 18: devolverStockDePedido lote faltante en BD

    @Test
    void devolverStockDePedido_loteFaltante_lanzaConflict() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");

        Pedido pedido = new Pedido();
        pedido.setId(1L);

        InventarioLote lote = new InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(5));

        AsignacionLotePedido asignacion = new AsignacionLotePedido();
        asignacion.setIdAsignacion(1L);
        asignacion.setPedido(pedido);
        asignacion.setProducto(producto);
        asignacion.setLote(lote);
        asignacion.setCantidadDescontada(BigDecimal.valueOf(4));
        asignacion.setCantidadDevuelta(BigDecimal.ZERO);
        asignacion.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        when(asignacionLotePedidoRepository.findByPedidoIdAndEstadoForUpdate(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(List.of(asignacion));
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DEVUELTA))
            .thenReturn(false);
        when(inventarioLoteRepository.findAllByIdInForUpdate(List.of(1L)))
            .thenReturn(List.of()); // Lote no encontrado

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.devolverStockDePedido(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("La trazabilidad de lotes del pedido es inconsistente", exception.getReason());
        verify(inventarioLoteRepository, never()).saveAll(any());
    }

    // =========================================================================
    // PRUEBAS FASE 2A: validarStockDevueltoParaReactivacion
    // =========================================================================

    // PRUEBA 19: validarStockDevueltoParaReactivacion rechaza ID nulo

    @Test
    void validarStockDevueltoParaReactivacion_rechazaIdNulo() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.validarStockDevueltoParaReactivacion(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El ID del pedido es obligatorio", exception.getReason());
        verify(asignacionLotePedidoRepository, never()).existsByPedido_IdAndEstado(any(), any());
    }

    // PRUEBA 20: validarStockDevueltoParaReactivacion con stock no devuelto

    @Test
    void validarStockDevueltoParaReactivacion_conStockNoDevuelto_lanzaConflict() {
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.validarStockDevueltoParaReactivacion(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El stock del pedido aún no ha sido devuelto", exception.getReason());
        verify(asignacionLotePedidoRepository, never()).existsByPedido_Id(any());
    }

    // PRUEBA 21: validarStockDevueltoParaReactivacion sin trazabilidad

    @Test
    void validarStockDevueltoParaReactivacion_sinTrazabilidad_lanzaConflict() {
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(false);
        when(asignacionLotePedidoRepository.existsByPedido_Id(1L))
            .thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> inventarioLoteService.validarStockDevueltoParaReactivacion(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El pedido no tiene trazabilidad de lotes; requiere ajuste manual de inventario", exception.getReason());
    }

    // PRUEBA 22: validarStockDevueltoParaReactivacion todas DEVUELTA finaliza correctamente

    @Test
    void validarStockDevueltoParaReactivacion_todasDevueltas_finalizaCorrectamente() {
        when(asignacionLotePedidoRepository.existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA))
            .thenReturn(false);
        when(asignacionLotePedidoRepository.existsByPedido_Id(1L))
            .thenReturn(true);

        // No debe lanzar excepción
        inventarioLoteService.validarStockDevueltoParaReactivacion(1L);

        verify(asignacionLotePedidoRepository).existsByPedido_IdAndEstado(1L, AsignacionLotePedido.EstadoAsignacion.DESCONTADA);
        verify(asignacionLotePedidoRepository).existsByPedido_Id(1L);
    }
}
