package com.gas.sistema_gas.Mapper;

import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.dto.ProveedorDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Proveedor toEntity(ProveedorDTO.Create createDto);

    ProveedorDTO.SimpleResponse toSimpleResponse(Proveedor proveedor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void updateEntityFromDto(ProveedorDTO.Update updateDto, @MappingTarget Proveedor proveedor);
}