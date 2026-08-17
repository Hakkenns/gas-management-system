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
import com.gas.sistema_gas.service.RubroService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RubroControllerTest {

    @Mock
    private RubroService rubroService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private RubroController controller;

    @Test
    void rubros_cargaModeloCompletoYLayout() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.rubros(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/rubros", model.getAttribute("contenido"));
        assertEquals(List.of(), model.getAttribute("rubros"));
        assertEquals(List.of(), model.getAttribute("menu"));
    }

    @Test
    void fragmentoRubros_cargaDatosNecesariosSinContenido() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoRubros(model, session);

        assertEquals("views/rubros :: content", vista);
        assertEquals(List.of(), model.getAttribute("rubros"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoRubros_usaPerfilDeSesionParaMenu() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(7L);
        when(rubroService.listAll()).thenReturn(List.of());
        when(opcionService.listByPerfilId(7L)).thenReturn(List.of());

        controller.fragmentoRubros(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaRubros_devuelveFragmentoDeTabla() {
        when(rubroService.listAll()).thenReturn(List.of());

        String vista = controller.tablaRubros(new ExtendedModelMap());

        assertEquals("views/rubros :: tablaRubros", vista);
        verify(rubroService).listAll();
    }

    private void configurarModelo() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(2L);
        when(rubroService.listAll()).thenReturn(List.of());
        when(opcionService.listByPerfilId(2L)).thenReturn(List.of());
    }
}
