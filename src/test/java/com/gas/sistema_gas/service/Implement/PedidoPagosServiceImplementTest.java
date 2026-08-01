package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Repository.EvidenciaRepository;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
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

    @InjectMocks
    private PedidoPagosServiceImplement pedidoPagosService;

    @Test
    void deberiaQuedarCreditoAlEntregarConPagosParciales() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setEstadoPedido("EN_DOMICILIO");
        pedido.setEstadoPago("CREDITO");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        MetodoPago efectivo = new MetodoPago();
        efectivo.setId(1L);
        efectivo.setNombre("Efectivo");

        // Pagos que el servicio persistirá (S/. 30 + S/. 30 = S/. 60 de S/. 100)
        List<PedidoPago> pagosParciales = new ArrayList<>();
        PedidoPago pago1 = new PedidoPago();
        pago1.setMonto(new BigDecimal("30.00"));
        PedidoPago pago2 = new PedidoPago();
        pago2.setMonto(new BigDecimal("30.00"));
        pagosParciales.add(pago1);
        pagosParciales.add(pago2);

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(new PagoRegistroDTO(1L, new BigDecimal("30.00"), "op-001", BigDecimal.ZERO));
        pagosDto.add(new PagoRegistroDTO(1L, new BigDecimal("30.00"), "op-002", BigDecimal.ZERO));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(1L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        // 1ª llamada: pagos anteriores a eliminar -> vacío
        // 2ª llamada: recálculo del estado de pago -> los pagos recién persistidos
        when(pedidoPagoRepository.findByPedido(pedido))
                .thenReturn(List.of())
                .thenReturn(pagosParciales);
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(pedidoPagoRepository.save(any(PedidoPago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoPagosService.registrarPagosMultiples(dto, null);

        // El estado del pedido pasa a ENTREGADO...
        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        // ...pero con pagos parciales (S/. 60 de S/. 100) NO puede quedar como PAGADO
        assertEquals("CREDITO", pedido.getEstadoPago());
    }

    @Test
    void deberiaQuedarPagadoAlEntregarConPagosQueCubrenElTotal() {
        Pedido pedido = new Pedido();
        pedido.setId(2L);
        pedido.setEstadoPedido("EN_DOMICILIO");
        pedido.setEstadoPago("CREDITO");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        MetodoPago efectivo = new MetodoPago();
        efectivo.setId(1L);
        efectivo.setNombre("Efectivo");

        // Pagos que el servicio persistirá (S/. 60 + S/. 40 = S/. 100 de S/. 100)
        List<PedidoPago> pagosCompletos = new ArrayList<>();
        PedidoPago pago1 = new PedidoPago();
        pago1.setMonto(new BigDecimal("60.00"));
        PedidoPago pago2 = new PedidoPago();
        pago2.setMonto(new BigDecimal("40.00"));
        pagosCompletos.add(pago1);
        pagosCompletos.add(pago2);

        List<PagoRegistroDTO> pagosDto = new ArrayList<>();
        pagosDto.add(new PagoRegistroDTO(1L, new BigDecimal("60.00"), "op-001", BigDecimal.ZERO));
        pagosDto.add(new PagoRegistroDTO(1L, new BigDecimal("40.00"), "op-002", BigDecimal.ZERO));
        ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(2L, pagosDto);

        when(pedidoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(pedido));
        // 1ª llamada: pagos anteriores a eliminar -> vacío
        // 2ª llamada: recálculo del estado de pago -> los pagos recién persistidos
        when(pedidoPagoRepository.findByPedido(pedido))
                .thenReturn(List.of())
                .thenReturn(pagosCompletos);
        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(efectivo));
        when(pedidoPagoRepository.save(any(PedidoPago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoPagosService.registrarPagosMultiples(dto, null);

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("PAGADO", pedido.getEstadoPago());
    }
}