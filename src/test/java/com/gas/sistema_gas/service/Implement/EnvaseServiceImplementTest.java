package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.ControlEnvase;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.dto.EnvioEnvaseDTO;

@ExtendWith(MockitoExtension.class)
class EnvaseServiceImplementTest {

    @Mock
    private ControlEnvaseRepository controlEnvaseRepository;

    @InjectMocks
    private EnvaseServiceImplement envaseService;

    @Test
    void listarDeudoresPendientes_calculaVencimientoSegunTipoFechaYEstado() {
        LocalDate hoy = LocalDate.now();
        when(controlEnvaseRepository.findDeudoresPendientes()).thenReturn(List.of(
            crearControl(1L, "NORMAL", "PRESTADO", hoy.plusDays(1)),
            crearControl(2L, "NORMAL", "PRESTADO", hoy),
            crearControl(3L, "NORMAL", "PRESTADO", hoy.minusDays(1)),
            crearControl(4L, "NORMAL", "PARCIAL", hoy.minusDays(1)),
            crearControl(5L, "ESPECIAL", "PRESTADO", null),
            crearControl(6L, "LEGADO", "PRESTADO", null)
        ));

        List<EnvioEnvaseDTO.DeudorResponse> respuesta = envaseService.listarDeudoresPendientes();

        assertEquals(6, respuesta.size());
        assertFalse(respuesta.get(0).vencido());
        assertFalse(respuesta.get(1).vencido());
        assertTrue(respuesta.get(2).vencido());
        assertTrue(respuesta.get(3).vencido());
        assertFalse(respuesta.get(4).vencido());
        assertFalse(respuesta.get(5).vencido());
    }

    @Test
    void listarDeudoresPendientes_noIncluyeSaldadosCuandoLaConsultaLosExcluye() {
        when(controlEnvaseRepository.findDeudoresPendientes()).thenReturn(List.of());

        List<EnvioEnvaseDTO.DeudorResponse> respuesta = envaseService.listarDeudoresPendientes();

        assertTrue(respuesta.isEmpty());
    }

    private ControlEnvase crearControl(Long id, String tipoPrestamo, String estado, LocalDate fechaLimite) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNombre("Cliente " + id);
        cliente.setTelefono("99999999" + id);
        cliente.setDireccion("Dirección " + id);

        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Envase " + id);

        ControlEnvase control = new ControlEnvase();
        control.setId(id);
        control.setCliente(cliente);
        control.setProducto(producto);
        control.setCantidadPrestada(2);
        control.setCantidadDevuelta("PARCIAL".equals(estado) ? 1 : 0);
        control.setFechaPrestamo(LocalDateTime.now().minusDays(1));
        control.setFechaLimiteDevolucion(fechaLimite);
        control.setTipoPrestamo(tipoPrestamo);
        control.setEstado(estado);
        return control;
    }
}
