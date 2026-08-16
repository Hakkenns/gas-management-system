package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.dto.CajaDTO;
import com.gas.sistema_gas.service.CajaService;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class CajaControllerTest {

    @Mock
    private CajaService cajaService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private HttpSession session;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private CajaController controller;

    @Test
    void caja_conSesionYPermiso_retornaLayout() {
        configurarSesionConPermiso();
        Model model = new ExtendedModelMap();

        String vista = controller.caja(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/caja", model.getAttribute("contenido"));
        verify(opcionService).listByPerfilId(2L);
    }

    @Test
    void listarCustodias_conPermiso_retornaListado() {
        configurarSesionConPermiso();
        List<CajaDTO.CustodiaPendienteResponse> custodias = List.of(
                new CajaDTO.CustodiaPendienteResponse(7L, "Motorizado", new BigDecimal("25.00"), 0));
        when(cajaService.listarCustodiasPendientes()).thenReturn(custodias);

        ResponseEntity<?> response = controller.listarCustodias(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(custodias, response.getBody());
    }

    @Test
    void liquidarCustodia_usaUsuarioIdExclusivamenteDeSesion() {
        configurarSesionConPermiso();
        CajaDTO.LiquidacionRequest request = new CajaDTO.LiquidacionRequest(7L, new BigDecimal("10.00"), "Entrega");
        CajaDTO.LiquidacionResponse respuesta = new CajaDTO.LiquidacionResponse(
                31L, 32L, 20L, 7L, new BigDecimal("10.00"), new BigDecimal("25.00"),
                new BigDecimal("15.00"), null, "LIQ-TEST");
        when(cajaService.liquidarCustodiaMotorizado(request, 99L)).thenReturn(respuesta);

        ResponseEntity<?> response = controller.liquidarCustodia(request, bindingResult, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(respuesta, response.getBody());
        verify(cajaService).liquidarCustodiaMotorizado(request, 99L);
    }

    @Test
    void liquidacionRequest_noContieneUsuarioArbitrario() {
        assertEquals(List.of("empleadoCustodioId", "monto", "observacion"),
                java.util.Arrays.stream(CajaDTO.LiquidacionRequest.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
    }

    @Test
    void api_sesionAusente_retornaUnauthorized() {
        ResponseEntity<?> response = controller.listarCustodias(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(cajaService, never()).listarCustodiasPendientes();
    }

    @Test
    void api_sinPermiso_retornaForbidden() {
        configurarSesionAutenticada();
        when(opcionService.tieneAccesoRuta(2L, "caja")).thenReturn(false);

        ResponseEntity<?> response = controller.obtenerEstado(session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(cajaService, never()).obtenerEstadoCaja();
    }

    @Test
    void liquidarCustodia_requestInvalido_retornaBadRequest() {
        configurarSesionConPermiso();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(
                new org.springframework.validation.ObjectError("request", "Monto inválido")));

        ResponseEntity<?> response = controller.liquidarCustodia(
                new CajaDTO.LiquidacionRequest(7L, BigDecimal.ZERO, null), bindingResult, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(cajaService, never()).liquidarCustodiaMotorizado(any(), any());
    }

    @Test
    void liquidarCustodia_conflictoDelBackend_seConserva() {
        configurarSesionConPermiso();
        CajaDTO.LiquidacionRequest request = new CajaDTO.LiquidacionRequest(7L, new BigDecimal("30.00"), null);
        when(cajaService.liquidarCustodiaMotorizado(request, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Caja cerrada"));

        ResponseEntity<?> response = controller.liquidarCustodia(request, bindingResult, session);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    private void configurarSesionConPermiso() {
        configurarSesionAutenticada();
        when(opcionService.tieneAccesoRuta(2L, "caja")).thenReturn(true);
    }

    private void configurarSesionAutenticada() {
        when(session.getAttribute("usuarioLogueado")).thenReturn("cajero");
        when(session.getAttribute("usuarioId")).thenReturn(99L);
        when(session.getAttribute("usuarioPerfilId")).thenReturn(2L);
    }
}
