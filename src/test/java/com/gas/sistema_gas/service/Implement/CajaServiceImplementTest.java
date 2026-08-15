package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Caja;
import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.EstadoSesionCaja;
import com.gas.sistema_gas.Model.MovimientoCaja;
import com.gas.sistema_gas.Model.OrigenMovimiento;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.SentidoMovimiento;
import com.gas.sistema_gas.Model.SesionCaja;
import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.TipoFinancieroMetodoPago;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.CajaRepository;
import com.gas.sistema_gas.Repository.MovimientoCajaRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
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
    private PedidoPagoRepository pedidoPagoRepository;

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
        org.mockito.Mockito.lenient().when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
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

    private SesionCaja configurarCierreDisponible(BigDecimal efectivoEsperado) {
        configurarUsuarioYCajaPrincipal();

        SesionCaja sesion = new SesionCaja();
        sesion.setId(20L);
        sesion.setCaja(cajaPrincipal);
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        sesion.setObservaciones("Fondo inicial");
        when(sesionCajaRepository.findByCajaAndEstadoForUpdate(cajaPrincipal, EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.of(sesion));
        when(movimientoCajaRepository.calcularSaldoPorSesionYCanal(
                sesion,
                CanalFondos.CAJA_FISICA,
                SentidoMovimiento.INGRESO,
                SentidoMovimiento.EGRESO)).thenReturn(efectivoEsperado);
        return sesion;
    }

    private void configurarGuardadoCierre() {
        when(sesionCajaRepository.save(any(SesionCaja.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SesionCaja configurarSesionAbiertaParaIngresos() {
        configurarUsuarioYCajaPrincipal();
        SesionCaja sesion = new SesionCaja();
        sesion.setId(20L);
        sesion.setCaja(cajaPrincipal);
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        when(sesionCajaRepository.findByCajaAndEstadoForUpdate(cajaPrincipal, EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.of(sesion));
        org.mockito.Mockito.lenient().when(movimientoCajaRepository.findByPedidoPagoForUpdate(any(PedidoPago.class)))
                .thenReturn(Optional.empty());
        return sesion;
    }

    private PedidoPago pagoLocal(Long id, TipoFinancieroMetodoPago tipo, String monto) {
        Pedido pedido = new Pedido();
        pedido.setId(40L);
        pedido.setCodigo("NV001-0040");
        pedido.setTipoVenta("LOCAL");
        MetodoPago metodo = new MetodoPago();
        metodo.setId(id + 100L);
        metodo.setTipoFinanciero(tipo);
        PedidoPago pago = new PedidoPago();
        pago.setId(id);
        pago.setPedido(pedido);
        pago.setMetodoPago(metodo);
        pago.setMonto(new BigDecimal(monto));
        return pago;
    }

    private PedidoPago pagoDomicilio(Long id, TipoFinancieroMetodoPago tipo, String monto, Long idPedido) {
        Pedido pedido = new Pedido();
        pedido.setId(idPedido);
        pedido.setCodigo("NV001-" + idPedido);
        pedido.setTipoVenta("DOMICILIO");
        Empleado empleado = new Empleado();
        empleado.setId(70L);
        pedido.setEmpleado(empleado);
        MetodoPago metodo = new MetodoPago();
        metodo.setId(id + 100L);
        metodo.setTipoFinanciero(tipo);
        PedidoPago pago = new PedidoPago();
        pago.setId(id);
        pago.setPedido(pedido);
        pago.setMetodoPago(metodo);
        pago.setMonto(new BigDecimal(monto));
        return pago;
    }

    private void configurarRegistroDomicilio(List<PedidoPago> pagos) {
        when(cajaRepository.findByCodigo(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL))
                .thenReturn(Optional.of(cajaPrincipal));
        org.mockito.Mockito.lenient().when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        List<PedidoPago> ordenados = pagos.stream()
                .sorted(java.util.Comparator.comparing(PedidoPago::getId))
                .toList();
        List<Long> ids = ordenados.stream().map(PedidoPago::getId).toList();
        when(pedidoPagoRepository.findAllByIdInForUpdate(ids)).thenReturn(ordenados);
        org.mockito.Mockito.lenient().when(movimientoCajaRepository.findByPedidoPago(any(PedidoPago.class)))
                .thenReturn(Optional.empty());
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

    @Test
    void cerrarCaja_cierreExacto_guardaSnapshotsYEstadoCerrada() {
        SesionCaja sesion = configurarCierreDisponible(new BigDecimal("100.00"));
        configurarGuardadoCierre();

        CajaDTO.CierreResponse response = cajaService.cerrarCaja(10L, new BigDecimal("100"), null);

        assertEquals(EstadoSesionCaja.CERRADA, sesion.getEstado());
        assertEquals(0, sesion.getMontoEsperadoCierre().compareTo(new BigDecimal("100.00")));
        assertEquals(0, sesion.getMontoDeclaradoCierre().compareTo(new BigDecimal("100.00")));
        assertEquals(0, sesion.getDiferenciaCierre().compareTo(BigDecimal.ZERO));
        assertEquals(usuario, sesion.getUsuarioCierre());
        assertEquals("Fondo inicial", sesion.getObservaciones());
        assertEquals(20L, response.idSesionCaja());
        assertEquals("CAJA_PRINCIPAL", response.codigoCaja());
        assertEquals("CERRADA", response.estado());
        verify(sesionCajaRepository).save(sesion);
    }

    @Test
    void cerrarCaja_sobrante_conNota_loPermite() {
        SesionCaja sesion = configurarCierreDisponible(new BigDecimal("100.00"));
        configurarGuardadoCierre();

        CajaDTO.CierreResponse response = cajaService.cerrarCaja(10L, new BigDecimal("110.00"), "Sobrante contado");

        assertEquals(0, response.diferencia().compareTo(new BigDecimal("10.00")));
        assertEquals(EstadoSesionCaja.CERRADA, sesion.getEstado());
        assertEquals("Fondo inicial\nCierre: Sobrante contado", sesion.getObservaciones());
        verify(sesionCajaRepository).save(sesion);
    }

    @Test
    void cerrarCaja_faltante_conNota_loPermite() {
        SesionCaja sesion = configurarCierreDisponible(new BigDecimal("100.00"));
        configurarGuardadoCierre();

        CajaDTO.CierreResponse response = cajaService.cerrarCaja(10L, new BigDecimal("90.00"), "Faltante contado");

        assertEquals(0, response.diferencia().compareTo(new BigDecimal("-10.00")));
        assertEquals(EstadoSesionCaja.CERRADA, sesion.getEstado());
        verify(sesionCajaRepository).save(sesion);
    }

    @Test
    void cerrarCaja_diferenciaSinObservacion_rechazaSinGuardarCierre() {
        SesionCaja sesion = configurarCierreDisponible(new BigDecimal("100.00"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.cerrarCaja(10L, new BigDecimal("90.00"), "   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(EstadoSesionCaja.ABIERTA, sesion.getEstado());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    void cerrarCaja_montoNegativo_rechazaAntesDeConsultar() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.cerrarCaja(10L, new BigDecimal("-0.01"), null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(usuarioRepository, never()).findById(any());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    void cerrarCaja_montoConMasDeDosDecimales_rechazaSinRedondear() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.cerrarCaja(10L, new BigDecimal("100.001"), null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(usuarioRepository, never()).findById(any());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    void cerrarCaja_usuarioInexistente_rechazaSinCerrarSesion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        when(cajaRepository.findByCodigoForUpdate(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL))
                .thenReturn(Optional.of(cajaPrincipal));
        SesionCaja sesion = new SesionCaja();
        sesion.setCaja(cajaPrincipal);
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        when(sesionCajaRepository.findByCajaAndEstadoForUpdate(cajaPrincipal, EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.of(sesion));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.cerrarCaja(99L, new BigDecimal("100.00"), null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    void cerrarCaja_cajaInactiva_rechazaSinCerrarSesion() {
        configurarUsuarioYCajaPrincipal();
        cajaPrincipal.setActiva(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.cerrarCaja(10L, new BigDecimal("100.00"), null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(sesionCajaRepository, never()).findByCajaAndEstadoForUpdate(any(), any());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    void cerrarCaja_sinSesionAbierta_rechazaSinGuardarCierre() {
        configurarUsuarioYCajaPrincipal();
        when(sesionCajaRepository.findByCajaAndEstadoForUpdate(cajaPrincipal, EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.cerrarCaja(10L, new BigDecimal("100.00"), null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).calcularSaldoPorSesionYCanal(any(), any(), any(), any());
        verify(sesionCajaRepository, never()).save(any());
    }

    @Test
    void cerrarCaja_conservaObservacionAperturaYAnexaNotaCierre() {
        SesionCaja sesion = configurarCierreDisponible(new BigDecimal("100.00"));
        configurarGuardadoCierre();

        cajaService.cerrarCaja(10L, new BigDecimal("100.00"), "Arqueo correcto");

        assertEquals("Fondo inicial\nCierre: Arqueo correcto", sesion.getObservaciones());
        verify(sesionCajaRepository).save(sesion);
    }

    @Test
    void registrarIngresosVentaLocal_efectivo_creaIngresoFisicoTrazable() {
        SesionCaja sesion = configurarSesionAbiertaParaIngresos();
        PedidoPago pago = pagoLocal(41L, TipoFinancieroMetodoPago.EFECTIVO, "35.00");

        cajaService.registrarIngresosVentaLocal(List.of(pago), 10L);

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository).save(captor.capture());
        MovimientoCaja movimiento = captor.getValue();
        assertEquals(SentidoMovimiento.INGRESO, movimiento.getSentido());
        assertEquals(OrigenMovimiento.VENTA, movimiento.getOrigen());
        assertEquals(CanalFondos.CAJA_FISICA, movimiento.getCanalFondos());
        assertEquals(0, pago.getMonto().compareTo(movimiento.getMonto()));
        assertEquals(pago, movimiento.getPedidoPago());
        assertEquals(pago.getMetodoPago(), movimiento.getMetodoPago());
        assertEquals(sesion, movimiento.getSesionCaja());
        assertEquals(usuario, movimiento.getUsuarioResponsable());
        assertEquals("NV001-0040", movimiento.getReferencia());
    }

    @Test
    void registrarIngresosVentaLocal_digital_asociaLaMismaSesionSinAfectarCanalFisico() {
        SesionCaja sesion = configurarSesionAbiertaParaIngresos();
        PedidoPago pago = pagoLocal(42L, TipoFinancieroMetodoPago.DIGITAL, "60.00");

        cajaService.registrarIngresosVentaLocal(List.of(pago), 10L);

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository).save(captor.capture());
        assertEquals(CanalFondos.DIGITAL_NEGOCIO, captor.getValue().getCanalFondos());
        assertEquals(sesion, captor.getValue().getSesionCaja());
    }

    @Test
    void registrarIngresosVentaLocal_mixto_creaUnMovimientoPorPago() {
        configurarSesionAbiertaParaIngresos();
        PedidoPago efectivo = pagoLocal(43L, TipoFinancieroMetodoPago.EFECTIVO, "40.00");
        PedidoPago digital = pagoLocal(44L, TipoFinancieroMetodoPago.DIGITAL, "60.00");

        cajaService.registrarIngresosVentaLocal(List.of(efectivo, digital), 10L);

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<MovimientoCaja> movimientos = captor.getAllValues();

        assertEquals(2, movimientos.size(), "Debe crear exactamente dos movimientos");
        assertEquals(CanalFondos.CAJA_FISICA, movimientos.get(0).getCanalFondos());
        assertEquals(0, new BigDecimal("40.00").compareTo(movimientos.get(0).getMonto()));
        assertEquals(efectivo, movimientos.get(0).getPedidoPago());
        assertEquals(CanalFondos.DIGITAL_NEGOCIO, movimientos.get(1).getCanalFondos());
        assertEquals(0, new BigDecimal("60.00").compareTo(movimientos.get(1).getMonto()));
        assertEquals(digital, movimientos.get(1).getPedidoPago());
    }

    @Test
    void registrarIngresosVentaLocal_sinSesion_rechazaSinGuardarMovimientos() {
        configurarUsuarioYCajaPrincipal();
        when(sesionCajaRepository.findByCajaAndEstadoForUpdate(cajaPrincipal, EstadoSesionCaja.ABIERTA))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaLocal(
                        List.of(pagoLocal(45L, TipoFinancieroMetodoPago.EFECTIVO, "10.00")), 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void registrarIngresosVentaLocal_pagoDuplicado_rechazaSinGuardarMovimientos() {
        configurarSesionAbiertaParaIngresos();
        PedidoPago pago = pagoLocal(46L, TipoFinancieroMetodoPago.EFECTIVO, "10.00");
        when(movimientoCajaRepository.findByPedidoPagoForUpdate(pago))
                .thenReturn(Optional.of(new MovimientoCaja()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaLocal(List.of(pago), 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void registrarIngresosVentaLocal_listaConSegundoDuplicado_noGuardaElPrimero() {
        configurarSesionAbiertaParaIngresos();
        PedidoPago primero = pagoLocal(47L, TipoFinancieroMetodoPago.EFECTIVO, "10.00");
        PedidoPago segundo = pagoLocal(48L, TipoFinancieroMetodoPago.DIGITAL, "20.00");
        when(movimientoCajaRepository.findByPedidoPagoForUpdate(segundo))
                .thenReturn(Optional.of(new MovimientoCaja()));

        assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaLocal(List.of(primero, segundo), 10L));

        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void registrarIngresosVentaLocal_sinTipoFinanciero_rechazaSinGuardarMovimientos() {
        configurarSesionAbiertaParaIngresos();
        PedidoPago pago = pagoLocal(49L, TipoFinancieroMetodoPago.EFECTIVO, "10.00");
        pago.getMetodoPago().setTipoFinanciero(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaLocal(List.of(pago), 10L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void registrarIngresosVentaLocal_montoNoPositivo_rechazaSinGuardarMovimientos() {
        configurarSesionAbiertaParaIngresos();
        PedidoPago pago = pagoLocal(50L, TipoFinancieroMetodoPago.EFECTIVO, "0.00");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaLocal(List.of(pago), 10L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void registrarIngresosVentaDomicilio_efectivo_creaCustodiaSinSesion() {
        PedidoPago pago = pagoDomicilio(61L, TipoFinancieroMetodoPago.EFECTIVO, "30.00", 81L);
        configurarRegistroDomicilio(List.of(pago));

        cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L);

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository).saveAndFlush(captor.capture());
        MovimientoCaja movimiento = captor.getValue();
        assertEquals(SentidoMovimiento.INGRESO, movimiento.getSentido());
        assertEquals(OrigenMovimiento.VENTA, movimiento.getOrigen());
        assertEquals(CanalFondos.CUSTODIA_MOTORIZADO, movimiento.getCanalFondos());
        assertEquals(0, pago.getMonto().compareTo(movimiento.getMonto()));
        assertEquals(pago.getPedido().getEmpleado(), movimiento.getEmpleadoCustodio());
        assertEquals(null, movimiento.getSesionCaja());
        verify(sesionCajaRepository, never()).findByCajaAndEstadoForUpdate(any(), any());
    }

    @Test
    void registrarIngresosVentaDomicilio_digital_creaIngresoDigitalSinCustodio() {
        PedidoPago pago = pagoDomicilio(62L, TipoFinancieroMetodoPago.DIGITAL, "30.00", 81L);
        configurarRegistroDomicilio(List.of(pago));

        cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L);

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository).saveAndFlush(captor.capture());
        assertEquals(CanalFondos.DIGITAL_NEGOCIO, captor.getValue().getCanalFondos());
        assertEquals(null, captor.getValue().getSesionCaja());
        assertEquals(null, captor.getValue().getEmpleadoCustodio());
    }

    @Test
    void registrarIngresosVentaDomicilio_mixto_creaUnMovimientoPorPago() {
        PedidoPago efectivo = pagoDomicilio(63L, TipoFinancieroMetodoPago.EFECTIVO, "20.00", 81L);
        PedidoPago digital = pagoDomicilio(64L, TipoFinancieroMetodoPago.DIGITAL, "30.00", 81L);
        configurarRegistroDomicilio(List.of(digital, efectivo));

        cajaService.registrarIngresosVentaDomicilio(List.of(digital, efectivo), 10L);

        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
        List<MovimientoCaja> movimientos = captor.getAllValues();
        assertEquals(2, movimientos.size());

        MovimientoCaja movimientoEfectivo = movimientos.stream()
                .filter(movimiento -> movimiento.getPedidoPago() == efectivo)
                .findFirst()
                .orElseThrow();
        MovimientoCaja movimientoDigital = movimientos.stream()
                .filter(movimiento -> movimiento.getPedidoPago() == digital)
                .findFirst()
                .orElseThrow();
        assertEquals(1L, movimientos.stream()
                .filter(movimiento -> movimiento.getPedidoPago() == efectivo)
                .count());
        assertEquals(1L, movimientos.stream()
                .filter(movimiento -> movimiento.getPedidoPago() == digital)
                .count());

        assertEquals(CanalFondos.CUSTODIA_MOTORIZADO, movimientoEfectivo.getCanalFondos());
        assertSame(efectivo, movimientoEfectivo.getPedidoPago());
        assertSame(efectivo.getMetodoPago(), movimientoEfectivo.getMetodoPago());
        assertEquals(0, efectivo.getMonto().compareTo(movimientoEfectivo.getMonto()));
        assertSame(efectivo.getPedido().getEmpleado(), movimientoEfectivo.getEmpleadoCustodio());
        assertEquals(null, movimientoEfectivo.getSesionCaja());
        assertEquals(SentidoMovimiento.INGRESO, movimientoEfectivo.getSentido());
        assertEquals(OrigenMovimiento.VENTA, movimientoEfectivo.getOrigen());

        assertEquals(CanalFondos.DIGITAL_NEGOCIO, movimientoDigital.getCanalFondos());
        assertSame(digital, movimientoDigital.getPedidoPago());
        assertSame(digital.getMetodoPago(), movimientoDigital.getMetodoPago());
        assertEquals(0, digital.getMonto().compareTo(movimientoDigital.getMonto()));
        assertEquals(null, movimientoDigital.getEmpleadoCustodio());
        assertEquals(null, movimientoDigital.getSesionCaja());
        assertEquals(SentidoMovimiento.INGRESO, movimientoDigital.getSentido());
        assertEquals(OrigenMovimiento.VENTA, movimientoDigital.getOrigen());
    }

    @Test
    void registrarIngresosVentaDomicilio_pagoInvalido_rechazaSinGuardar() {
        PedidoPago pago = pagoDomicilio(65L, TipoFinancieroMetodoPago.EFECTIVO, "0.00", 81L);
        configurarRegistroDomicilio(List.of(pago));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarIngresosVentaDomicilio_tipoNulo_rechazaSinGuardar() {
        PedidoPago pago = pagoDomicilio(66L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        pago.getMetodoPago().setTipoFinanciero(null);
        configurarRegistroDomicilio(List.of(pago));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarIngresosVentaDomicilio_pedidoNoDomicilio_rechazaSinGuardar() {
        PedidoPago pago = pagoDomicilio(67L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        pago.getPedido().setTipoVenta("LOCAL");
        configurarRegistroDomicilio(List.of(pago));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarIngresosVentaDomicilio_pedidoSinEmpleado_rechazaSinGuardar() {
        PedidoPago pago = pagoDomicilio(67L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        pago.getPedido().setEmpleado(null);
        configurarRegistroDomicilio(List.of(pago));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarIngresosVentaDomicilio_pagoDuplicado_rechazaSinGuardar() {
        PedidoPago pago = pagoDomicilio(68L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        configurarRegistroDomicilio(List.of(pago));
        when(movimientoCajaRepository.findByPedidoPago(pago)).thenReturn(Optional.of(new MovimientoCaja()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(movimientoCajaRepository, never()).saveAndFlush(any());
        verify(movimientoCajaRepository, never()).findByPedidoPagoForUpdate(any());
    }

    @Test
    void registrarIngresosVentaDomicilio_uniquePedidoPago_traduceAConflict() {
        PedidoPago pago = pagoDomicilio(73L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        configurarRegistroDomicilio(List.of(pago));
        org.hibernate.exception.ConstraintViolationException constraintViolation =
                new org.hibernate.exception.ConstraintViolationException(
                        "Violacion de uk_movimiento_caja_pedido_pago",
                        new SQLException("Constraint uk_movimiento_caja_pedido_pago", "23000", 1062),
                        "uk_movimiento_caja_pedido_pago");
        DataIntegrityViolationException integrityException =
                new DataIntegrityViolationException("No se pudo insertar MovimientoCaja", constraintViolation);
        when(movimientoCajaRepository.saveAndFlush(any(MovimientoCaja.class))).thenThrow(integrityException);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(null, exception.getCause());
    }

    @Test
    void registrarIngresosVentaDomicilio_otraIntegridad_noLaTraduceAConflict() {
        PedidoPago pago = pagoDomicilio(74L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        configurarRegistroDomicilio(List.of(pago));
        org.hibernate.exception.ConstraintViolationException constraintViolation =
                new org.hibernate.exception.ConstraintViolationException(
                        "Violacion de otra restriccion",
                        new SQLException("Constraint uk_otra_restriccion", "23000", 1062),
                        "uk_otra_restriccion");
        DataIntegrityViolationException integrityException =
                new DataIntegrityViolationException("No se pudo insertar MovimientoCaja", constraintViolation);
        when(movimientoCajaRepository.saveAndFlush(any(MovimientoCaja.class))).thenThrow(integrityException);

        DataIntegrityViolationException propagada = assertThrows(DataIntegrityViolationException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));

        assertSame(integrityException, propagada);
    }

    @Test
    void registrarIngresosVentaDomicilio_cajaInexistenteOInactiva_rechaza() {
        PedidoPago pago = pagoDomicilio(69L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        when(cajaRepository.findByCodigo(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL)).thenReturn(Optional.empty());

        ResponseStatusException inexistente = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));
        assertEquals(HttpStatus.NOT_FOUND, inexistente.getStatusCode());

        cajaPrincipal.setActiva(false);
        when(cajaRepository.findByCodigo(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL))
                .thenReturn(Optional.of(cajaPrincipal));
        ResponseStatusException inactiva = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(pago), 10L));
        assertEquals(HttpStatus.CONFLICT, inactiva.getStatusCode());
    }

    @Test
    void registrarIngresosVentaDomicilio_idsDuplicadosOPedidosDistintos_rechazaSinGuardar() {
        PedidoPago duplicado = pagoDomicilio(70L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        ResponseStatusException idsDuplicados = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(duplicado, duplicado), 10L));
        assertEquals(HttpStatus.BAD_REQUEST, idsDuplicados.getStatusCode());

        PedidoPago primero = pagoDomicilio(71L, TipoFinancieroMetodoPago.EFECTIVO, "10.00", 81L);
        PedidoPago segundo = pagoDomicilio(72L, TipoFinancieroMetodoPago.DIGITAL, "10.00", 82L);
        configurarRegistroDomicilio(List.of(primero, segundo));
        ResponseStatusException pedidosDistintos = assertThrows(ResponseStatusException.class,
                () -> cajaService.registrarIngresosVentaDomicilio(List.of(primero, segundo), 10L));
        assertEquals(HttpStatus.BAD_REQUEST, pedidosDistintos.getStatusCode());
        verify(movimientoCajaRepository, never()).saveAndFlush(any());
    }
}
