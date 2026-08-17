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

import com.gas.sistema_gas.service.MotoService;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class MotoControllerTest {

    @Mock
    private MotoService motoService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private MotoController controller;

    @Test
    void motos_cargaModeloCompletoYLayout() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.motos(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/moto", model.getAttribute("contenido"));
        assertEquals(List.of(), model.getAttribute("motos"));
        assertEquals(List.of(), model.getAttribute("menu"));
    }

    @Test
    void fragmentoMotos_cargaMismosDatosSinContenido() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoMotos(model, session);

        assertEquals("views/moto :: content", vista);
        assertEquals(List.of(), model.getAttribute("motos"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoMotos_usaPerfilDeSesionParaMenu() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(7L);
        when(motoService.listMoto()).thenReturn(List.of());
        when(opcionService.listByPerfilId(7L)).thenReturn(List.of());

        controller.fragmentoMotos(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaMotos_devuelveFragmentoDeTabla() {
        when(motoService.listMoto()).thenReturn(List.of());

        String vista = controller.tablaMotos(new ExtendedModelMap());

        assertEquals("views/moto :: tablaMotos", vista);
        verify(motoService).listMoto();
    }

    private void configurarModelo() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(2L);
        when(motoService.listMoto()).thenReturn(List.of());
        when(opcionService.listByPerfilId(2L)).thenReturn(List.of());
    }
}
