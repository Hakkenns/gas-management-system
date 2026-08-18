package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.service.CategoriaService;
import com.gas.sistema_gas.service.EnvaseService;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.ProductoService;
import com.gas.sistema_gas.service.ProveedorService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock private ProductoService productoService;
    @Mock private OpcionService opcionService;
    @Mock private CategoriaService categoriaService;
    @Mock private ProveedorService proveedorService;
    @Mock private Validator validator;
    @Mock private EnvaseService envaseService;
    @Mock private ProductoRepository productoRepository;
    @Mock private HttpSession session;

    @InjectMocks private ProductoController controller;

    @Test
    void productos_cargaVistaCompletaYModelo() {
        configurarModelo(2L);
        Model model = new ExtendedModelMap();

        assertEquals("components/layout", controller.productos(model, session));
        assertModeloCompleto(model);
        assertEquals("views/productos", model.getAttribute("contenido"));
        verificarModelo(2L);
    }

    @Test
    void fragmentoProductos_cargaModeloSinContenido() {
        configurarModelo(3L);
        Model model = new ExtendedModelMap();

        assertEquals("views/productos :: content", controller.fragmentoProductos(model, session));
        assertModeloCompleto(model);
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoProductos_usaPerfilDeSesion() {
        configurarModelo(7L);

        controller.fragmentoProductos(new ExtendedModelMap(), session);

        verify(session).getAttribute("usuarioPerfilId");
        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaProductos_cargaSoloProductos() {
        when(productoService.listAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        assertEquals("views/productos :: tablaProductos", controller.tablaProductos(model));
        assertNotNull(model.getAttribute("productos"));
        assertNull(model.getAttribute("menu"));
        assertNull(model.getAttribute("categorias"));
        assertNull(model.getAttribute("envases"));
        assertNull(model.getAttribute("proveedores"));
        assertNull(model.getAttribute("contenido"));
        verify(productoService).listAll();
        verifyNoInteractions(opcionService, categoriaService, envaseService, proveedorService, session);
    }

    private void configurarModelo(Long perfilId) {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(perfilId);
        when(opcionService.listByPerfilId(perfilId)).thenReturn(List.of());
        when(productoService.listAll()).thenReturn(List.of());
        when(categoriaService.listAll()).thenReturn(List.of());
        when(envaseService.listarTodos()).thenReturn(List.of());
        when(proveedorService.listAll()).thenReturn(List.of());
    }

    private void assertModeloCompleto(Model model) {
        assertNotNull(model.getAttribute("menu"));
        assertNotNull(model.getAttribute("productos"));
        assertNotNull(model.getAttribute("categorias"));
        assertNotNull(model.getAttribute("envases"));
        assertNotNull(model.getAttribute("proveedores"));
    }

    private void verificarModelo(Long perfilId) {
        verify(session).getAttribute("usuarioPerfilId");
        verify(opcionService).listByPerfilId(perfilId);
        verify(productoService).listAll();
        verify(categoriaService).listAll();
        verify(envaseService).listarTodos();
        verify(proveedorService).listAll();
    }
}
