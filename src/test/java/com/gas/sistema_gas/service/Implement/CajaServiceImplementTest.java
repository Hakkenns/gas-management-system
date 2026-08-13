package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Caja;
import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.EstadoSesionCaja;
import com.gas.sistema_gas.Model.MovimientoCaja;
import com.gas.sistema_gas.Model.OrigenMovimiento;
import com.gas.sistema_gas.Model.SentidoMovimiento;
import com.gas.sistema_gas.Model.SesionCaja;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.CajaRepository;
import com.gas.sistema_gas.Repository.MovimientoCajaRepository;
import com.gas.sistema_gas.Repository.SesionCajaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.CajaDTO;

@ExtendWith(MockitoExtension.class)
class CajaServiceImplementTest {

    @Mock
    private CajaRepository cajaRepository;

    @Mock
    private SesionCajaRepository sesionCajaRepository;

    @Mock
    private MovimientoCajaRepository movimientoCajaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CajaServiceImplement cajaService;

    private Caja cajaPrincipal;
    private Usuario usuario;

    @BeforeEach
    void prepararEntidades() {
        cajaPrincipal = new Caja();
        cajaPrincipal.setId(1L);
        cajaPrincipal.setCodigo(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL);
        cajaPrincipal.setNombre("Caja principal");
        cajaPrincipal.setActiva(true);

        usuario = new Usuario();
        usuario.setId(10L);

    }

    private void configurarUsuarioYCajaPrincipal() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(cajaRepository.findByCodigoForUpdate(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL))
                .thenReturn(Optional.of(cajaPrincipal));
    }

    private void configurarAperturaDisponible() {
        when(sesionCajaRepository.findByCajaAndEstadoForUpdate(cajaPrincipal, EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.empty());
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(invocation -> {
            SesionCaja sesion = invocation.getArgument(0);
            sesion.setId(20L);
            return sesion;
        });
        when(movimientoCajaRepository.save(any(MovimientoCaja.class))).thenAnswer(invocation -> {
            MovimientoCaja movimiento = invocation.getArgument(0);
            movimiento.setId(30L);
            return movimiento;
        });
    }

    @Test
    void abrirCaja_conCienSoles_creaSesionYMovimientoApertura() {
        configurarUsuarioYCajaPrincipal();
        configurarAperturaDisponible();

        CajaDTO.AperturaResponse response = cajaService.abrirCaja(10L, new BigDecimal("100.00"), "Fondo inicial");

        ArgumentCaptor<SesionCaja> sesionCaptor = ArgumentCaptor.forClass(SesionCaja.class);
        ArgumentCaptor<MovimientoCaja> movimientoCaptor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(sesionCajaRepository).save(sesionCaptor.capture());
        verify(movimientoCajaRepository).save(movimientoCaptor.capture());

        SesionCaja sesion = sesionCaptor.getValue();
        MovimientoCaja movimiento = movimientoCaptor.getValue();
        assertEquals(cajaPrincipal, sesion.getCaja());
        assertEquals(EstadoSesionCaja.ABIERTA, sesion.getEstado());
        assertEquals(usuario, sesion.getUsuarioApertura());
        assertEquals("Fondo inicial", sesion.getObservaciones());
        assertEquals(cajaPrincipal, movimiento.getCaja());
        assertEquals(sesion, movimiento.getSesionCaja());
        assertEquals(SentidoMovimiento.INGRESO, movimiento.getSentido());
        assertEquals(OrigenMovimiento.APERTURA, movimiento.getOrigen());
        assertEquals(CanalFondos.CAJA_FISICA, movimiento.getCanalFondos());
        assertEquals(0, new BigDecimal("100.00").compareTo(movimiento.getMonto()));
        assertEquals(usuario, movimiento.getUsuarioResponsable());
        assertEquals(20L, response.idSesionCaja());
        assertEquals(30L, response.idMovimientoCaja());
    }

    @Test
    void abrirCaja_conCero_creaSesionYMovimientoAperturaDeCero() {
        configurarUsuarioYCajaPrincipal();
        configurarAperturaDisponible();

        CajaDTO.AperturaResponse response = cajaService.abrirCaja(10L, BigDecimal.ZERO, null);

        ArgumentCaptor<MovimientoCaja> movimientoCaptor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(sesionCajaRepository).save(any(SesionCaja.class));
        verify(movimientoCajaRepository).save(movimientoCaptor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(movimientoCaptor.getValue().getMonto()));
        assertEquals(OrigenMovimiento.APERTURA, movimientoCaptor.getValue().getOrigen());
        assertEquals(BigDecimal.ZERO, response.montoInicial());
    }

    @Test
    void abrirCaja_montoNegativo_rechazaSinCrearSesionNiMovimiento() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.abrirCaja(10L, new BigDecimal("-0.01"), null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(usuarioRepository, never()).findById(any());
        verify(sesionCajaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void abrirCaja_cajaInactiva_rechazaSinCrearSesionNiMovimiento() {
        configurarUsuarioYCajaPrincipal();
        cajaPrincipal.setActiva(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.abrirCaja(10L, BigDecimal.ZERO, null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(sesionCajaRepository, never()).findByCajaAndEstadoForUpdate(any(), any());
        verify(sesionCajaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void abrirCaja_sesionAbierta_rechazaSinCrearOtraSesionNiMovimiento() {
        configurarUsuarioYCajaPrincipal();
        SesionCaja sesionExistente = new SesionCaja();
        sesionExistente.setEstado(EstadoSesionCaja.ABIERTA);
        when(sesionCajaRepository.findByCajaAndEstadoForUpdate(cajaPrincipal, EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.of(sesionExistente));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.abrirCaja(10L, BigDecimal.ZERO, null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(sesionCajaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void abrirCaja_usuarioInexistente_rechazaSinCrearSesionNiMovimiento() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.abrirCaja(99L, BigDecimal.ZERO, null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(cajaRepository, never()).findByCodigoForUpdate(any());
        verify(sesionCajaRepository, never()).save(any());
        verify(movimientoCajaRepository, never()).save(any());
    }
}
