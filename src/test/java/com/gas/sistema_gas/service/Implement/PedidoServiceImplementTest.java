package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.PedidoMapper;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.EvidenciaRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.PedidoDTO;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplementTest {

    // Nota: estas pruebas son unitarias y usan Mockito para simular el repositorio y el mapper.
    // Los objetos creados en este archivo NO se persisten en la base de datos.

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PedidoPagoRepository pedidoPagoRepository;

    @Mock
    private EvidenciaRepository evidenciaRepository;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private DetallePedidoRepository detalleRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioLoteRepository inventarioLoteRepository;

    @Mock
    private com.gas.sistema_gas.service.InventarioLoteService inventarioLoteService;

    @InjectMocks
    private PedidoServiceImplement pedidoService;

    // ---------- Helpers ----------

    private Pedido pedido(Long id, String estado) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setEstadoPedido(estado);
        return pedido;
    }

    private Producto producto(Long id, BigDecimal stockReservado) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setStockReservado(stockReservado);
        return producto;
    }

    private DetallePedido detalle(Producto producto, int cantidad) {
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        return detalle;
    }

    private PedidoDTO.SimpleResponse simpleResponse(Long id, String estado) {
        return new PedidoDTO.SimpleResponse(
            id, "NV001", LocalDateTime.now(), null, null, null, null, null,
            estado, "PENDIENTE", BigDecimal.ZERO, BigDecimal.ZERO, null, "DOMICILIO", null, List.of()
        );
    }

    // ---------- Pruebas existentes (migradas a findByIdForUpdate) ----------

    @Test
    void shouldExposeClientAddressAndDateInSimpleResponse() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCodigo("NV001");
        pedido.setFechaSolicitud(LocalDateTime.of(2024, 1, 2, 3, 4));

        Cliente cliente = new Cliente();
        cliente.setNombre("Juanito");
        cliente.setDireccion("Av. Siempre Viva 123");
        pedido.setCliente(cliente);

        PedidoDTO.SimpleResponse mappedResponse = new PedidoDTO.SimpleResponse(
            1L, "NV001", pedido.getFechaSolicitud(), "Juanito", "Av. Siempre Viva 123",
            null, "Entregar en la puerta principal", null, "PENDIENTE", "PENDIENTE",
            BigDecimal.TEN, BigDecimal.TEN, null, "DOMICILIO", null, List.of()
        );

        when(pedidoRepository.findByTipoVentaAndEmpleadoIdWithMetodoPago("DOMICILIO", 10L)).thenReturn(List.of(pedido));
        when(pedidoPagoRepository.findByPedido_Id(1L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(mappedResponse);

        List<PedidoDTO.SimpleResponse> result = pedidoService.listByTipoVentaAndEmpleadoId("DOMICILIO", 10L);

        assertEquals("Juanito", result.get(0).nombreCliente());
        assertEquals("Av. Siempre Viva 123", result.get(0).direccionCliente());
        assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4), result.get(0).fechaSolicitud());
    }

    @Test
    void shouldUpdatePedidoStateAndPersistIt() {
        Pedido pedido = pedido(1L, "PENDIENTE");

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(1L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(simpleResponse(1L, "ACEPTADO"));

        PedidoDTO.SimpleResponse result = pedidoService.updateEstadoPedido(1L, "ACEPTADO");

        assertEquals("ACEPTADO", result.estadoPedido());
        assertEquals("ACEPTADO", pedido.getEstadoPedido());
    }

    @Test
    void shouldRejectBackwardsStateTransitions() {
        Pedido pedido = pedido(2L, "EN_CAMINO");

        when(pedidoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(pedido));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(2L, "ACEPTADO"));

        assertEquals("El estado del pedido no puede retroceder", exception.getReason());
    }

    @Test
    void deberiaMantenerPendienteAlEntregarPedidoSinPagos() {
        Pedido pedido = pedido(3L, "EN_DOMICILIO");
        pedido.setEstadoPago("PENDIENTE");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        when(pedidoRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(3L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(simpleResponse(3L, "ENTREGADO"));

        pedidoService.updateEstadoPedido(3L, "ENTREGADO");

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("PENDIENTE", pedido.getEstadoPago());
    }

    @Test
    void deberiaMantenerCreditoAlEntregarPedidoConPagoParcial() {
        Pedido pedido = pedido(4L, "EN_DOMICILIO");
        pedido.setEstadoPago("CREDITO");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        PedidoPago pagoParcial = new PedidoPago();
        pagoParcial.setId(41L);
        pagoParcial.setMonto(new BigDecimal("50.00"));

        when(pedidoRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(4L)).thenReturn(List.of(pagoParcial));
        when(evidenciaRepository.findByPedidoPago_Id(41L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(simpleResponse(4L, "ENTREGADO"));

        pedidoService.updateEstadoPedido(4L, "ENTREGADO");

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("CREDITO", pedido.getEstadoPago());
    }

    @Test
    void deberiaMarcarPagadoSoloCuandoLosPagosCubrenElTotal() {
        Pedido pedido = pedido(5L, "EN_DOMICILIO");
        pedido.setEstadoPago("CREDITO");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        PedidoPago pagoCompleto = new PedidoPago();
        pagoCompleto.setId(51L);
        pagoCompleto.setMonto(new BigDecimal("100.00"));

        when(pedidoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(5L)).thenReturn(List.of(pagoCompleto));
        when(evidenciaRepository.findByPedidoPago_Id(51L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(simpleResponse(5L, "ENTREGADO"));

        pedidoService.updateEstadoPedido(5L, "ENTREGADO");

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("PAGADO", pedido.getEstadoPago());
    }

    @Test
    void mapperDeberiaIgnorarEstadoPagoEnviadoPorElFrontend() {
        Pedido pedido = new Pedido();
        pedido.setEstadoPago("PENDIENTE");
        pedido.setEstadoPedido("PENDIENTE");

        PedidoMapper realMapper = org.mapstruct.factory.Mappers.getMapper(PedidoMapper.class);
        PedidoDTO.Update updateDto = new PedidoDTO.Update(null, "ENTREGADO", "PAGADO", "OP123", "mi observacion");

        realMapper.updateEntityFromDto(updateDto, pedido);

        assertEquals("PENDIENTE", pedido.getEstadoPago());
        assertEquals("mi observacion", pedido.getObservaciones());
    }

    // ---------- PRUEBA 1: deleteOrder sobre PENDIENTE ----------

    @Test
    void deleteOrder_sobrePENDIENTe_liberaReserva_noTocaLotes() {
        Pedido pedido = pedido(10L, "PENDIENTE");
        Producto producto = producto(1L, new BigDecimal("10.00"));
        DetallePedido detalle = detalle(producto, 4);

        when(pedidoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(10L)).thenReturn(List.of(detalle));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoService.deleteOrder(10L);

        // stockReservado final = 10 - 4 = 6
        assertEquals(0, new BigDecimal("6.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe ser 6");
        assertEquals("ANULADO", pedido.getEstadoPedido());

        // Verificar que se guardó Producto y Pedido
        verify(productoRepository).save(any(Producto.class));
        verify(pedidoRepository).save(any(Pedido.class));

        // NUNCA consultar ni guardar InventarioLote
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
        verify(inventarioLoteRepository, never()).save(any());
    }

    // ---------- PRUEBA 2: deleteOrder sobre CARGADO ----------

    @Test
    void deleteOrder_sobreCARGADO_lanzaBadRequest_noModificaInventario() {
        Pedido pedido = pedido(11L, "CARGADO");

        when(pedidoRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.deleteOrder(11L));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("Solo se puede anular un pedido en estado PENDIENTE", ex.getReason());

        // No modificar Producto
        verify(productoRepository, never()).save(any());
        // No consultar lotes
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
        // No guardar Pedido
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // ---------- PRUEBA 3: deleteOrder sobre ENTREGADO ----------

    @Test
    void deleteOrder_sobreENTREGADO_lanzaBadRequest_noModificaInventario() {
        Pedido pedido = pedido(12L, "ENTREGADO");

        when(pedidoRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.deleteOrder(12L));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("Solo se puede anular un pedido en estado PENDIENTE", ex.getReason());

        verify(productoRepository, never()).save(any());
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // ---------- PRUEBA 4: deleteOrder sobre ANULADO ----------

    @Test
    void deleteOrder_sobreANULADO_lanzaBadRequest_noModificaInventario() {
        Pedido pedido = pedido(13L, "ANULADO");

        when(pedidoRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.deleteOrder(13L));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("El pedido ya está anulado", ex.getReason());

        verify(productoRepository, never()).save(any());
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // ---------- PRUEBA 5: updateEstadoPedido de PENDIENTE a ANULADO ----------

    @Test
    void updateEstadoPedido_dePENDIENTeAANULADO_liberaSoloReserva_noTocaLotes() {
        Pedido pedido = pedido(14L, "PENDIENTE");
        Producto producto = producto(1L, new BigDecimal("10.00"));
        DetallePedido detalle = detalle(producto, 4);

        when(pedidoRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(14L)).thenReturn(List.of(detalle));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(simpleResponse(14L, "ANULADO"));

        pedidoService.updateEstadoPedido(14L, "ANULADO");

        // stockReservado final = 10 - 4 = 6
        assertEquals(0, new BigDecimal("6.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe ser 6");
        assertEquals("ANULADO", pedido.getEstadoPedido());

        // NUNCA consultar ni guardar InventarioLote
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
        verify(inventarioLoteRepository, never()).save(any());
    }

    // ---------- PRUEBA 6: Reserva insuficiente ----------

    @Test
    void deleteOrder_reservaInsuficiente_lanzaConflict_pedidoNoAnulado_noTocaLotes() {
        Pedido pedido = pedido(15L, "PENDIENTE");
        Producto producto = producto(1L, new BigDecimal("3.00")); // reserva 3
        DetallePedido detalle = detalle(producto, 5); // cantidad 5 > reserva 3

        when(pedidoRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(15L)).thenReturn(List.of(detalle));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.deleteOrder(15L));

        assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());
        assertEquals("La reserva del producto es menor que la cantidad del pedido", ex.getReason());

        // El pedido sigue siendo PENDIENTE (no se asignó ANULADO)
        assertEquals("PENDIENTE", pedido.getEstadoPedido());

        // NUNCA consultar ni guardar InventarioLote
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
        verify(inventarioLoteRepository, never()).save(any());
        // NUNCA llamar descontarStockPorPEPS
        verify(inventarioLoteService, never()).descontarStockPorPEPS(any(), any());
        // No guardar Pedido (la excepción ocurre antes)
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // ---------- PRUEBA 7: Verificar uso de findByIdForUpdate ----------

    @Test
    void updateEstadoPedido_yDeleteOrder_usanFindByIdForUpdate_noFindById() {
        // Esta prueba verifica indirectamente que el método usa findByIdForUpdate
        // Si usara findById, el stub de findByIdForUpdate no se invocaría y Mockito
        // lanzaría UnnecessaryStubbingException o el test fallaría al no encontrar el pedido.

        Pedido pedido = pedido(16L, "PENDIENTE");

        when(pedidoRepository.findByIdForUpdate(16L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(16L)).thenReturn(List.of());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoService.deleteOrder(16L);

        // Verificar que findByIdForUpdate fue llamado (no findById)
        verify(pedidoRepository).findByIdForUpdate(16L);
        // findById nunca debe ser llamado para deleteOrder
        verify(pedidoRepository, never()).findById(any());
    }
}