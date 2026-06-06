package com.gas.sistema_gas.Mapper;

import com.gas.sistema_gas.Model.Moto;
import com.gas.sistema_gas.dto.MotoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MotoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Moto toEntity(MotoDTO.Create createDto);

    MotoDTO.SimpleResponse toSimpleResponse(Moto moto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void toUpdateFromDto(MotoDTO.Update updateDto, @MappingTarget Moto moto);
}
