package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gas.sistema_gas.Model.Opcion;
import com.gas.sistema_gas.dto.OpcionDTO;

@Mapper(componentModel = "spring")
public interface OpcionMapper {
    
    // Convertir DTO Create a entidad Opcion
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "perfiles", ignore = true)
    Opcion toEntity(OpcionDTO.Create createDto);
    
    // Convertir Opcion a SimpleResponse
    @Mapping(source = "estado", target = "estado")
    OpcionDTO.SimpleResponse toSimpleResponse(Opcion opcion);
    
    // Actualizar Opcion desde DTO Update
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "perfiles", ignore = true)
    void updateEntityFromDto(OpcionDTO.Update updateDto, @MappingTarget Opcion opcion);
}
