package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.dto.ClienteDTO;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    
    /*
     *  Convertir el DTO de creación a al entidad Cliente.
     *  Ignoramos el ID porque lo genera la base de datos y el estado
     *  porque este tiene un valor predeterminado de 1 en la entidad
     */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Cliente toEntity(ClienteDTO.Create createDto);
    
    // Convertir la entidad Cliente a Dto.SimpleResponse
    ClienteDTO.SimpleResponse toSimpleResponse(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ClienteDTO.Update updateDto, @MappingTarget Cliente cliente);
}
