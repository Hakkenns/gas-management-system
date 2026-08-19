package com.gas.sistema_gas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.dto.CategoriaDTO;
import com.gas.sistema_gas.service.CategoriaService;
import com.gas.sistema_gas.service.CompraService;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.ProductoService;
import com.gas.sistema_gas.service.ProveedorService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class CompraControllerTest {

    @Mock private CompraService compraService;
    @Mock private OpcionService opcionService;
    @Mock private ProveedorService proveedorService;
    @Mock private ProductoService productoService;
    @Mock private CategoriaService categoriaService;
    @Mock private InventarioLoteRepository inventarioLoteRepository;
    @Mock private HttpSession session;

    @InjectMocks private CompraController controller;

    @Test
    void compras_cargaVistaCompletaYModelo() {
        configurarModelo(2L);
        Model model = new ExtendedModelMap();

        assertEquals("components/layout", controller.compras(model, session));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertEquals(List.of(), model.getAttribute("compras"));
        assertEquals(List.of(), model.getAttribute("proveedores"));
        assertEquals(List.of(), model.getAttribute("productos"));
        assertEquals(1, ((List<?>) model.getAttribute("categorias")).size());
        assertEquals("views/compras", model.getAttribute("contenido"));

        verifyModelo(2L);
    }

    @Test
    void fragmentoCompras_cargaModeloSinContenido() {
        configurarModelo(3L);
        Model model = new ExtendedModelMap();

        assertEquals("views/compras :: content", controller.fragmentoCompras(model, session));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertEquals(List.of(), model.getAttribute("compras"));
        assertEquals(List.of(), model.getAttribute("proveedores"));
        assertEquals(List.of(), model.getAttribute("productos"));
        assertEquals(1, ((List<?>) model.getAttribute("categorias")).size());
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoCompras_usaPerfilDeSesion() {
        configurarModelo(7L);

        controller.fragmentoCompras(new ExtendedModelMap(), session);

        verify(session).getAttribute("usuarioPerfilId");
        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaCompras_cargaSoloCompras() {
        when(compraService.listAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        assertEquals("views/compras :: tablaCompras", controller.tablaCompras(model));
        assertEquals(List.of(), model.getAttribute("compras"));
        assertNull(model.getAttribute("menu"));
        assertNull(model.getAttribute("proveedores"));
        assertNull(model.getAttribute("productos"));
        assertNull(model.getAttribute("categorias"));
        assertNull(model.getAttribute("contenido"));

        verify(compraService).listAll();
        verifyNoInteractions(opcionService, proveedorService, productoService,
                categoriaService, inventarioLoteRepository, session);
    }

    private void configurarModelo(Long perfilId) {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(perfilId);
        when(opcionService.listByPerfilId(perfilId)).thenReturn(List.of());
        when(compraService.listAll()).thenReturn(List.of());
        when(proveedorService.listAll()).thenReturn(List.of());
        when(productoService.listAll()).thenReturn(List.of());
        when(categoriaService.listAll()).thenReturn(List.of(
                categoria(1L, 1), categoria(2L, 0), categoria(3L, null)));
    }

    private CategoriaDTO.SimpleResponse categoria(Long id, Integer estado) {
        return new CategoriaDTO.SimpleResponse(id, "Categoria " + id, null, estado,
                "UND", false, "Capacidad", false, List.of());
    }

    private void verifyModelo(Long perfilId) {
        verify(session).getAttribute("usuarioPerfilId");
        verify(opcionService).listByPerfilId(perfilId);
        verify(compraService).listAll();
        verify(proveedorService).listAll();
        verify(productoService).listAll();
        verify(categoriaService).listAll();
    }
}
