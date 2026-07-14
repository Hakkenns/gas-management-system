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
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.dto.PedidoDTO;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplementTest {

    // Nota: estas pruebas son unitarias y usan Mockito para simular el repositorio y el mapper.
    // Los objetos creados en este archivo NO se persisten en la base de datos.

    @Mock
    private PedidoRepository pedidoRepository;

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
            null
        );

        when(pedidoRepository.findByTipoVentaAndEmpleadoIdWithMetodoPago("DOMICILIO", 10L)).thenReturn(List.of(pedido));
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
            null
        );

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
}
