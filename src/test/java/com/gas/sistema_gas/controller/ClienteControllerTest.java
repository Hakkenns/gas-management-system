package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.gas.sistema_gas.dto.ClienteDTO;
import com.gas.sistema_gas.dto.OpcionDTO;
import com.gas.sistema_gas.service.ClienteService;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private ClienteController controller;

    @Test
    void clientes_conservaLayoutCompleto() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.clientes(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/cliente", model.getAttribute("contenido"));
        assertEquals(clientes(), model.getAttribute("clientes"));
        assertEquals(menu(), model.getAttribute("menu"));
    }

    @Test
    void clientesFragment_devuelveSoloFragmentoYDatosNecesarios() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoClientes(model, session);

        assertEquals("views/cliente :: content", vista);
        assertEquals(clientes(), model.getAttribute("clientes"));
        assertEquals(menu(), model.getAttribute("menu"));
        assertEquals(null, model.getAttribute("contenido"));
    }

    @Test
    void clientesFragment_usaElPerfilDeLaSesionParaElMenu() {
        List<ClienteDTO.SimpleResponse> clientes = clientes();
        List<OpcionDTO.SimpleResponse> menu = menu();
        when(session.getAttribute("usuarioPerfilId")).thenReturn(7L);
        when(clienteService.listAll()).thenReturn(clientes);
        when(opcionService.listByPerfilId(7L)).thenReturn(menu);

        controller.fragmentoClientes(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    private void configurarModelo() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(2L);
        when(clienteService.listAll()).thenReturn(clientes());
        when(opcionService.listByPerfilId(2L)).thenReturn(menu());
    }

    private List<ClienteDTO.SimpleResponse> clientes() {
        return List.of(new ClienteDTO.SimpleResponse(
                1L, "Cliente", "12345678", "999999999", "Direccion", "Referencia",
                "cliente@example.com", 1, false));
    }

    private List<OpcionDTO.SimpleResponse> menu() {
        return List.of(new OpcionDTO.SimpleResponse(
                13L, "Clientes", "fas fa-user-friends", "clientes", 1, List.of()));
    }
}
