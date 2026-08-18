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

import com.gas.sistema_gas.service.CategoriaService;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock private CategoriaService categoriaService;
    @Mock private OpcionService opcionService;
    @Mock private HttpSession session;

    @InjectMocks private CategoriaController controller;

    @Test
    void categorias_cargaModeloCompletoYLayout() {
        configurarModelo(2L);
        Model model = new ExtendedModelMap();

        String vista = controller.categorias(model, session);

        assertEquals("components/layout", vista);
        assertEquals(List.of(), model.getAttribute("categorias"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertEquals("views/categoria", model.getAttribute("contenido"));
        verify(categoriaService).listAll();
        verify(opcionService).listByPerfilId(2L);
    }

    @Test
    void fragmentoCategorias_cargaDatosNecesariosSinContenido() {
        configurarModelo(3L);
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoCategorias(model, session);

        assertEquals("views/categoria :: content", vista);
        assertEquals(List.of(), model.getAttribute("categorias"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoCategorias_usaPerfilDeSesionParaMenu() {
        configurarModelo(7L);

        controller.fragmentoCategorias(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaCategorias_devuelveFragmentoDeTabla() {
        when(categoriaService.listAll()).thenReturn(List.of());

        String vista = controller.tablaCategorias(new ExtendedModelMap());

        assertEquals("views/categoria :: tablaCategorias", vista);
        verify(categoriaService).listAll();
    }

    private void configurarModelo(Long perfilId) {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(perfilId);
        when(categoriaService.listAll()).thenReturn(List.of());
        when(opcionService.listByPerfilId(perfilId)).thenReturn(List.of());
    }
}
