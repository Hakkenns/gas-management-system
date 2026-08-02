package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.Incidencia;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.RespuestaIncidencia;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Repository.IncidenciaRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.RespuestaIncidenciaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.service.InventarioLoteService;

import jakarta.servlet.http.HttpSession;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidenciaControllerTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private RespuestaIncidenciaRepository respuestaIncidenciaRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private DetallePedidoRepository detallePedidoRepository;

    @Mock
    private InventarioLoteRepository inventarioLoteRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioLoteService inventarioLoteService;

    @InjectMocks
    private IncidenciaController incidenciaController;

    // Helpers

    private Pedido pedido(Long id, String estado) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setEstadoPedido(estado);
        return p;
    }

    private Incidencia incidencia(Long id, String tipoIncidencia, String estado, Pedido pedido) {
        Incidencia i = new Incidencia();
        i.setId(id);
        i.setTipoIncidencia(tipoIncidencia);
        i.setEstado(estado);
        i.setPedido(pedido);
        return i;
    }

    private HttpSession session() {
        HttpSession session = org.mockito.Mockito.mock(HttpSession.class);
        when(session.getAttribute("usuarioLogueado")).thenReturn("admin");
        when(session.getAttribute("usuarioId")).thenReturn(1L);
        return session;
    }

    // PRUEBA 1: reportarIncidencia usa findByIdForUpdate

    @Test
    void reportarIncidencia_usaFindByIdForUpdate() {
        Pedido pedido = pedido(1L, "EN_DOMICILIO");
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        empleado.setNombre("Motorizado");
        pedido.setEmpleado(empleado);
        com.gas.sistema_gas.Model.Cliente cliente = new com.gas.sistema_gas.Model.Cliente();
        cliente.setNombre("Cliente Test");
        cliente.setTelefono("999999999");
        pedido.setCliente(cliente);
        pedido.setCodigo("PED-001");

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new com.gas.sistema_gas.Model.Usuario() {{
            setEmpleado(empleado);
        }}));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> {
            Incidencia i = inv.getArgument(0);
            i.setId(10L);
            return i;
        });
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        var evidencia = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(evidencia.isEmpty()).thenReturn(false);
        when(evidencia.getOriginalFilename()).thenReturn("foto.jpg");
        try {
            when(evidencia.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        ResponseEntity<?> response = incidenciaController.reportarIncidencia(1L, "CLIENTE_AUSENTE", evidencia, session());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pedidoRepository).findByIdForUpdate(1L);
        verify(pedidoRepository, never()).findById(1L);
    }

    // PRUEBA 2: confirmarRechazo válido - llama devolverStockDePedido antes de guardar

    @Test
    void confirmarRechazo_valido_estadoEN_REVISION_a_RECHAZADO() {
        Pedido pedido = pedido(1L, "EN_REVISION");
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        empleado.setNombre("Motorizado");
        pedido.setEmpleado(empleado);
        pedido.setCodigo("PED-001");

        Incidencia incidencia = incidencia(10L, "RECHAZO_POST_LLEGADA", "PENDIENTE", pedido);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(incidenciaRepository.findByPedidoId(1L)).thenReturn(List.of(incidencia));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(respuestaIncidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = incidenciaController.confirmarRechazo(Map.of("idPedido", 1L), session());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RECHAZADO", pedido.getEstadoPedido());
        assertEquals("CANCELADO", pedido.getEstadoPago());
        assertEquals("CONFIRMADO", incidencia.getEstado());
        verify(pedidoRepository).findByIdForUpdate(1L);
        verify(inventarioLoteService, times(1)).devolverStockDePedido(1L);

        // La devolución ocurre antes de guardar el pedido
        InOrder inOrder = inOrder(inventarioLoteService, pedidoRepository);
        inOrder.verify(inventarioLoteService).devolverStockDePedido(1L);
        inOrder.verify(pedidoRepository).save(pedido);
    }

    // PRUEBA 3: confirmarRechazo sobre pedido RECHAZADO

    @Test
    void confirmarRechazo_sobreRECHAZADO_retorna409() {
        Pedido pedido = pedido(1L, "RECHAZADO");

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        ResponseEntity<?> response = incidenciaController.confirmarRechazo(Map.of("idPedido", 1L), session());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("El rechazo ya fue confirmado", body.get("message"));
        verify(pedidoRepository, never()).save(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
        verify(productoRepository, never()).save(any());
        verify(respuestaIncidenciaRepository, never()).save(any());
    }

    // PRUEBA 4: confirmarRechazo sobre EN_DOMICILIO

    @Test
    void confirmarRechazo_sobreEN_DOMICILIO_retorna409() {
        Pedido pedido = pedido(1L, "EN_DOMICILIO");

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        ResponseEntity<?> response = incidenciaController.confirmarRechazo(Map.of("idPedido", 1L), session());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("El pedido debe estar en EN_REVISION para confirmar el rechazo", body.get("message"));
        verify(pedidoRepository, never()).save(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }

    // PRUEBA 5: confirmarRechazo sin incidencia pendiente

    @Test
    void confirmarRechazo_sinIncidenciaPendiente_retorna409() {
        Pedido pedido = pedido(1L, "EN_REVISION");
        Incidencia incidencia = incidencia(10L, "RECHAZO_POST_LLEGADA", "CONFIRMADO", pedido);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(incidenciaRepository.findByPedidoId(1L)).thenReturn(List.of(incidencia));

        ResponseEntity<?> response = incidenciaController.confirmarRechazo(Map.of("idPedido", 1L), session());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("No existe un rechazo pendiente para confirmar", body.get("message"));
        verify(pedidoRepository, never()).save(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }

    // PRUEBA 6: confirmarRechazo no marca incidencias diferentes

    @Test
    void confirmarRechazo_noMarcaIncidenciasDiferentes() {
        Pedido pedido = pedido(1L, "EN_REVISION");
        Incidencia rechazo = incidencia(10L, "RECHAZO_POST_LLEGADA", "PENDIENTE", pedido);
        Incidencia otra = incidencia(11L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(incidenciaRepository.findByPedidoId(1L)).thenReturn(List.of(rechazo, otra));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(respuestaIncidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = incidenciaController.confirmarRechazo(Map.of("idPedido", 1L), session());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("CONFIRMADO", rechazo.getEstado());
        assertEquals("PENDIENTE", otra.getEstado());
        verify(inventarioLoteService, times(1)).devolverStockDePedido(1L);
    }

    // PRUEBA: confirmarRechazo cuando devolverStockDePedido falla

    @Test
    void confirmarRechazo_cuandoDevolverStockFalla_noGuardaNada() {
        Pedido pedido = pedido(1L, "EN_REVISION");
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        empleado.setNombre("Motorizado");
        pedido.setEmpleado(empleado);
        pedido.setCodigo("PED-001");

        Incidencia incidencia = incidencia(10L, "RECHAZO_POST_LLEGADA", "PENDIENTE", pedido);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(incidenciaRepository.findByPedidoId(1L)).thenReturn(List.of(incidencia));

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La trazabilidad de lotes del pedido es inconsistente"))
            .when(inventarioLoteService).devolverStockDePedido(1L);

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> incidenciaController.confirmarRechazo(Map.of("idPedido", 1L), session())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        // El pedido conserva EN_REVISION
        assertEquals("EN_REVISION", pedido.getEstadoPedido());
        // No guarda pedido
        verify(pedidoRepository, never()).save(any(Pedido.class));
        // No guarda incidencia
        verify(incidenciaRepository, never()).save(any(Incidencia.class));
        // No crea RespuestaIncidencia
        verify(respuestaIncidenciaRepository, never()).save(any());
        // No envía notificaciones posteriores
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/admin/incidencias"), any(Object.class));
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.startsWith("/topic/repartidor/respuestas/"), any(Object.class));
    }

    // PRUEBA 7: marcarComoRevisado válido

    @Test
    void marcarComoRevisado_valido_CLIENTE_AUSENTE() {
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        incidencia.setEmpleado(new Empleado() {{ setId(1L); setNombre("Motorizado"); }});

        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(respuestaIncidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = incidenciaController.marcarComoRevisado(10L, session());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ATENDIDO", incidencia.getEstado());
        verify(incidenciaRepository).findByIdForUpdate(10L);
        verify(pedidoRepository).findByIdForUpdate(1L);
        verify(inventarioLoteService, times(1)).devolverStockDePedido(1L);
        verify(respuestaIncidenciaRepository, times(1)).save(any(RespuestaIncidencia.class));

        // La devolución ocurre antes de marcar ATENDIDO
        InOrder inOrder = inOrder(inventarioLoteService, incidenciaRepository);
        inOrder.verify(inventarioLoteService).devolverStockDePedido(1L);
        inOrder.verify(incidenciaRepository).save(incidencia);
    }

    // PRUEBA: marcarComoRevisado cuando devolverStockDePedido falla

    @Test
    void marcarComoRevisado_cuandoDevolverStockFalla_noGuardaNada() {
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        incidencia.setEmpleado(new Empleado() {{ setId(1L); setNombre("Motorizado"); }});

        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La trazabilidad de lotes del pedido es inconsistente"))
            .when(inventarioLoteService).devolverStockDePedido(1L);

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> incidenciaController.marcarComoRevisado(10L, session())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        // La incidencia continua PENDIENTE
        assertEquals("PENDIENTE", incidencia.getEstado());
        // No guarda incidencia
        verify(incidenciaRepository, never()).save(any(Incidencia.class));
        // No crea respuesta
        verify(respuestaIncidenciaRepository, never()).save(any());
        // No notifica
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.startsWith("/topic/repartidor/respuestas/"), any(Object.class));
    }

    // PRUEBA 8: marcarComoRevisado sobre ATENDIDO

    @Test
    void marcarComoRevisado_sobreATENDIDO_retorna409() {
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "ATENDIDO", pedido);

        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));

        ResponseEntity<?> response = incidenciaController.marcarComoRevisado(10L, session());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("La incidencia ya fue atendida", body.get("message"));
        verify(pedidoRepository, never()).findByIdForUpdate(any());
        verify(respuestaIncidenciaRepository, never()).save(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }

    // PRUEBA 9: marcarComoRevisado sobre tipo RECHAZO_POST_LLEGADA

    @Test
    void marcarComoRevisado_sobreRECHAZO_POST_LLEGADA_retornaBadRequest() {
        Pedido pedido = pedido(1L, "EN_REVISION");
        Incidencia incidencia = incidencia(10L, "RECHAZO_POST_LLEGADA", "PENDIENTE", pedido);

        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));

        ResponseEntity<?> response = incidenciaController.marcarComoRevisado(10L, session());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Esta acción solo corresponde a Cliente Ausente", body.get("message"));
        verify(pedidoRepository, never()).findByIdForUpdate(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }

    // ========== PRUEBAS DE REASIGNAR PEDIDO ==========
    
    // PRUEBA 11: reasignarPedido válido - CLIENTE_AUSENTE/PENDIENTE a PENDIENTE con nuevo repartidor
    
    @Test
    void reasignarPedido_valido_CLIENTE_AUSENTE() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");
        producto.setStockReservado(BigDecimal.ZERO);
        producto.setStockLlenos(BigDecimal.valueOf(15));
        
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        pedido.setCodigo("PED-001");
        Empleado empleadoAnterior = new Empleado();
        empleadoAnterior.setId(1L);
        empleadoAnterior.setNombre("Motorizado Anterior");
        pedido.setEmpleado(empleadoAnterior);
        
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        incidencia.setEmpleado(empleadoAnterior);
        
        com.gas.sistema_gas.Model.InventarioLote lote = new com.gas.sistema_gas.Model.InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(15));
        
        Empleado nuevoRepartidor = new Empleado();
        nuevoRepartidor.setId(2L);
        nuevoRepartidor.setNombre("Nuevo Motorizado");
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(empleadoRepository.findById(2L)).thenReturn(Optional.of(nuevoRepartidor));
        when(detallePedidoRepository.findByPedido_Id(1L)).thenReturn(List.of(detalle));
        when(inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(lote));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(respuestaIncidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        ResponseEntity<?> response = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PENDIENTE", pedido.getEstadoPedido());
        assertEquals(nuevoRepartidor, pedido.getEmpleado());
        assertEquals("ATENDIDO", incidencia.getEstado());
        assertEquals(BigDecimal.valueOf(5), producto.getStockReservado());
        
        verify(incidenciaRepository).findByIdForUpdate(10L);
        verify(pedidoRepository).findByIdForUpdate(1L);
        verify(incidenciaRepository, never()).findById(any());
        verify(pedidoRepository, never()).findById(any());
        verify(respuestaIncidenciaRepository).save(any(RespuestaIncidencia.class));
        
        // devolverStockDePedido se llama exactamente una vez
        verify(inventarioLoteService, times(1)).devolverStockDePedido(1L);
        
        // La devolución ocurre antes de reservar stock
        InOrder inOrder = inOrder(inventarioLoteService, productoRepository);
        inOrder.verify(inventarioLoteService).devolverStockDePedido(1L);
        inOrder.verify(productoRepository).save(producto);
    }
    
    // PRUEBA: reasignarPedido cuando devolverStockDePedido falla

    @Test
    void reasignarPedido_cuandoDevolverStockFalla_noGuardaNada() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");
        producto.setStockReservado(BigDecimal.ZERO);
        producto.setStockLlenos(BigDecimal.valueOf(15));
        
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        pedido.setCodigo("PED-001");
        Empleado empleadoAnterior = new Empleado();
        empleadoAnterior.setId(1L);
        empleadoAnterior.setNombre("Motorizado Anterior");
        pedido.setEmpleado(empleadoAnterior);
        
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        incidencia.setEmpleado(empleadoAnterior);
        
        com.gas.sistema_gas.Model.InventarioLote lote = new com.gas.sistema_gas.Model.InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(15));
        
        Empleado nuevoRepartidor = new Empleado();
        nuevoRepartidor.setId(2L);
        nuevoRepartidor.setNombre("Nuevo Motorizado");
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(empleadoRepository.findById(2L)).thenReturn(Optional.of(nuevoRepartidor));
        when(detallePedidoRepository.findByPedido_Id(1L)).thenReturn(List.of(detalle));
        when(inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(lote));
        
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La trazabilidad de lotes del pedido es inconsistente"))
            .when(inventarioLoteService).devolverStockDePedido(1L);
        
        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> incidenciaController.reasignarPedido(10L, Map.of("idNuevoRepartidor", 2L), session())
        );
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        // El pedido continúa CLIENTE_AUSENTE
        assertEquals("CLIENTE_AUSENTE", pedido.getEstadoPedido());
        // La incidencia continúa PENDIENTE
        assertEquals("PENDIENTE", incidencia.getEstado());
        // No guarda Producto
        verify(productoRepository, never()).save(any(Producto.class));
        // No guarda Pedido
        verify(pedidoRepository, never()).save(any(Pedido.class));
        // No guarda Incidencia
        verify(incidenciaRepository, never()).save(any(Incidencia.class));
        // No crea RespuestaIncidencia
        verify(respuestaIncidenciaRepository, never()).save(any());
        // No notifica
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.startsWith("/topic/pedidos/"), any(Object.class));
    }
    
    // PRUEBA 12: reasignarPedido sobre incidencia ATENDIDO retorna 409
    
    @Test
    void reasignarPedido_sobreATENDIDO_retorna409() {
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "ATENDIDO", pedido);
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        
        ResponseEntity<?> response = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("La incidencia ya fue atendida", body.get("message"));
        verify(pedidoRepository, never()).findByIdForUpdate(any());
        verify(productoRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
        verify(respuestaIncidenciaRepository, never()).save(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }
    
    // PRUEBA 13: reasignarPedido sobre tipo RECHAZO_POST_LLEGADA retorna 400
    
    @Test
    void reasignarPedido_sobreRECHAZO_POST_LLEGADA_retorna400() {
        Pedido pedido = pedido(1L, "EN_REVISION");
        Incidencia incidencia = incidencia(10L, "RECHAZO_POST_LLEGADA", "PENDIENTE", pedido);
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        
        ResponseEntity<?> response = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Solo se pueden reasignar incidencias de Cliente Ausente", body.get("message"));
        verify(pedidoRepository, never()).findByIdForUpdate(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }
    
    // PRUEBA 14: reasignarPedido sobre pedido EN_REVISION retorna 409
    
    @Test
    void reasignarPedido_cuandoPedidoNoEsCLIENTE_AUSENTE_retorna409() {
        Pedido pedido = pedido(1L, "EN_REVISION");
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        
        ResponseEntity<?> response = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("El pedido no está en estado CLIENTE_AUSENTE", body.get("message"));
        verify(productoRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
        verify(respuestaIncidenciaRepository, never()).save(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }
    
    // PRUEBA 15: reasignarPedido sin pedido asociado retorna 400
    
    @Test
    void reasignarPedido_sinPedidoAsociado_retorna400() {
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", null);
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        
        ResponseEntity<?> response = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("La incidencia no tiene un pedido asociado", body.get("message"));
        verify(pedidoRepository, never()).findByIdForUpdate(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
    }
    
    // PRUEBA 16: reasignarPedido con stock proyectado insuficiente retorna 409
    
    @Test
    void reasignarPedido_stockProyectadoInsuficiente_retorna409() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");
        producto.setStockReservado(BigDecimal.valueOf(3));
        producto.setStockLlenos(BigDecimal.valueOf(2));
        
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        pedido.setCodigo("PED-001");
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        empleado.setNombre("Motorizado");
        pedido.setEmpleado(empleado);
        
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        incidencia.setEmpleado(empleado);
        
        com.gas.sistema_gas.Model.InventarioLote lote = new com.gas.sistema_gas.Model.InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(2));
        
        Empleado nuevoRepartidor = new Empleado();
        nuevoRepartidor.setId(2L);
        nuevoRepartidor.setNombre("Nuevo Motorizado");
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(empleadoRepository.findById(2L)).thenReturn(Optional.of(nuevoRepartidor));
        when(detallePedidoRepository.findByPedido_Id(1L)).thenReturn(List.of(detalle));
        when(inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(lote));
        
        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> incidenciaController.reasignarPedido(10L, Map.of("idNuevoRepartidor", 2L), session())
        );
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Stock insuficiente para reasignar el pedido: Gas 10kg", exception.getReason());
        
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
        verify(productoRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
        verify(incidenciaRepository, never()).save(any());
        verify(respuestaIncidenciaRepository, never()).save(any());
    }
    
    // PRUEBA 17: reasignarPedido usa findByIdForUpdate y nunca findById
    
    @Test
    void reasignarPedido_usaFindByIdForUpdate() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");
        producto.setStockReservado(BigDecimal.ZERO);
        producto.setStockLlenos(BigDecimal.valueOf(15));
        
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        pedido.setCodigo("PED-001");
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        empleado.setNombre("Motorizado");
        pedido.setEmpleado(empleado);
        
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        incidencia.setEmpleado(empleado);
        
        com.gas.sistema_gas.Model.InventarioLote lote = new com.gas.sistema_gas.Model.InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(15));
        
        Empleado nuevoRepartidor = new Empleado();
        nuevoRepartidor.setId(2L);
        nuevoRepartidor.setNombre("Nuevo Motorizado");
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(empleadoRepository.findById(2L)).thenReturn(Optional.of(nuevoRepartidor));
        when(detallePedidoRepository.findByPedido_Id(1L)).thenReturn(List.of(detalle));
        when(inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(lote));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(respuestaIncidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        ResponseEntity<?> response = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(incidenciaRepository).findByIdForUpdate(10L);
        verify(pedidoRepository).findByIdForUpdate(1L);
        verify(incidenciaRepository, never()).findById(10L);
        verify(pedidoRepository, never()).findById(1L);
    }
    
    // PRUEBA 18: Doble ejecución - segunda ejecución retorna 409
    
    @Test
    void reasignarPedido_dobleEjecucion_segundaRetorna409() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Gas 10kg");
        producto.setStockReservado(BigDecimal.ZERO);
        producto.setStockLlenos(BigDecimal.valueOf(15));
        
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(5);
        
        Pedido pedido = pedido(1L, "CLIENTE_AUSENTE");
        pedido.setCodigo("PED-001");
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        empleado.setNombre("Motorizado");
        pedido.setEmpleado(empleado);
        
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);
        incidencia.setEmpleado(empleado);
        
        com.gas.sistema_gas.Model.InventarioLote lote = new com.gas.sistema_gas.Model.InventarioLote();
        lote.setId(1L);
        lote.setProducto(producto);
        lote.setCantidadActual(BigDecimal.valueOf(15));
        
        Empleado nuevoRepartidor = new Empleado();
        nuevoRepartidor.setId(2L);
        nuevoRepartidor.setNombre("Nuevo Motorizado");
        
        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(empleadoRepository.findById(2L)).thenReturn(Optional.of(nuevoRepartidor));
        when(detallePedidoRepository.findByPedido_Id(1L)).thenReturn(List.of(detalle));
        when(inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(lote));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(respuestaIncidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        // Primera ejecución
        ResponseEntity<?> response1 = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals("ATENDIDO", incidencia.getEstado());
        
        // Segunda ejecución
        ResponseEntity<?> response2 = incidenciaController.reasignarPedido(10L, 
            Map.of("idNuevoRepartidor", 2L), session());
        
        assertEquals(HttpStatus.CONFLICT, response2.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response2.getBody();
        assertEquals("La incidencia ya fue atendida", body.get("message"));
        
        // Verificar que la devolución se llamó exactamente una vez
        verify(inventarioLoteService, times(1)).devolverStockDePedido(1L);
        
        // Verificar que solo se creó una RespuestaIncidencia
        verify(respuestaIncidenciaRepository, times(1)).save(any());
    }
    
    // PRUEBA 10: marcarComoRevisado cuando el pedido no está CLIENTE_AUSENTE

    @Test
    void marcarComoRevisado_cuandoPedidoNoEsCLIENTE_AUSENTE_retorna409() {
        Pedido pedido = pedido(1L, "EN_DOMICILIO");
        Incidencia incidencia = incidencia(10L, "CLIENTE_AUSENTE", "PENDIENTE", pedido);

        when(incidenciaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(incidencia));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        ResponseEntity<?> response = incidenciaController.marcarComoRevisado(10L, session());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("El pedido no está en estado CLIENTE_AUSENTE", body.get("message"));
        verify(pedidoRepository, never()).save(any());
        verify(inventarioLoteService, never()).devolverStockDePedido(any());
        verify(productoRepository, never()).save(any());
    }
}