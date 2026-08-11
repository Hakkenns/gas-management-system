package com.gas.sistema_gas.Mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.dto.ClienteDTO;

class ClienteMapperTest {

    private final ClienteMapper clienteMapper = Mappers.getMapper(ClienteMapper.class);

    @Test
    void createDebeMapearPrestamoIlimitadoYUsarFalseCuandoEsNulo() {
        Cliente autorizado = clienteMapper.toEntity(crearDto(true));
        Cliente noAutorizado = clienteMapper.toEntity(crearDto(false));
        Cliente ausente = clienteMapper.toEntity(crearDto(null));

        assertAll(
                () -> assertTrue(autorizado.getPrestamoIlimitado()),
                () -> assertFalse(noAutorizado.getPrestamoIlimitado()),
                () -> assertFalse(ausente.getPrestamoIlimitado())
        );
    }

    @Test
    void updateDebeMapearTrueYFalseYConservarValorCuandoEsNulo() {
        Cliente cliente = new Cliente();
        cliente.setPrestamoIlimitado(false);

        clienteMapper.updateEntityFromDto(actualizarDto(true), cliente);
        assertTrue(cliente.getPrestamoIlimitado());

        clienteMapper.updateEntityFromDto(actualizarDto(false), cliente);
        assertFalse(cliente.getPrestamoIlimitado());

        cliente.setPrestamoIlimitado(true);
        clienteMapper.updateEntityFromDto(actualizarDto(null), cliente);
        assertTrue(cliente.getPrestamoIlimitado());
    }

    @Test
    void simpleResponseDebeExponerPrestamoIlimitado() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setPrestamoIlimitado(true);

        ClienteDTO.SimpleResponse respuesta = clienteMapper.toSimpleResponse(cliente);

        assertEquals(Boolean.TRUE, respuesta.prestamoIlimitado());
    }

    private ClienteDTO.Create crearDto(Boolean prestamoIlimitado) {
        return new ClienteDTO.Create(
                "Cliente Test", "12345678", "999999999", "Dirección", "Referencia", "test@example.com",
                prestamoIlimitado);
    }

    private ClienteDTO.Update actualizarDto(Boolean prestamoIlimitado) {
        return new ClienteDTO.Update(
                "Cliente Test", "12345678", "999999999", "Dirección", "Referencia", "test@example.com",
                prestamoIlimitado);
    }
}
