package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    // PRUEBA 2: confirmarRechazo válido

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
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
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
        verify(respuestaIncidenciaRepository).save(any(RespuestaIncidencia.class));
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
        verify(inventarioLoteRepository, never()).findByProductoIdOrderByCreatedAtDesc(any());
        verify(productoRepository, never()).save(any());
    }
}