package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;
import com.gas.sistema_gas.service.PedidoPagosService;
import com.gas.sistema_gas.service.PedidoService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class MotorizadoControllerTest {

    @Mock
    private PedidoService pedidoService;

    @Mock
    private PedidoPagosService pedidoPagosService;

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
