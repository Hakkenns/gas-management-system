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

import com.gas.sistema_gas.service.EnvaseService;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class EnvaseControllerTest {

    @Mock private EnvaseService envaseService;
    @Mock private OpcionService opcionService;
    @Mock private HttpSession session;

    @InjectMocks private EnvaseController controller;

    @Test
    void vistaEnvases_cargaCatalogoCompletoYLayout() {
        configurarMenu(2L);
        Model model = new ExtendedModelMap();

        String vista = controller.vistaEnvases(model, session);

        assertEquals("components/layout", vista);
        assertEquals(List.of(), model.getAttribute("menu"));
        assertEquals("views/envases-maestro", model.getAttribute("contenido"));
        verify(opcionService).listByPerfilId(2L);
    }

    @Test
    void fragmentoVistaEnvases_cargaMenuSinContenido() {
        configurarMenu(3L);
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoVistaEnvases(model, session);

        assertEquals("views/envases-maestro :: content", vista);
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoVistaEnvases_usaPerfilDeSesionParaMenu() {
        configurarMenu(7L);

        controller.fragmentoVistaEnvases(new ExtendedModelMap(), session);

        verify(session).getAttribute("usuarioPerfilId");
        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void aliasesMaestroYCatalogo_conservanVistaCompleta() {
        configurarMenu(4L);
        Model maestroModel = new ExtendedModelMap();
        Model catalogoModel = new ExtendedModelMap();

        assertEquals("components/layout", controller.maestroEnvases(maestroModel, session));
        assertEquals("components/layout", controller.catalogoEnvases(catalogoModel, session));
        assertEquals("views/envases-maestro", maestroModel.getAttribute("contenido"));
        assertEquals("views/envases-maestro", catalogoModel.getAttribute("contenido"));
        verify(opcionService, org.mockito.Mockito.times(2)).listByPerfilId(4L);
    }

    private void configurarMenu(Long perfilId) {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(perfilId);
        when(opcionService.listByPerfilId(perfilId)).thenReturn(List.of());
    }
}
