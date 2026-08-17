package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.ProveedorService;
import com.gas.sistema_gas.service.RubroService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    @Mock
    private ProveedorService proveedorService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private RubroService rubroService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private ProveedorController controller;

    @Test
    void proveedores_cargaModeloCompletoYLayout() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.proveedores(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/proveedores", model.getAttribute("contenido"));
        assertEquals(List.of(), model.getAttribute("proveedores"));
        assertEquals(List.of(), model.getAttribute("rubros"));
        assertEquals(List.of(), model.getAttribute("menu"));
    }

    @Test
    void fragmentoProveedores_cargaDatosNecesariosSinContenido() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoProveedores(model, session);

        assertEquals("views/proveedores :: content", vista);
        assertEquals(List.of(), model.getAttribute("proveedores"));
        assertEquals(List.of(), model.getAttribute("rubros"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoProveedores_usaPerfilDeSesionParaMenu() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(7L);
        when(proveedorService.listAll()).thenReturn(List.of());
        when(rubroService.listAll()).thenReturn(List.of());
        when(opcionService.listByPerfilId(7L)).thenReturn(List.of());

        controller.fragmentoProveedores(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaProveedores_devuelveFragmentoDeTabla() {
        when(proveedorService.listAll()).thenReturn(List.of());

        String vista = controller.tablaProveedores(new ExtendedModelMap());

        assertEquals("views/proveedores :: tablaProveedores", vista);
        verify(proveedorService).listAll();
    }

    private void configurarModelo() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(2L);
        when(proveedorService.listAll()).thenReturn(List.of());
        when(rubroService.listAll()).thenReturn(List.of());
        when(opcionService.listByPerfilId(2L)).thenReturn(List.of());
    }
}
