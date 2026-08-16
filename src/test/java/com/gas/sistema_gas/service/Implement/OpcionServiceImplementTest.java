package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gas.sistema_gas.Mapper.OpcionMapper;
import com.gas.sistema_gas.Model.Opcion;
import com.gas.sistema_gas.Repository.OpcionRepository;

@ExtendWith(MockitoExtension.class)
class OpcionServiceImplementTest {

    @Mock
    private OpcionRepository repository;

    @Mock
    private OpcionMapper opcionMapper;

    @InjectMocks
    private OpcionServiceImplement service;

    @Test
    void administrador_conCajaActiva_tieneAcceso() {
        Opcion caja = opcion(1);
        when(repository.findByRuta(eq("caja"))).thenReturn(Optional.of(caja));

        assertTrue(service.tieneAccesoRuta(1L, "caja"));
        verify(repository).findByRuta("caja");
        verify(repository, never()).existsActiveByRutaAndPerfilId("caja", 1L);
    }

    @Test
    void administrador_conCajaInactiva_noTieneAcceso() {
        Opcion caja = opcion(0);
        when(repository.findByRuta(eq("caja"))).thenReturn(Optional.of(caja));

        assertFalse(service.tieneAccesoRuta(1L, "caja"));
        verify(repository).findByRuta("caja");
    }

    @Test
    void perfilNormal_conPermiso_tieneAcceso() {
        when(repository.existsActiveByRutaAndPerfilId("caja", 2L)).thenReturn(true);

        assertTrue(service.tieneAccesoRuta(2L, "caja"));
        verify(repository).existsActiveByRutaAndPerfilId("caja", 2L);
    }

    @Test
    void perfilNormal_sinPermiso_noTieneAcceso() {
        when(repository.existsActiveByRutaAndPerfilId("caja", 2L)).thenReturn(false);

        assertFalse(service.tieneAccesoRuta(2L, "caja"));
        verify(repository).existsActiveByRutaAndPerfilId("caja", 2L);
    }

    @Test
    void perfilNuloORutaBlank_noConsultaRepository() {
        assertFalse(service.tieneAccesoRuta(null, "caja"));
        assertFalse(service.tieneAccesoRuta(2L, "   "));

        verifyNoInteractions(repository);
    }

    private Opcion opcion(int estado) {
        Opcion opcion = new Opcion();
        opcion.setRuta("caja");
        opcion.setEstado(estado);
        return opcion;
    }
}
