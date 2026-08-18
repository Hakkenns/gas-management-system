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

import com.gas.sistema_gas.service.AsignacionMotoService;
import com.gas.sistema_gas.service.EmpleadoService;
import com.gas.sistema_gas.service.MotoService;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class AsignacionMotoControllerTest {

    @Mock
    private AsignacionMotoService asignacionMotoService;

    @Mock
    private EmpleadoService empleadoService;

    @Mock
    private MotoService motoService;

    @Mock
    private OpcionService opcionService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AsignacionMotoController controller;

    @Test
    void index_cargaModeloCompletoYLayout() {
        configurarModelo(2L);
        Model model = new ExtendedModelMap();

        String vista = controller.index(model, session);

        assertEquals("components/layout", vista);
        assertEquals("views/asignacion_moto", model.getAttribute("contenido"));
        assertEquals(List.of(), model.getAttribute("asignaciones"));
        assertEquals(List.of(), model.getAttribute("empleados"));
        assertEquals(List.of(), model.getAttribute("motos"));
        assertEquals(List.of(), model.getAttribute("menu"));
        verify(asignacionMotoService).listAll();
        verify(empleadoService).listDisponibles();
        verify(motoService).listDisponibles();
        verify(opcionService).listByPerfilId(2L);
    }

    @Test
    void fragmentoAsignaciones_cargaDatosNecesariosSinContenido() {
        configurarModelo(3L);
        Model model = new ExtendedModelMap();

        String vista = controller.fragmentoAsignaciones(model, session);

        assertEquals("views/asignacion_moto :: content", vista);
        assertEquals(List.of(), model.getAttribute("asignaciones"));
        assertEquals(List.of(), model.getAttribute("empleados"));
        assertEquals(List.of(), model.getAttribute("motos"));
        assertEquals(List.of(), model.getAttribute("menu"));
        assertNull(model.getAttribute("contenido"));
    }

    @Test
    void fragmentoAsignaciones_usaPerfilDeSesionParaMenu() {
        configurarModelo(7L);

        controller.fragmentoAsignaciones(new ExtendedModelMap(), session);

        verify(opcionService).listByPerfilId(7L);
    }

    @Test
    void tablaAsignaciones_devuelveFragmentoDeTabla() {
        when(asignacionMotoService.listAll()).thenReturn(List.of());

        String vista = controller.tablaAsignaciones(new ExtendedModelMap());

        assertEquals("views/asignacion_moto :: tablaAsignaciones", vista);
        verify(asignacionMotoService).listAll();
    }

    private void configurarModelo(Long perfilId) {
        when(session.getAttribute("usuarioPerfilId")).thenReturn(perfilId);
        when(asignacionMotoService.listAll()).thenReturn(List.of());
        when(empleadoService.listDisponibles()).thenReturn(List.of());
        when(motoService.listDisponibles()).thenReturn(List.of());
        when(opcionService.listByPerfilId(perfilId)).thenReturn(List.of());
    }
}
