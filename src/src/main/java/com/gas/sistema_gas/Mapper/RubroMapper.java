package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.gas.sistema_gas.Model.Rubro;
import com.gas.sistema_gas.dto.RubroDTO;

@Mapper(componentModel = "spring")
public interface RubroMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true) // Ignoramos auditoría en creación
    @Mapping(target = "updatedAt", ignore = true)
    Rubro toEntity(RubroDTO.Create createDto);

    RubroDTO.SimpleResponse toSimpleResponse(Rubro rubro);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true) // Ignoramos auditoría en edición
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(RubroDTO.Update updateDto, @MappingTarget Rubro rubro);
}