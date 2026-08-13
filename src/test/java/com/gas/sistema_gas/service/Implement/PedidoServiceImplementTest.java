package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.InOrder;

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

import com.gas.sistema_gas.Mapper.ClienteMapper;
import com.gas.sistema_gas.Mapper.PedidoMapper;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Repository.EvidenciaRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.service.CorrelativoService;
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

    @Mock
    private CorrelativoService correlativoService;

    @Mock
    private ControlEnvaseRepository controlEnvaseRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @Mock
    private ClienteMapper clienteMapper;

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
        producto.setNombre("Producto " + id);
        producto.setStockReservado(stockReservado);
        producto.setPrecioVenta(new BigDecimal("100.00"));
        producto.setStockLlenos(BigDecimal.ZERO);
        return producto;
    }

    private DetallePedido detalle(Producto producto, int cantidad) {
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        return detalle;
    }

    private InventarioLote lote(
            Long id,
            Producto producto,
            BigDecimal cantidadActual) {

        InventarioLote lote = new InventarioLote();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setCantidadActual(cantidadActual);
        return lote;
    }

    private PedidoDTO.SimpleResponse simpleResponse(Long id, String estado) {
        return new PedidoDTO.SimpleResponse(
            id, "NV001", LocalDateTime.now(), null, null, null, null, null,
            estado, "PENDIENTE", BigDecimal.ZERO, BigDecimal.ZERO, null, "DOMICILIO", null, List.of()
        );
    }

    private PedidoDTO.Create createDtoDomicilio(List<PedidoDTO.DetalleCreate> detalles) {
        return new PedidoDTO.Create(
            null, 1L, null, "Juan", "Av Test 123", "ref", "999999999",
            10L, 1L, null, null, "obs", "PENDIENTE", "DOMICILIO", null,
            List.of(), detalles, "NINGUNO", List.of()
        );
    }

    private PedidoDTO.Create createDtoLocal(List<PedidoDTO.DetalleCreate> detalles) {
        return new PedidoDTO.Create(
            null, 1L, null, "Juan", "Av Test 123", "ref", "999999999",
            null, 1L, null, null, "obs", null, "LOCAL", null,
            List.of(), detalles, "NINGUNO", List.of()
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
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
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
        verify(inventarioLoteRepository, never()).save(any());
    }

    @Test
    void deleteOrder_conPagoPositivo_lanzaConflict_sinRevertirInventario() {
        Pedido pedido = pedido(101L, "PENDIENTE");

        when(pedidoRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(pedido));
        when(pedidoPagoRepository.existsByPedido_IdAndMontoGreaterThan(101L, BigDecimal.ZERO)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.deleteOrder(101L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("El pedido tiene pagos registrados y requiere un reembolso antes de poder cancelarse", ex.getReason());
        assertEquals("PENDIENTE", pedido.getEstadoPedido());
        verify(detalleRepository, never()).findByPedido_Id(any());
        verify(productoRepository, never()).findAllByIdInForUpdate(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
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
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(simpleResponse(14L, "ANULADO"));

        pedidoService.updateEstadoPedido(14L, "ANULADO");

        // stockReservado final = 10 - 4 = 6
        assertEquals(0, new BigDecimal("6.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe ser 6");
        assertEquals("ANULADO", pedido.getEstadoPedido());

        // NUNCA consultar ni guardar InventarioLote
        verify(inventarioLoteRepository, never()).save(any());
    }

    @Test
    void updateEstadoPedido_aAnuladoConPagoPositivo_lanzaConflict_sinCambios() {
        Pedido pedido = pedido(102L, "PENDIENTE");

        when(pedidoRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(pedido));
        when(pedidoPagoRepository.existsByPedido_IdAndMontoGreaterThan(102L, BigDecimal.ZERO)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(102L, "ANULADO"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("PENDIENTE", pedido.getEstadoPedido());
        verify(detalleRepository, never()).findByPedido_Id(any());
        verify(productoRepository, never()).findAllByIdInForUpdate(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void updateEstadoPedido_aRechazadoOCanceladoConPagoPositivo_lanzaConflict() {
        Pedido rechazado = pedido(103L, "EN_DOMICILIO");
        Pedido cancelado = pedido(104L, "EN_DOMICILIO");

        when(pedidoRepository.findByIdForUpdate(103L)).thenReturn(Optional.of(rechazado));
        when(pedidoRepository.findByIdForUpdate(104L)).thenReturn(Optional.of(cancelado));
        when(pedidoPagoRepository.existsByPedido_IdAndMontoGreaterThan(103L, BigDecimal.ZERO)).thenReturn(true);
        when(pedidoPagoRepository.existsByPedido_IdAndMontoGreaterThan(104L, BigDecimal.ZERO)).thenReturn(true);

        ResponseStatusException rechazo = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(103L, "RECHAZADO"));
        ResponseStatusException cancelacion = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(104L, "CANCELADO"));

        assertEquals(HttpStatus.CONFLICT, rechazo.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, cancelacion.getStatusCode());
        assertEquals("EN_DOMICILIO", rechazado.getEstadoPedido());
        assertEquals("EN_DOMICILIO", cancelado.getEstadoPedido());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // ---------- PRUEBA 6: Reserva insuficiente ----------

    @Test
    void deleteOrder_reservaInsuficiente_lanzaConflict_pedidoNoAnulado_noTocaLotes() {
        Pedido pedido = pedido(15L, "PENDIENTE");
        Producto producto = producto(1L, new BigDecimal("3.00")); // reserva 3
        DetallePedido detalle = detalle(producto, 5); // cantidad 5 > reserva 3

        when(pedidoRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(15L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.deleteOrder(15L));

        assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());
        assertEquals("La reserva del producto es menor que la cantidad del pedido", ex.getReason());

        // El pedido sigue siendo PENDIENTE (no se asignó ANULADO)
        assertEquals("PENDIENTE", pedido.getEstadoPedido());

        // NUNCA consultar ni guardar InventarioLote
        verify(inventarioLoteRepository, never()).save(any());
        // NUNCA llamar descontarStockPorPEPS
        verify(inventarioLoteService, never()).descontarStockPorPEPS(any(DetallePedido.class));
        // No guardar Pedido (la excepción ocurre antes)
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // ---------- PRUEBA 7: Verificar uso de findByIdForUpdate ----------

    @Test
    void updateEstadoPedido_yDeleteOrder_usanFindByIdForUpdate_noFindById() {
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

    // ---------- PRUEBA 8: CLIENTE_AUSENTE → ACEPTADO con devolución confirmada ----------

    @Test
    void updateEstadoPedido_CLIENTE_AUSENTE_a_ACEPTADO_conStock_disponible_reservaStock() {
        Pedido pedido = pedido(20L, "CLIENTE_AUSENTE");
        Producto producto = producto(1L, new BigDecimal("3.00")); // stockReservado inicial 3
        DetallePedido detalle = detalle(producto, 5); // cantidad 5

        when(pedidoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(20L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("20.00"));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(20L, "ACEPTADO"));

        pedidoService.updateEstadoPedido(20L, "ACEPTADO");

        // Verificar estado final
        assertEquals("ACEPTADO", pedido.getEstadoPedido());

        // Verificar stockReservado final = 3 + 5 = 8
        assertEquals(0, new BigDecimal("8.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe ser 8");

        // FASE 2B: validarStockDevueltoParaReactivacion se llama exactamente una vez
        verify(inventarioLoteService, times(1)).validarStockDevueltoParaReactivacion(20L);

        // La validación ocurre antes de guardar Producto con la nueva reserva
        InOrder inOrder = inOrder(inventarioLoteService, productoRepository);
        inOrder.verify(inventarioLoteService).validarStockDevueltoParaReactivacion(20L);
        inOrder.verify(productoRepository).save(producto);

        // Verificar que se guardó Producto y Pedido
        verify(productoRepository).save(any(Producto.class));
        verify(pedidoRepository).save(any(Pedido.class));

        // NUNCA modificar InventarioLote
        verify(inventarioLoteRepository, never()).save(any());
        // NUNCA llamar descontarStockPorPEPS
        verify(inventarioLoteService, never()).descontarStockPorPEPS(any(DetallePedido.class));
    }

    // ---------- PRUEBA 8B: CLIENTE_AUSENTE → ACEPTADO cuando validación lanza CONFLICT ----------

    @Test
    void updateEstadoPedido_CLIENTE_AUSENTE_a_ACEPTADO_cuandoValidacionFalla_lanzaConflict_noGuardaNada() {
        Pedido pedido = pedido(25L, "CLIENTE_AUSENTE");
        Producto producto = producto(1L, new BigDecimal("3.00"));
        DetallePedido detalle = detalle(producto, 5);

        when(pedidoRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(pedido));

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El stock del pedido aún no ha sido devuelto"))
            .when(inventarioLoteService).validarStockDevueltoParaReactivacion(25L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(25L, "ACEPTADO"));

        assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());
        assertEquals("El stock del pedido aún no ha sido devuelto", ex.getReason());

        // El estado continúa CLIENTE_AUSENTE
        assertEquals("CLIENTE_AUSENTE", pedido.getEstadoPedido());

        // productoRepository.save nunca se ejecuta
        verify(productoRepository, never()).save(any(Producto.class));
        // pedidoRepository.save nunca se ejecuta
        verify(pedidoRepository, never()).save(any(Pedido.class));
        // descontarStockPorPEPS nunca se ejecuta
        verify(inventarioLoteService, never()).descontarStockPorPEPS(any(DetallePedido.class));
    }

    // ---------- PRUEBA 9: CLIENTE_AUSENTE → ACEPTADO sin stock suficiente ----------

    @Test
    void updateEstadoPedido_CLIENTE_AUSENTE_a_ACEPTADO_sinStock_suficiente_lanzaConflict() {
        Pedido pedido = pedido(21L, "CLIENTE_AUSENTE");
        Producto producto = producto(1L, new BigDecimal("3.00")); // stockReservado 3
        DetallePedido detalle = detalle(producto, 5); // cantidad 5

        when(pedidoRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(21L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("6.00"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(21L, "ACEPTADO"));

        assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());
        assertEquals("Stock insuficiente para reactivar el pedido: " + producto.getNombre(), ex.getReason());

        // Estado permanece CLIENTE_AUSENTE
        assertEquals("CLIENTE_AUSENTE", pedido.getEstadoPedido());

        // NUNCA guardar Pedido
        verify(pedidoRepository, never()).save(any(Pedido.class));
        // NUNCA modificar InventarioLote
        verify(inventarioLoteRepository, never()).save(any());
        // NUNCA llamar descontarStockPorPEPS
        verify(inventarioLoteService, never()).descontarStockPorPEPS(any(DetallePedido.class));
    }

    // ---------- PRUEBA 10: Producto con stockReservado null ----------

    @Test
    void updateEstadoPedido_CLIENTE_AUSENTE_a_ACEPTado_productoStockReservadoNull_reservaCorrectamente() {
        Pedido pedido = pedido(22L, "CLIENTE_AUSENTE");
        Producto producto = producto(1L, null); // stockReservado null
        DetallePedido detalle = detalle(producto, 5);

        when(pedidoRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(22L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("20.00"));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(22L, "ACEPTADO"));

        pedidoService.updateEstadoPedido(22L, "ACEPTADO");

        // Verificar estado final
        assertEquals("ACEPTADO", pedido.getEstadoPedido());

        // Verificar stockReservado final = 0 + 5 = 5 (tratando null como cero)
        assertEquals(0, new BigDecimal("5.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe ser 5");

        verify(productoRepository).save(any(Producto.class));
        verify(pedidoRepository).save(any(Pedido.class));
    }

    // ---------- PRUEBA 11: PENDIENTE → ACEPTADO no ejecuta lógica de reactivación ----------

    @Test
    void updateEstadoPedido_PENDIENTE_a_ACEPTADO_noEjecutaReservarStock() {
        Pedido pedido = pedido(23L, "PENDIENTE");
        Producto producto = producto(1L, new BigDecimal("10.00"));
        DetallePedido detalle = detalle(producto, 5);

        when(pedidoRepository.findByIdForUpdate(23L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(23L, "ACEPTADO"));

        pedidoService.updateEstadoPedido(23L, "ACEPTADO");

        // Verificar estado final
        assertEquals("ACEPTADO", pedido.getEstadoPedido());

        // Verificar que NO se modificó stockReservado (no debe ejecutar reservarStockParaReactivacion)
        assertEquals(0, new BigDecimal("10.00").compareTo(producto.getStockReservado()),
                "stockReservado no debe modificarse en transición PENDIENTE → ACEPTADO");

        // FASE 2B: NO debe llamar validarStockDevueltoParaReactivacion en PENDIENTE → ACEPTADO
        verify(inventarioLoteService, never()).validarStockDevueltoParaReactivacion(any());

        // NUNCA debe llamar a inventarioLoteRepository en esta transición
        verify(inventarioLoteRepository, never()).sumCantidadActualByProductoId(any());
        // NUNCA debe guardar Producto en esta transición
        verify(productoRepository, never()).save(any(Producto.class));
        // Solo se guarda el Pedido
        verify(pedidoRepository).save(any(Pedido.class));
    }

    // ---------- PRUEBA 12: ACEPTADO → CARGADO mantiene comportamiento actual ----------

    @Test
    void updateEstadoPedido_ACEPTADO_a_CARGADO_descuentaPorPEPS_yLiberaReserva() {
        Pedido pedido = pedido(24L, "ACEPTADO");
        Producto producto = producto(1L, new BigDecimal("10.00")); // stockReservado 10
        DetallePedido detalle = detalle(producto, 5);

        when(pedidoRepository.findByIdForUpdate(24L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(24L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("20.00"));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(24L, "CARGADO"));

        pedidoService.updateEstadoPedido(24L, "CARGADO");

        // Verificar estado final
        assertEquals("CARGADO", pedido.getEstadoPedido());

        // FASE 2B: NO debe llamar validarStockDevueltoParaReactivacion en ACEPTADO → CARGADO
        verify(inventarioLoteService, never()).validarStockDevueltoParaReactivacion(any());

        // Verificar que se llamó descontarStockPorPEPS con el detalle correcto
        verify(inventarioLoteService).descontarStockPorPEPS(any(DetallePedido.class));

        // Verificar que se liberó la reserva: 10 - 5 = 5
        assertEquals(0, new BigDecimal("5.00").compareTo(producto.getStockReservado()),
                "stockReservado debe liberarse a 5");

        // Verificar que se guardó Producto
        verify(productoRepository).save(any(Producto.class));
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void updateEstadoPedido_ACEPTADO_a_CARGADO_ignoraDetalleDeEnvaseVendido() {
        Pedido pedido = pedido(241L, "ACEPTADO");
        Producto contenido = producto(1L, BigDecimal.ONE);
        Producto envase = producto(2L, BigDecimal.ZERO);
        DetallePedido detalleContenido = detalle(contenido, 1);
        DetallePedido detalleEnvase = detalle(envase, 2);
        detalleEnvase.setEsEnvaseVendido(true);

        when(pedidoRepository.findByIdForUpdate(241L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(241L)).thenReturn(List.of(detalleContenido, detalleEnvase));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(contenido));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("9.00"));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(241L, "CARGADO"));

        pedidoService.updateEstadoPedido(241L, "CARGADO");

        assertAll(
                () -> assertEquals("CARGADO", pedido.getEstadoPedido()),
                () -> assertEquals(BigDecimal.ZERO, contenido.getStockReservado()),
                () -> assertEquals(BigDecimal.ZERO, envase.getStockReservado())
        );
        verify(inventarioLoteService).descontarStockPorPEPS(detalleContenido);
        verify(inventarioLoteService, never()).descontarStockPorPEPS(
                argThat(detalle -> detalle == detalleEnvase));
        verify(inventarioLoteRepository).sumCantidadActualByProductoId(1L);
        verify(inventarioLoteRepository, never()).sumCantidadActualByProductoId(2L);
    }

    // ---------- PRUEBA 13: createOrder con idPedido rechaza edición ----------

    @Test
    void createOrder_conIdPedido_rechazaEdicionAntesDeModificarDatos() {
        PedidoDTO.Create createDto = new PedidoDTO.Create(
            99L, // idPedido no nulo
            1L, "Juan", "999999999", "Av. Test 123", "referencia",
            "12345678", 1L, 1L, 1L, "OP123", "obs",
            "PENDIENTE", "DOMICILIO", null,
            List.of(), List.of(), "NINGUNO", List.of()
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> pedidoService.createOrder(createDto, 1L));

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode().value());
        assertEquals("La edición de pedidos existentes está temporalmente deshabilitada", exception.getReason());

        // No debe consultar ni modificar nada
        verify(pedidoRepository, never()).findById(any());
        verify(pedidoRepository, never()).findByIdForUpdate(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(detalleRepository, never()).findByPedido_Id(any());
        verify(detalleRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(controlEnvaseRepository, never()).deleteByPedido_Id(any());
        verify(inventarioLoteRepository, never()).save(any());
        verify(productoRepository, never()).save(any());
        verify(inventarioLoteService, never()).descontarStockPorPEPS(any(DetallePedido.class));
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
        verify(correlativoService, never()).incrementarYObtenerCodigo(any(), any());
    }

    @Test
    void createOrder_canjeDomicilio_persisteCantidadPendienteSinIncrementarVacios() {
        PedidoDTO.Create createDto = new PedidoDTO.Create(
                null, 1L, "Juan", "999999999", "Av. Test 123", "referencia",
                "12345678", 1L, 1L, null, null, null,
                "PENDIENTE", "DOMICILIO", null,
                List.of(),
                List.of(new PedidoDTO.DetalleCreate(1L, 1, new BigDecimal("10.00"), 0)),
                "CANJE",
                List.of(new PedidoDTO.EnvaseMovimientoCreate(1L, 1, null, null, null, null))
        );
        Pedido pedido = pedido(1L, "PENDIENTE");
        Producto producto = producto(1L, BigDecimal.ZERO);
        producto.setRequiereEnvase(true);
        producto.setStockVacios(4);
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        DetallePedido[] detalleGuardado = new DetallePedido[1];

        when(pedidoMapper.toEntity(createDto)).thenReturn(pedido);
        when(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001"))
                .thenReturn("NV001-0001");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findById(1L)).thenReturn(Optional.of(empleado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.findByProductoIdsForUpdate(List.of(1L)))
                .thenReturn(List.of(lote(1L, producto, new BigDecimal("10.00"))));
        when(detalleRepository.save(any(DetallePedido.class))).thenAnswer(invocation -> {
            detalleGuardado[0] = invocation.getArgument(0);
            return detalleGuardado[0];
        });
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(any())).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(1L, "PENDIENTE"));

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(createDto, 1L);

        assertEquals("PENDIENTE", respuesta.estadoPedido());
        assertEquals(1, detalleGuardado[0].getCantidadCanje());
        assertEquals(4, producto.getStockVacios());
        assertEquals(0, producto.getStockReservado().compareTo(BigDecimal.ONE));
        verify(controlEnvaseRepository, never()).save(any());
    }

    // =========================================================================
    // FASE 4B-2A: PRUEBAS NUEVAS — BLOQUEOS ATÓMICOS DE PRODUCTOS
    // =========================================================================

    @Test
    void createOrder_DOMICILIO_bloqueaProductoAntesDeReservar() {
        Producto producto = producto(1L, BigDecimal.ZERO);
        PedidoDTO.Create createDto = createDtoDomicilio(
                List.of(new PedidoDTO.DetalleCreate(1L, 3, null, null))
        );

        Pedido pedido = new Pedido();
        pedido.setTipoVenta("DOMICILIO");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Empleado empleado = new Empleado();
        empleado.setId(10L);

        when(pedidoMapper.toEntity(createDto)).thenReturn(pedido);
        when(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001")).thenReturn("NV001");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findById(10L)).thenReturn(Optional.of(empleado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.findByProductoIdsForUpdate(List.of(1L))).thenReturn(List.of(
            lote(1L, producto, new BigDecimal("50.00"))
        ));
        when(detalleRepository.save(any(DetallePedido.class))).thenAnswer(invocation -> {
            DetallePedido d = invocation.getArgument(0);
            d.setIdDetalle(1L);
            return d;
        });
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(any())).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(1L, "PENDIENTE"));

        pedidoService.createOrder(createDto, 1L);

        // FASE 4B-2A: se bloquea con findAllByIdInForUpdate con IDs ordenados
        verify(productoRepository).findAllByIdInForUpdate(List.of(1L));
        // FASE 4B-2A: se bloquea inventario de lotes con findByProductoIdsForUpdate
        verify(inventarioLoteRepository, times(1)).findByProductoIdsForUpdate(List.of(1L));
        // se usa el Producto bloqueado: stockReservado aumenta correctamente
        assertEquals(0, new BigDecimal("3.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe ser 3");
        // createOrder ya no usa productoRepository.findById para los detalles
        verify(productoRepository, never()).findById(eq(1L));
    }

    @Test
    void createOrder_DOMICILIO_variosDetallesMismoProducto_bloqueaUnaVezYAcumula() {
        Producto producto = producto(1L, BigDecimal.ZERO);
        PedidoDTO.Create createDto = createDtoDomicilio(
                List.of(
                        new PedidoDTO.DetalleCreate(1L, 3, null, null),
                        new PedidoDTO.DetalleCreate(1L, 4, null, null)
                )
        );

        Pedido pedido = new Pedido();
        pedido.setTipoVenta("DOMICILIO");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Empleado empleado = new Empleado();
        empleado.setId(10L);

        when(pedidoMapper.toEntity(createDto)).thenReturn(pedido);
        when(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001")).thenReturn("NV001");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findById(10L)).thenReturn(Optional.of(empleado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.findByProductoIdsForUpdate(List.of(1L))).thenReturn(List.of(
            lote(1L, producto, new BigDecimal("50.00"))
        ));
        when(detalleRepository.save(any(DetallePedido.class))).thenAnswer(invocation -> {
            DetallePedido d = invocation.getArgument(0);
            d.setIdDetalle(1L);
            return d;
        });
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(any())).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(1L, "PENDIENTE"));

        pedidoService.createOrder(createDto, 1L);

        // FASE 4B-2A: se bloquea una sola fila de Producto (una sola llamada)
        verify(productoRepository, times(1)).findAllByIdInForUpdate(List.of(1L));
        // FASE 4B-2A: se bloquea inventario de lotes con findByProductoIdsForUpdate
        verify(inventarioLoteRepository, times(1)).findByProductoIdsForUpdate(List.of(1L));
        // stockReservado final = 3 + 4 = 7
        assertEquals(0, new BigDecimal("7.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe acumular 7");
    }

    @Test
    void createOrder_LOCAL_bloqueaProductoAntesDeDescontarPeps() {
        Producto producto = producto(1L, BigDecimal.ZERO);
        PedidoDTO.Create createDto = createDtoLocal(
                List.of(new PedidoDTO.DetalleCreate(1L, 2, null, null))
        );

        Pedido pedido = new Pedido();
        pedido.setTipoVenta("LOCAL");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(pedidoMapper.toEntity(createDto)).thenReturn(pedido);
        when(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001")).thenReturn("NV001");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.findByProductoIdsForUpdate(List.of(1L))).thenReturn(List.of(
            lote(1L, producto, new BigDecimal("50.00"))
        ));
        when(detalleRepository.save(any(DetallePedido.class))).thenAnswer(invocation -> {
            DetallePedido d = invocation.getArgument(0);
            d.setIdDetalle(1L);
            return d;
        });
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(any())).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(1L, "ENTREGADO"));

        pedidoService.createOrder(createDto, 1L);

        // FASE 4B-2A: InOrder verifica que el bloqueo ocurre ANTES del descuento PEPS
        InOrder inOrder = inOrder(productoRepository, inventarioLoteRepository, inventarioLoteService);
        inOrder.verify(productoRepository).findAllByIdInForUpdate(List.of(1L));
        inOrder.verify(inventarioLoteRepository).findByProductoIdsForUpdate(List.of(1L));
        inOrder.verify(inventarioLoteService).descontarStockPorPEPS(any(DetallePedido.class));
        // FASE 4B-2A: se bloquea inventario de lotes con findByProductoIdsForUpdate
        verify(inventarioLoteRepository, times(1)).findByProductoIdsForUpdate(List.of(1L));
    }

    @Test
    void updateEstadoPedido_CARGADO_bloqueaProductosAntesDePeps() {
        Pedido pedido = pedido(30L, "ACEPTADO");
        Producto producto = producto(1L, new BigDecimal("10.00"));
        DetallePedido detalle = detalle(producto, 5);

        when(pedidoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(30L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("20.00"));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(30L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(30L, "CARGADO"));

        pedidoService.updateEstadoPedido(30L, "CARGADO");

        // FASE 4B-2A: Orden Pedido → Productos → Lotes PEPS
        InOrder inOrder = inOrder(pedidoRepository, productoRepository, inventarioLoteService);
        inOrder.verify(pedidoRepository).findByIdForUpdate(30L);
        inOrder.verify(productoRepository).findAllByIdInForUpdate(List.of(1L));
        inOrder.verify(inventarioLoteService).descontarStockPorPEPS(any(DetallePedido.class));
    }

    @Test
    void updateEstadoPedido_CARGADO_reservaInsuficiente_lanzaConflictYNoGuarda() {
        Pedido pedido = pedido(31L, "ACEPTADO");
        Producto producto = producto(1L, new BigDecimal("2.00")); // reserva 2 < cantidad 5
        DetallePedido detalle = detalle(producto, 5);

        when(pedidoRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(31L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("20.00"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(31L, "CARGADO"));

        // Lanza HTTP 409 CONFLICT
        assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());

        // No guarda Producto
        verify(productoRepository, never()).save(any(Producto.class));
        // No guarda el nuevo estado del Pedido
        verify(pedidoRepository, never()).save(any(Pedido.class));

        // Nota: descontarStockPorPEPS puede haber sido invocado antes del CONFLICT;
        // el rollback real se probará en integración en FASE 4B-4.
    }

    @Test
    void deleteOrder_variosDetallesMismoProducto_liberaTotalAgrupado() {
        Pedido pedido = pedido(32L, "PENDIENTE");
        Producto producto = producto(1L, new BigDecimal("10.00"));
        DetallePedido detalle1 = detalle(producto, 3);
        DetallePedido detalle2 = detalle(producto, 4);

        when(pedidoRepository.findByIdForUpdate(32L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(32L)).thenReturn(List.of(detalle1, detalle2));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoService.deleteOrder(32L);

        // FASE 4B-2A: se agrupan cantidades (3 + 4 = 7) y se libera la suma exacta
        verify(productoRepository, times(1)).findAllByIdInForUpdate(List.of(1L));
        // stockReservado final = 10 - 7 = 3
        assertEquals(0, new BigDecimal("3.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe liberar la suma agrupada (7)");
        verify(productoRepository, times(1)).save(producto);
        assertEquals("ANULADO", pedido.getEstadoPedido());
    }

    @Test
    void reactivacion_bloqueaProductoYReserva() {
        Pedido pedido = pedido(33L, "CLIENTE_AUSENTE");
        Producto producto = producto(1L, BigDecimal.ZERO);
        DetallePedido detalle = detalle(producto, 5);

        when(pedidoRepository.findByIdForUpdate(33L)).thenReturn(Optional.of(pedido));
        when(detalleRepository.findByPedido_Id(33L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(producto));
        when(inventarioLoteRepository.sumCantidadActualByProductoId(1L)).thenReturn(new BigDecimal("20.00"));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(33L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(33L, "ACEPTADO"));

        pedidoService.updateEstadoPedido(33L, "ACEPTADO");

        // FASE 4B-2A: CLIENTE_AUSENTE → ACEPTADO usa el Producto bloqueado
        verify(productoRepository).findAllByIdInForUpdate(List.of(1L));
        // stockReservado final = 0 + 5 = 5
        assertEquals(0, new BigDecimal("5.00").compareTo(producto.getStockReservado()),
                "stockReservado final debe ser 5");
        // No modifica lotes, no descuenta PEPS
        verify(inventarioLoteRepository, never()).save(any());
        verify(inventarioLoteService, never()).descontarStockPorPEPS(any(DetallePedido.class));
    }

    @Test
    void createOrder_DOMICILIO_productosDistintos_bloqueaIdsOrdenados() {
        Producto producto1 = producto(1L, BigDecimal.ZERO);
        Producto producto2 = producto(2L, BigDecimal.ZERO);

        // Detalles en orden inverso: producto 2 primero, luego producto 1
        PedidoDTO.Create createDto = createDtoDomicilio(
                List.of(
                        new PedidoDTO.DetalleCreate(2L, 3, null, null),
                        new PedidoDTO.DetalleCreate(1L, 4, null, null)
                )
        );

        Pedido pedido = new Pedido();
        pedido.setTipoVenta("DOMICILIO");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Empleado empleado = new Empleado();
        empleado.setId(10L);

        when(pedidoMapper.toEntity(createDto)).thenReturn(pedido);
        when(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001")).thenReturn("NV001");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findById(10L)).thenReturn(Optional.of(empleado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Los IDs deben llegar ordenados ASC: List.of(1L, 2L)
        when(productoRepository.findAllByIdInForUpdate(List.of(1L, 2L))).thenReturn(List.of(producto1, producto2));
        when(inventarioLoteRepository.findByProductoIdsForUpdate(List.of(1L, 2L))).thenReturn(List.of(
            lote(1L, producto1, new BigDecimal("50.00")),
            lote(2L, producto2, new BigDecimal("50.00"))
        ));
        when(detalleRepository.save(any(DetallePedido.class))).thenAnswer(invocation -> {
            DetallePedido d = invocation.getArgument(0);
            d.setIdDetalle(1L);
            return d;
        });
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(any())).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(any(Pedido.class))).thenReturn(simpleResponse(1L, "PENDIENTE"));

        pedidoService.createOrder(createDto, 1L);

        // FASE 4B-2A: se invoca exactamente una vez con IDs ordenados ASC
        verify(productoRepository, times(1)).findAllByIdInForUpdate(List.of(1L, 2L));
        // FASE 4B-2A: se bloquea inventario de lotes con findByProductoIdsForUpdate
        verify(inventarioLoteRepository, times(1)).findByProductoIdsForUpdate(List.of(1L, 2L));
        // Ambos productos reservaron correctamente
        assertEquals(0, new BigDecimal("4.00").compareTo(producto1.getStockReservado()),
                "stockReservado del producto 1 debe ser 4");
        assertEquals(0, new BigDecimal("3.00").compareTo(producto2.getStockReservado()),
                "stockReservado del producto 2 debe ser 3");
    }
}
