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

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class EmpleadoControllerTest {

    @Mock
    private EmpleadoService empleadoService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private EmpleadoController controller;

    @Test
    void empleados_cargaModeloCompletoYLayout() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.empleados(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/empleado", model.getAttribute("contenido"));
        assertEquals(List.of(), model.getAttribute("empleados"));
        assertEquals(List.of(), model.getAttribute("menu"));
    }

    @Test
    void fragmentoEmpleados_cargaMismosDatosSinContenido() {
        configurarModelo();
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoEmpleados(model, session);

        assertEquals("views/empleado :: content", vista);
        assertEquals(List.of(), model.getAttribute("empleados"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoEmpleados_usaPerfilDeSesionParaMenu() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(7L);
        when(empleadoService.listAll()).thenReturn(List.of());
        when(opcionService.listByPerfilId(7L)).thenReturn(List.of());

        controller.fragmentoEmpleados(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaEmpleados_devuelveFragmentoDeTabla() {
        when(empleadoService.listAll()).thenReturn(List.of());

        String vista = controller.tablaEmpleados(new ExtendedModelMap());

        assertEquals("views/empleado :: tablaEmpleados", vista);
        verify(empleadoService).listAll();
    }

    private void configurarModelo() {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(2L);
        when(empleadoService.listAll()).thenReturn(List.of());
        when(opcionService.listByPerfilId(2L)).thenReturn(List.of());
    }
}
