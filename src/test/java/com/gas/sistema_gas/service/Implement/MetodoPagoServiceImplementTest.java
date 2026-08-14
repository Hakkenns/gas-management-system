package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.MetodoPagoMapper;
import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.TipoFinancieroMetodoPago;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.dto.MetodoPagoDTO;

@ExtendWith(MockitoExtension.class)
class MetodoPagoServiceImplementTest {

    @Mock
    private MetodoPagoRepository repository;

    @Mock
    private MetodoPagoMapper mapper;

    @InjectMocks
    private MetodoPagoServiceImplement service;

    @Test
    void create_normalizaCodigoTecnico() {
        MetodoPago entidad = new MetodoPago();
        when(repository.findByCodigo("YAPE_NEGOCIO")).thenReturn(Optional.empty());
        when(repository.findByNombre("Yape negocio")).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(entidad);
        when(repository.saveAndFlush(entidad)).thenReturn(entidad);
        when(mapper.toSimpleResponse(entidad)).thenReturn(
                new MetodoPagoDTO.Response(1L, "YAPE_NEGOCIO", "Yape negocio",
                        TipoFinancieroMetodoPago.DIGITAL, 1));

        MetodoPagoDTO.Response response = service.create(
                new MetodoPagoDTO.Create(" yape_negocio ", " Yape negocio ", TipoFinancieroMetodoPago.DIGITAL));

        assertEquals("YAPE_NEGOCIO", entidad.getCodigo());
        assertEquals("Yape negocio", entidad.getNombre());
        assertEquals(TipoFinancieroMetodoPago.DIGITAL, entidad.getTipoFinanciero());
        assertEquals(1, entidad.getEstado());
        assertEquals("YAPE_NEGOCIO", response.codigo());
    }

    @Test
    void create_codigoDuplicado_rechazaConflict() {
        when(repository.findByCodigo("YAPE")).thenReturn(Optional.of(new MetodoPago()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(new MetodoPagoDTO.Create("yape", "Yape", TipoFinancieroMetodoPago.DIGITAL)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void create_nombreDuplicado_rechazaConflict() {
        when(repository.findByCodigo("YAPE_NEGOCIO")).thenReturn(Optional.empty());
        when(repository.findByNombre("Yape negocio")).thenReturn(Optional.of(new MetodoPago()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(
                        new MetodoPagoDTO.Create("yape_negocio", " Yape negocio ", TipoFinancieroMetodoPago.DIGITAL)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void obtenerActivo_idNulo_rechazaBadRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.obtenerActivo(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void obtenerActivo_inexistente_rechazaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.obtenerActivo(99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void obtenerActivo_inactivo_rechazaConflict() {
        MetodoPago metodo = metodo(TipoFinancieroMetodoPago.EFECTIVO, 0);
        when(repository.findById(1L)).thenReturn(Optional.of(metodo));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.obtenerActivo(1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void obtenerActivo_sinClasificacion_rechazaConflict() {
        MetodoPago metodo = metodo(null, 1);
        when(repository.findById(1L)).thenReturn(Optional.of(metodo));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.obtenerActivo(1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void numeroOperacion_efectivoNullOBlank_normalizaNull() {
        MetodoPago efectivo = metodo(TipoFinancieroMetodoPago.EFECTIVO, 1);

        assertEquals(null, service.validarYNormalizarNumeroOperacion(efectivo, null));
        assertEquals(null, service.validarYNormalizarNumeroOperacion(efectivo, "   "));
    }

    @Test
    void numeroOperacion_efectivoConContenido_rechazaBadRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarYNormalizarNumeroOperacion(metodo(TipoFinancieroMetodoPago.EFECTIVO, 1), "OP-1"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void numeroOperacion_digital_requiereValorYConservaCase() {
        MetodoPago digital = metodo(TipoFinancieroMetodoPago.DIGITAL, 1);

        ResponseStatusException nullException = assertThrows(ResponseStatusException.class,
                () -> service.validarYNormalizarNumeroOperacion(digital, null));
        assertEquals(HttpStatus.BAD_REQUEST, nullException.getStatusCode());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarYNormalizarNumeroOperacion(digital, "   "));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("AbC-123", service.validarYNormalizarNumeroOperacion(digital, "  AbC-123  "));
    }

    @Test
    void numeroOperacion_digitalMayorACincuenta_rechazaBadRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarYNormalizarNumeroOperacion(metodo(TipoFinancieroMetodoPago.DIGITAL, 1), "x".repeat(51)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private MetodoPago metodo(TipoFinancieroMetodoPago tipoFinanciero, Integer estado) {
        MetodoPago metodo = new MetodoPago();
        metodo.setId(1L);
        metodo.setCodigo("PRUEBA");
        metodo.setNombre("Método prueba");
        metodo.setTipoFinanciero(tipoFinanciero);
        metodo.setEstado(estado);
        return metodo;
    }
}
