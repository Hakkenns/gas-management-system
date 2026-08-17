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

    @Test
    void abrirCaja_usaUsuarioIdExclusivamenteDeSesion() {
        configurarSesionConPermiso();
        CajaDTO.AperturaRequest request = new CajaDTO.AperturaRequest(new BigDecimal("10.00"), "Fondo inicial");
        CajaDTO.AperturaResponse respuesta = new CajaDTO.AperturaResponse(
                20L, 30L, "CAJA_PRINCIPAL", null, new BigDecimal("10.00"), "ABIERTA");
        when(cajaService.abrirCaja(99L, request.montoInicial(), request.observaciones())).thenReturn(respuesta);

        ResponseEntity<?> response = controller.abrirCaja(request, bindingResult, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(respuesta, response.getBody());
        verify(cajaService).abrirCaja(99L, request.montoInicial(), request.observaciones());
    }

    @Test
    void abrirCaja_requestInvalido_retornaBadRequest() {
        configurarSesionConPermiso();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(
                new org.springframework.validation.ObjectError("request", "Monto inválido")));

        ResponseEntity<?> response = controller.abrirCaja(
                new CajaDTO.AperturaRequest(new BigDecimal("1.001"), null), bindingResult, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(cajaService, never()).abrirCaja(any(), any(), any());
    }

    @Test
    void abrirCaja_sinSesion_retornaUnauthorized() {
        ResponseEntity<?> response = controller.abrirCaja(
                new CajaDTO.AperturaRequest(BigDecimal.ZERO, null), bindingResult, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(cajaService, never()).abrirCaja(any(), any(), any());
    }

    @Test
    void abrirCaja_sinPermiso_retornaForbidden() {
        configurarSesionAutenticada();
        when(opcionService.tieneAccesoRuta(2L, "caja")).thenReturn(false);

        ResponseEntity<?> response = controller.abrirCaja(
                new CajaDTO.AperturaRequest(BigDecimal.ZERO, null), bindingResult, session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(cajaService, never()).abrirCaja(any(), any(), any());
    }

    @Test
    void abrirCaja_conflictoDelBackend_seConserva() {
        configurarSesionConPermiso();
        CajaDTO.AperturaRequest request = new CajaDTO.AperturaRequest(BigDecimal.ZERO, null);
        when(cajaService.abrirCaja(99L, request.montoInicial(), request.observaciones()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Caja ya abierta"));

        ResponseEntity<?> response = controller.abrirCaja(request, bindingResult, session);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void cerrarCaja_usaUsuarioIdExclusivamenteDeSesion() {
        configurarSesionConPermiso();
        CajaDTO.CierreRequest request = new CajaDTO.CierreRequest(new BigDecimal("10.00"), null);
        CajaDTO.CierreResponse respuesta = new CajaDTO.CierreResponse(
                20L, "CAJA_PRINCIPAL", null, new BigDecimal("10.00"), new BigDecimal("10.00"),
                BigDecimal.ZERO, "CERRADA");
        when(cajaService.cerrarCaja(99L, request.montoDeclarado(), request.observaciones())).thenReturn(respuesta);

        ResponseEntity<?> response = controller.cerrarCaja(request, bindingResult, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(respuesta, response.getBody());
        verify(cajaService).cerrarCaja(99L, request.montoDeclarado(), request.observaciones());
    }

    @Test
    void cerrarCaja_requestInvalido_retornaBadRequest() {
        configurarSesionConPermiso();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(
                new org.springframework.validation.ObjectError("request", "Monto inválido")));

        ResponseEntity<?> response = controller.cerrarCaja(
                new CajaDTO.CierreRequest(new BigDecimal("1.001"), null), bindingResult, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(cajaService, never()).cerrarCaja(any(), any(), any());
    }

    @Test
    void cerrarCaja_sinSesion_retornaUnauthorized() {
        ResponseEntity<?> response = controller.cerrarCaja(
                new CajaDTO.CierreRequest(BigDecimal.ZERO, null), bindingResult, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(cajaService, never()).cerrarCaja(any(), any(), any());
    }

    @Test
    void cerrarCaja_sinPermiso_retornaForbidden() {
        configurarSesionAutenticada();
        when(opcionService.tieneAccesoRuta(2L, "caja")).thenReturn(false);

        ResponseEntity<?> response = controller.cerrarCaja(
                new CajaDTO.CierreRequest(BigDecimal.ZERO, null), bindingResult, session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(cajaService, never()).cerrarCaja(any(), any(), any());
    }

    @Test
    void cerrarCaja_conflictoDelBackend_seConserva() {
        configurarSesionConPermiso();
        CajaDTO.CierreRequest request = new CajaDTO.CierreRequest(BigDecimal.ZERO, null);
        when(cajaService.cerrarCaja(99L, request.montoDeclarado(), request.observaciones()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Caja cerrada"));

        ResponseEntity<?> response = controller.cerrarCaja(request, bindingResult, session);

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
