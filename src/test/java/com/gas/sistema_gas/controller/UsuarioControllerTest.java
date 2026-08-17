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

import com.gas.sistema_gas.service.EmpleadoService;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PerfilService;
import com.gas.sistema_gas.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private PerfilService perfilService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private EmpleadoService empleadoService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private UsuarioController controller;

    @Test
    void usuarios_cargaModeloCompletoYLayout() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.usuarios(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/usuario", model.getAttribute("contenido"));
        assertEquals(List.of(), model.getAttribute("usuarios"));
        assertEquals(List.of(), model.getAttribute("empleadosDisponibles"));
        assertEquals(List.of(), model.getAttribute("perfiles"));
        assertEquals(List.of(), model.getAttribute("menu"));
    }

    @Test
    void fragmentoUsuarios_cargaMismosDatosSinContenido() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoUsuarios(model, session);

        assertEquals("views/usuario :: content", vista);
        assertEquals(List.of(), model.getAttribute("usuarios"));
        assertEquals(List.of(), model.getAttribute("empleadosDisponibles"));
        assertEquals(List.of(), model.getAttribute("perfiles"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoUsuarios_usaPerfilDeSesionParaMenu() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(7L);
        when(usuarioService.listAll()).thenReturn(List.of());
        when(empleadoService.listEmpleadosSinUsuario()).thenReturn(List.of());
        when(perfilService.listActive()).thenReturn(List.of());
        when(opcionService.listByPerfilId(7L)).thenReturn(List.of());

        controller.fragmentoUsuarios(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaUsuarios_devuelveFragmentoDeTabla() {
        when(usuarioService.listAll()).thenReturn(List.of());

        String vista = controller.tablaUsuarios(new ExtendedModelMap());

        assertEquals("views/usuario :: tablaUsuarios", vista);
        verify(usuarioService).listAll();
    }

    private void configurarModelo() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(2L);
        when(usuarioService.listAll()).thenReturn(List.of());
        when(empleadoService.listEmpleadosSinUsuario()).thenReturn(List.of());
        when(perfilService.listActive()).thenReturn(List.of());
        when(opcionService.listByPerfilId(2L)).thenReturn(List.of());
    }
}
