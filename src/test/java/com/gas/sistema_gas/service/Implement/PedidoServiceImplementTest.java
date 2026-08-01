package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.PedidoMapper;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Repository.EvidenciaRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.dto.PedidoDTO;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplementTest {

    // Nota: estas pruebas son unitarias y usan Mockito para simular el repositorio y el mapper.
    // Los objetos creados en este archivo NO se persisten en la base de datos.

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PedidoPagoRepository pedidoPagoRepository;

    @Mock
    private EvidenciaRepository evidenciaRepository;

    @Mock
    private PedidoMapper pedidoMapper;

    @InjectMocks
    private PedidoServiceImplement pedidoService;

    @Test
    void shouldExposeClientAddressAndDateInSimpleResponse() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCodigo("NV001");
        pedido.setFechaSolicitud(LocalDateTime.of(2024, 1, 2, 3, 4));

        Cliente cliente = new Cliente();
        cliente.setNombre("Juanito");
        cliente.setDireccion("Av. Siempre Viva 123");
        pedido.setCliente(cliente);

                PedidoDTO.SimpleResponse mappedResponse = new PedidoDTO.SimpleResponse(
                    1L,
                    "NV001",
                    pedido.getFechaSolicitud(),
                    "Juanito",
                    "Av. Siempre Viva 123",
                    null,
                    "Entregar en la puerta principal",
                    null,
                    "PENDIENTE",
                    "PENDIENTE",
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    null,
                    "DOMICILIO",
                    null,
                    List.of()
                );

        when(pedidoRepository.findByTipoVentaAndEmpleadoIdWithMetodoPago("DOMICILIO", 10L)).thenReturn(List.of(pedido));
        when(pedidoPagoRepository.findByPedido_Id(1L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(mappedResponse);

        List<PedidoDTO.SimpleResponse> result = pedidoService.listByTipoVentaAndEmpleadoId("DOMICILIO", 10L);

        assertEquals("Juanito", result.get(0).nombreCliente());
        assertEquals("Av. Siempre Viva 123", result.get(0).direccionCliente());
        assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4), result.get(0).fechaSolicitud());
    }

    @Test
    void shouldUpdatePedidoStateAndPersistIt() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setEstadoPedido("PENDIENTE");

        PedidoDTO.SimpleResponse mappedResponse = new PedidoDTO.SimpleResponse(
            1L,
            "NV001",
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            "ACEPTADO",
            "PENDIENTE",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            "DOMICILIO",
            null,
            List.of()
        );

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(1L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(mappedResponse);

        PedidoDTO.SimpleResponse result = pedidoService.updateEstadoPedido(1L, "ACEPTADO");

        assertEquals("ACEPTADO", result.estadoPedido());
        assertEquals("ACEPTADO", pedido.getEstadoPedido());
    }

    @Test
    void shouldRejectBackwardsStateTransitions() {
        Pedido pedido = new Pedido();
        pedido.setId(2L);
        pedido.setEstadoPedido("EN_CAMINO");

        when(pedidoRepository.findById(2L)).thenReturn(Optional.of(pedido));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> pedidoService.updateEstadoPedido(2L, "ACEPTADO"));

        assertEquals("El estado del pedido no puede retroceder", exception.getReason());
    }

    @Test
    void deberiaMantenerPendienteAlEntregarPedidoSinPagos() {
        Pedido pedido = new Pedido();
        pedido.setId(3L);
        pedido.setEstadoPedido("EN_DOMICILIO");
        pedido.setEstadoPago("PENDIENTE");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        PedidoDTO.SimpleResponse mappedResponse = new PedidoDTO.SimpleResponse(
            3L,
            "NV003",
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            "ENTREGADO",
            "PENDIENTE",
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            null,
            "DOMICILIO",
            null,
            List.of()
        );

        when(pedidoRepository.findById(3L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(3L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(mappedResponse);

        pedidoService.updateEstadoPedido(3L, "ENTREGADO");

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("PENDIENTE", pedido.getEstadoPago());
    }

    @Test
    void deberiaMantenerCreditoAlEntregarPedidoConPagoParcial() {
        Pedido pedido = new Pedido();
        pedido.setId(4L);
        pedido.setEstadoPedido("EN_DOMICILIO");
        pedido.setEstadoPago("CREDITO");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        PedidoPago pagoParcial = new PedidoPago();
        pagoParcial.setId(41L);
        pagoParcial.setMonto(new BigDecimal("50.00"));

        PedidoDTO.SimpleResponse mappedResponse = new PedidoDTO.SimpleResponse(
            4L,
            "NV004",
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            "ENTREGADO",
            "CREDITO",
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            null,
            "DOMICILIO",
            null,
            List.of()
        );

        when(pedidoRepository.findById(4L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(4L)).thenReturn(List.of(pagoParcial));
        when(evidenciaRepository.findByPedidoPago_Id(41L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(mappedResponse);

        pedidoService.updateEstadoPedido(4L, "ENTREGADO");

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("CREDITO", pedido.getEstadoPago());
    }

    @Test
    void deberiaMarcarPagadoSoloCuandoLosPagosCubrenElTotal() {
        Pedido pedido = new Pedido();
        pedido.setId(5L);
        pedido.setEstadoPedido("EN_DOMICILIO");
        pedido.setEstadoPago("CREDITO");
        pedido.setMontoTotal(new BigDecimal("100.00"));

        PedidoPago pagoCompleto = new PedidoPago();
        pagoCompleto.setId(51L);
        pagoCompleto.setMonto(new BigDecimal("100.00"));

        PedidoDTO.SimpleResponse mappedResponse = new PedidoDTO.SimpleResponse(
            5L,
            "NV005",
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            "ENTREGADO",
            "PAGADO",
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            null,
            "DOMICILIO",
            null,
            List.of()
        );

        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoPagoRepository.findByPedido_Id(5L)).thenReturn(List.of(pagoCompleto));
        when(evidenciaRepository.findByPedidoPago_Id(51L)).thenReturn(List.of());
        when(pedidoMapper.toSimpleResponse(pedido)).thenReturn(mappedResponse);

        pedidoService.updateEstadoPedido(5L, "ENTREGADO");

        assertEquals("ENTREGADO", pedido.getEstadoPedido());
        assertEquals("PAGADO", pedido.getEstadoPago());
    }

    @Test
    void mapperDeberiaIgnorarEstadoPagoEnviadoPorElFrontend() {
        // Regla 4: el backend no debe confiar en el estadoPago que envía el frontend
        Pedido pedido = new Pedido();
        pedido.setEstadoPago("PENDIENTE");
        pedido.setEstadoPedido("PENDIENTE");

        PedidoMapper realMapper = org.mapstruct.factory.Mappers.getMapper(PedidoMapper.class);
        PedidoDTO.Update updateDto = new PedidoDTO.Update(null, "ENTREGADO", "PAGADO", "OP123", "mi observacion");

        realMapper.updateEntityFromDto(updateDto, pedido);

        // El estadoPago "PAGADO" del frontend NO debe sobrescribir el estado real
        assertEquals("PENDIENTE", pedido.getEstadoPago());
        assertEquals("mi observacion", pedido.getObservaciones());
    }
}
