package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;
import com.gas.sistema_gas.dto.MetodoPagoDTO;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.Model.TipoFinancieroMetodoPago;
import com.gas.sistema_gas.service.PedidoPagosService;
import com.gas.sistema_gas.service.PedidoService;
import com.gas.sistema_gas.service.MetodoPagoService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class MotorizadoControllerTest {

    @Mock
    private PedidoService pedidoService;

    @Mock
    private PedidoPagosService pedidoPagosService;

    @Mock
    private MetodoPagoService metodoPagoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HttpSession session;

    @InjectMocks
    private MotorizadoController controller;

    @Test
    void pagarYape_pedidoAsignadoAlMotorizado_continuaFlujo() {
        configurarSesionConEmpleado(7L);
        when(pedidoService.existsByIdAndEmpleadoId(41L, 7L)).thenReturn(true);

        PedidoPago pago = new PedidoPago();
        pago.setId(99L);
        when(pedidoPagosService.confirmarEntregaPagoUnico(
                any(PedidoPagoYapeDTO.class), any(), any())).thenReturn(pago);

        Object resultado = controller.pagarYape(41L, 1L, new BigDecimal("10.00"), "",
                null, null, session, "XMLHttpRequest");

        ResponseEntity<?> response = (ResponseEntity<?>) resultado;
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pedidoPagosService).confirmarEntregaPagoUnico(any(PedidoPagoYapeDTO.class), any(), any());
    }

    @Test
    void pagarYape_delegaValidacionDeOperacionAlServicioSinUsarIdsFijos() {
        configurarSesionConEmpleado(7L);
        when(pedidoService.existsByIdAndEmpleadoId(41L, 7L)).thenReturn(true);
        PedidoPago pago = new PedidoPago();
        pago.setId(99L);
        when(pedidoPagosService.confirmarEntregaPagoUnico(any(PedidoPagoYapeDTO.class), any(), any()))
                .thenReturn(pago);

        Object resultado = controller.pagarYape(41L, 2L, new BigDecimal("10.00"), null,
                null, null, session, "XMLHttpRequest");

        assertEquals(HttpStatus.OK, ((ResponseEntity<?>) resultado).getStatusCode());
        verify(pedidoPagosService).confirmarEntregaPagoUnico(any(PedidoPagoYapeDTO.class), any(), any());
    }

    @Test
    void confirmarEntrega_preservaResponseStatusException() {
        configurarSesionConEmpleado(7L);
        when(pedidoService.existsByIdAndEmpleadoId(41L, 7L)).thenReturn(true);
        when(pedidoPagosService.confirmarEntregaConPagos(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Método de pago inactivo"));

        ResponseEntity<?> response = controller.confirmarEntrega(41L,
                "[{\"idMetodo\":1,\"monto\":10.00,\"numOperacion\":\"\"}]", null, null, session);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void detalle_pedidoAsignado_agregaMetodosPagoActivosAlModelo() {
        configurarSesionConEmpleado(7L);
        PedidoDTO.SimpleResponse pedido = new PedidoDTO.SimpleResponse(
                41L, null, null, null, null, 7L, null, null, null, null,
                null, null, null, null, null, List.of());
        PedidoDTO.EditResponse editData = new PedidoDTO.EditResponse(
                41L, null, null, null, null, null, null, null, 7L, null,
                null, null, null, List.of(), List.of());
        when(pedidoService.findByIdAndEmpleadoId(41L, 7L)).thenReturn(pedido);
        when(pedidoService.getEditData(41L)).thenReturn(editData);
        List<MetodoPagoDTO.Response> metodos = List.of(
                new MetodoPagoDTO.Response(8L, "BILLETERA", "Billetera", TipoFinancieroMetodoPago.DIGITAL, 1));
        when(metodoPagoService.listActive()).thenReturn(metodos);

        Model model = new ExtendedModelMap();
        String vista = controller.detalle(41L, null, model, session);

        assertEquals("repartidor/detalle", vista);
        assertEquals(metodos, model.getAttribute("metodosPago"));
        verify(metodoPagoService).listActive();
    }

    @Test
    void pagarYape_pedidoDeOtroMotorizado_rechazaAntesDelPago() {
        configurarSesionConEmpleado(7L);
        when(pedidoService.existsByIdAndEmpleadoId(41L, 7L)).thenReturn(false);

        Object resultado = controller.pagarYape(41L, 1L, new BigDecimal("10.00"), "",
                null, null, session, "XMLHttpRequest");

        ResponseEntity<?> response = (ResponseEntity<?>) resultado;
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(pedidoPagosService, never()).confirmarEntregaPagoUnico(any(PedidoPagoYapeDTO.class), any(), any());
    }

    @Test
    void pagarYape_pedidoNoAsignado_rechazaAunqueExistaSesionValida() {
        configurarSesionConEmpleado(7L);
        when(pedidoService.existsByIdAndEmpleadoId(eq(99L), eq(7L))).thenReturn(false);

        Object resultado = controller.pagarYape(99L, 1L, new BigDecimal("10.00"), "",
                null, null, session, "XMLHttpRequest");

        ResponseEntity<?> response = (ResponseEntity<?>) resultado;
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(pedidoPagosService, never()).confirmarEntregaPagoUnico(any(PedidoPagoYapeDTO.class), any(), any());
    }

    private void configurarSesionConEmpleado(Long empleadoId) {
        Empleado empleado = new Empleado();
        empleado.setId(empleadoId);
        Usuario usuario = new Usuario();
        usuario.setEmpleado(empleado);
        when(session.getAttribute("usuarioLogueado")).thenReturn("moto");
        when(session.getAttribute("usuarioId")).thenReturn(5L);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
    }
}
