package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Evidencia;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.EvidenciaRepository;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.ConfirmarEntregaMixtaDTO;
import com.gas.sistema_gas.dto.PagoRegistroDTO;

@ExtendWith(MockitoExtension.class)
class PedidoPagosServiceImplementTest {

    // Nota: estas pruebas son unitarias y usan Mockito para simular los repositorios.
    // Los objetos creados en este archivo NO se persisten en la base de datos.

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @Mock
    private PedidoPagoRepository pedidoPagoRepository;

    @Mock
    private EvidenciaRepository evidenciaRepository;

    @Mock
    private DetallePedidoRepository detallePedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private PedidoPagosServiceImplement pedidoPagosService;

    // ---------- Helpers ----------

    private Pedido pedido(String montoTotal) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setEstadoPedido("EN_DOMICILIO");
        pedido.setMontoTotal(new BigDecimal(montoTotal));
        return pedido;
    }

    private MetodoPago metodo(Long id, String nombre) {
        MetodoPago metodo = new MetodoPago();
        metodo.setId(id);
        metodo.setNombre(nombre);
        return metodo;
    }

    private PagoRegistroDTO pagoDto(Long idMetodo, String monto, String numOperacion) {
        return new PagoRegistroDTO(idMetodo, new BigDecimal(monto), numOperacion, BigDecimal.ZERO);
    }

    private static PedidoPago pagoSimulado(String monto) {
        PedidoPago p = new PedidoPago();
        p.setMonto(new BigDecimal(monto));
        return p;
    }

    private void assertMonto(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                "Se esperaba " + esperado + " pero se obtuvo " + actual);
    }

    // ---------- 1. Pago mixto exacto ----------

    @Test
    void pagoMixtoExacto_60Efectivo40Yape_total100_quedaPAGADO() {
        Pedido pedido = pedido("100.00");
        MetodoPago efectivo = metodo(1L, "Efectivo");
        MetodoPago yape = metodo(2L, "Yape");

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(pagoDto(1L, "60.00", "op-efectivo"));
        pagosDto.add(pagoDto(2L, "40.00", "op-yape"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        // 1ª llamada findByPedido: pagos anteriores -> vacío
        // 2ª llamada findByPedido: recálculo -> los montos aplicados recién guardados
        List<PedidoPago> pagosSimulados = new ArrayList<>();
        pagosSimulados.add(pagoSimulado("60.00"));
        pagosSimulados.add(pagoSimulado("40.00"));

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));
        when(pedidoPagoRepository.findByPedido(pedido)).thenReturn(List.of()).thenReturn(pagosSimulados);
        when(pedidoPagoRepository.save(any(PedidoPago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<PedidoPago> guardados = pedidoPagosService.registrarPagosMultiples(dto, null);

        // Montos aplicados = montos recibidos (sin vuelto)
        assertEquals(2, guardados.size());
        assertMonto("60.00", guardados.get(0).getMonto());
        assertMonto("0.00", guardados.get(0).getVuelto());
        assertMonto("40.00", guardados.get(1).getMonto());
        assertMonto("0.00", guardados.get(1).getVuelto());

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("PAGADO", pedido.getEstadoPago());
    }

    // ---------- 2. Pago mixto con vuelto ----------

    @Test
    void pagoMixtoConVuelto_100Efectivo20Yape_total110_vueltoDescontadoDelEfectivo() {
        Pedido pedido = pedido("110.00");
        MetodoPago efectivo = metodo(1L, "Efectivo");
        MetodoPago yape = metodo(2L, "Yape");

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(pagoDto(1L, "100.00", "op-efectivo"));
        pagosDto.add(pagoDto(2L, "20.00", "op-yape"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        // Montos aplicados: efectivo 100 - vuelto 10 = 90; yape 20 -> suma 110
        List<PedidoPago> pagosSimulados = new ArrayList<>();
        pagosSimulados.add(pagoSimulado("90.00"));
        pagosSimulados.add(pagoSimulado("20.00"));

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));
        when(pedidoPagoRepository.findByPedido(pedido)).thenReturn(List.of()).thenReturn(pagosSimulados);
        when(pedidoPagoRepository.save(any(PedidoPago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoPagosService.registrarPagosMultiples(dto, null);

        ArgumentCaptor<PedidoPago> pagoCaptor = ArgumentCaptor.forClass(PedidoPago.class);
        verify(pedidoPagoRepository, times(2)).save(pagoCaptor.capture());
        List<PedidoPago> capturados = pagoCaptor.getAllValues();

        // El vuelto (10) se descuenta únicamente del pago en Efectivo
        PedidoPago pagoEfectivo = capturados.get(0);
        PedidoPago pagoYape = capturados.get(1);
        assertMonto("90.00", pagoEfectivo.getMonto());
        assertMonto("10.00", pagoEfectivo.getVuelto());
        assertMonto("20.00", pagoYape.getMonto());
        assertMonto("0.00", pagoYape.getVuelto());

        // La suma final de PedidoPago.monto debe ser exactamente el montoTotal
        BigDecimal sumaAplicada = capturados.stream()
                .map(PedidoPago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMonto("110.00", sumaAplicada);

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("PAGADO", pedido.getEstadoPago());
    }

    // ---------- 3. Suma menor al total ----------

    @Test
    void sumaMenorAlTotal_lanzaBadRequest_yNuncaEliminaPagosAnteriores() {
        Pedido pedido = pedido("100.00");
        MetodoPago efectivo = metodo(1L, "Efectivo");
        MetodoPago yape = metodo(2L, "Yape");

        // 30 Efectivo + 30 Yape = 60 < 100 (dos métodos diferentes)
        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(pagoDto(1L, "30.00", "op-001"));
        pagosDto.add(pagoDto(2L, "30.00", "op-002"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoPagosService.registrarPagosMultiples(dto, null));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("El total pagado no cubre el monto del pedido", ex.getReason());

        // La validación ocurre ANTES de eliminar los pagos anteriores
        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).flush();
        verify(pedidoPagoRepository, never()).save(any(PedidoPago.class));
        assertNotEquals("ENTREGADO", pedido.getEstadoPedido());
    }

    // ---------- 4. idMetodo nulo ----------

    @Test
    void idMetodoNulo_lanzaBadRequest_yNuncaEliminaPagosAnteriores() {
        Pedido pedido = pedido("100.00");

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(new PagoRegistroDTO(null, new BigDecimal("50.00"), "op-001", BigDecimal.ZERO));
        pagosDto.add(pagoDto(2L, "50.00", "op-002"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoPagosService.registrarPagosMultiples(dto, null));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("Método de pago no válido", ex.getReason());

        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).flush();
        verify(pedidoPagoRepository, never()).save(any(PedidoPago.class));
        assertNotEquals("ENTREGADO", pedido.getEstadoPedido());
    }

    // ---------- 5. Monto cero ----------

    @Test
    void montoCero_lanzaBadRequest() {
        Pedido pedido = pedido("100.00");

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(new PagoRegistroDTO(1L, BigDecimal.ZERO, "op-001", BigDecimal.ZERO));
        pagosDto.add(pagoDto(1L, "100.00", "op-002"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoPagosService.registrarPagosMultiples(dto, null));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("El monto debe ser mayor a cero", ex.getReason());
        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).flush();
        assertNotEquals("ENTREGADO", pedido.getEstadoPedido());
    }

    // ---------- 6. Monto negativo ----------

    @Test
    void montoNegativo_lanzaBadRequest() {
        Pedido pedido = pedido("100.00");

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(new PagoRegistroDTO(1L, new BigDecimal("-10.00"), "op-001", BigDecimal.ZERO));
        pagosDto.add(pagoDto(1L, "110.00", "op-002"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoPagosService.registrarPagosMultiples(dto, null));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("El monto debe ser mayor a cero", ex.getReason());
        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).flush();
        assertNotEquals("ENTREGADO", pedido.getEstadoPedido());
    }

    // ---------- 7. Excedente sin método Efectivo ----------

    @Test
    void excedenteSinMetodoEfectivo_lanzaBadRequest() {
        Pedido pedido = pedido("100.00");
        MetodoPago yape = metodo(2L, "Yape");
        MetodoPago plin = metodo(3L, "Plin");

        // 70 + 50 = 120 > 100 -> vuelto de 20 sin efectivo para devolverlo
        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(pagoDto(2L, "70.00", "op-yape"));
        pagosDto.add(pagoDto(3L, "50.00", "op-plin"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));
        when(metodoPagoRepository.findById(3L)).thenReturn(Optional.of(plin));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoPagosService.registrarPagosMultiples(dto, null));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("No se puede entregar vuelto sin un pago en efectivo", ex.getReason());
        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).flush();
        verify(pedidoPagoRepository, never()).save(any(PedidoPago.class));
        assertNotEquals("ENTREGADO", pedido.getEstadoPedido());
    }

    // ---------- 8. Dos filas con el mismo idMetodo ----------

    @Test
    void dosFilasMismoIdMetodo_lanzaBadRequest_yNuncaEliminaPagosAnteriores() {
        Pedido pedido = pedido("100.00");
        MetodoPago yape = metodo(2L, "Yape");

        // 60 Yape + 40 Yape = 100, pero ambos usan el mismo método
        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(pagoDto(2L, "60.00", "op-001"));
        pagosDto.add(pagoDto(2L, "40.00", "op-002"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoPagosService.registrarPagosMultiples(dto, null));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("El pago mixto requiere al menos dos métodos de pago diferentes", ex.getReason());

        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).flush();
        verify(pedidoPagoRepository, never()).save(any(PedidoPago.class));
        assertNotEquals("ENTREGADO", pedido.getEstadoPedido());
    }

    // ---------- 9. Efectivo aplicado igual a cero ----------

    @Test
    void efectivoAplicadoCero_lanzaBadRequest_yNuncaEliminaPagosAnteriores() {
        Pedido pedido = pedido("100.00");
        MetodoPago efectivo = metodo(1L, "Efectivo");
        MetodoPago yape = metodo(2L, "Yape");

        // 50 Efectivo + 100 Yape = 150 -> vuelto 50 -> efectivo aplicado 0
        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(pagoDto(1L, "50.00", "op-efectivo"));
        pagosDto.add(pagoDto(2L, "100.00", "op-yape"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedidoPagosService.registrarPagosMultiples(dto, null));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("Cada método debe aplicar un monto mayor a cero al pedido", ex.getReason());

        verify(pedidoPagoRepository, never()).deleteAll(any());
        verify(pedidoPagoRepository, never()).flush();
        verify(pedidoPagoRepository, never()).save(any(PedidoPago.class));
        assertNotEquals("ENTREGADO", pedido.getEstadoPedido());
    }

    // ---------- 10. Evidencia de vuelto asociada al pago en Efectivo ----------

    @Test
    void evidenciaVuelto_seAsociaAlPagoEnEfectivo_yNoAlUltimoPago() {
        // Evitar escribir archivos en el directorio real del proyecto durante el test
        ReflectionTestUtils.setField(pedidoPagosService, "evidenciasDir", System.getProperty("java.io.tmpdir"));

        Pedido pedido = pedido("110.00");
        MetodoPago efectivo = metodo(1L, "Efectivo");
        MetodoPago yape = metodo(2L, "Yape");

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(pagoDto(1L, "100.00", "op-efectivo"));
        pagosDto.add(pagoDto(2L, "20.00", "op-yape"));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        MockMultipartFile evidenciaVuelto = new MockMultipartFile(
                "evidenciaVuelto", "vuelto.png", "image/png", new byte[] { 'v', 'u', 'e', 'l', 't', 'o' });

        List<PedidoPago> pagosSimulados = new ArrayList<>();
        pagosSimulados.add(pagoSimulado("90.00"));
        pagosSimulados.add(pagoSimulado("20.00"));

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));
        when(pedidoPagoRepository.findByPedido(pedido)).thenReturn(List.of()).thenReturn(pagosSimulados);
        when(pedidoPagoRepository.save(any(PedidoPago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoPagosService.registrarPagosMultiples(dto, null, evidenciaVuelto);

        // El pago en Efectivo es el primero en guardarse (100 - 10 de vuelto = 90)
        ArgumentCaptor<PedidoPago> pagoCaptor = ArgumentCaptor.forClass(PedidoPago.class);
        verify(pedidoPagoRepository, times(2)).save(pagoCaptor.capture());
        PedidoPago pagoEfectivo = pagoCaptor.getAllValues().get(0);
        assertEquals("Efectivo", pagoEfectivo.getMetodoPago().getNombre());
        assertMonto("10.00", pagoEfectivo.getVuelto());

        // La evidencia VUELTO debe quedar asociada al pago en Efectivo
        ArgumentCaptor<Evidencia> evidenciaCaptor = ArgumentCaptor.forClass(Evidencia.class);
        verify(evidenciaRepository, times(1)).save(evidenciaCaptor.capture());
        Evidencia evidenciaVueltoGuardada = evidenciaCaptor.getValue();

        assertEquals("VUELTO", evidenciaVueltoGuardada.getTipoEvidencia());
        assertEquals(pagoEfectivo, evidenciaVueltoGuardada.getPedidoPago());
    }

    @Test
    void confirmarEntregaMixta_conCanjesPendientesAgrupaProductoYConsumeDetalles() {
        Pedido pedido = pedido("30.00");
        pedido.setTipoVenta("DOMICILIO");
        MetodoPago efectivo = metodo(1L, "Efectivo");
        MetodoPago yape = metodo(2L, "Yape");
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, List.of(
                pagoDto(1L, "10.00", "op-efectivo"),
                pagoDto(2L, "20.00", "op-yape")));

        Producto producto = new Producto();
        producto.setId(10L);
        producto.setStockVacios(4);
        DetallePedido detalleUno = new DetallePedido();
        detalleUno.setPedido(pedido);
        detalleUno.setProducto(producto);
        detalleUno.setCantidadCanje(1);
        DetallePedido detalleDos = new DetallePedido();
        detalleDos.setPedido(pedido);
        detalleDos.setProducto(producto);
        detalleDos.setCantidadCanje(2);

        List<PedidoPago> pagosSimulados = List.of(pagoSimulado("10.00"), pagoSimulado("20.00"));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(yape));
        when(pedidoPagoRepository.findByPedido(pedido)).thenReturn(List.of()).thenReturn(pagosSimulados);
        when(pedidoPagoRepository.save(any(PedidoPago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(detallePedidoRepository.findByPedido_Id(1L)).thenReturn(List.of(detalleUno, detalleDos));
        when(productoRepository.findAllByIdInForUpdate(List.of(10L))).thenReturn(List.of(producto));

        List<PedidoPago> resultado = pedidoPagosService.confirmarEntregaConPagos(dto, null, null);

        assertEquals(2, resultado.size());
        assertEquals(7, producto.getStockVacios());
        assertEquals(0, detalleUno.getCantidadCanje());
        assertEquals(0, detalleDos.getCantidadCanje());
        verify(productoRepository).findAllByIdInForUpdate(List.of(10L));
        verify(productoRepository).save(producto);
        verify(detallePedidoRepository, times(2)).save(any(DetallePedido.class));
    }

    @Test
    void confirmarEntregaMixta_dosVecesPedidoEntregado_noDuplicaPagosNiCanje() {
        Pedido pedido = pedido("30.00");
        pedido.setEstadoPedido("ENTREGADO");
        PedidoPago pagoExistente = pagoSimulado("30.00");
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, List.of());

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(pedidoPagoRepository.findByPedido(pedido)).thenReturn(List.of(pagoExistente));

        List<PedidoPago> primera = pedidoPagosService.confirmarEntregaConPagos(dto, null, null);
        List<PedidoPago> segunda = pedidoPagosService.confirmarEntregaConPagos(dto, null, null);

        assertSame(pagoExistente, primera.get(0));
        assertSame(pagoExistente, segunda.get(0));
        verify(pedidoPagoRepository, never()).save(any(PedidoPago.class));
        verify(detallePedidoRepository, never()).findByPedido_Id(any());
        verify(productoRepository, never()).findAllByIdInForUpdate(any());
    }
}
