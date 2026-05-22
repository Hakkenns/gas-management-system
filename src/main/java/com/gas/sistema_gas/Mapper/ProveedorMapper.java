package com.gas.sistema_gas.Mapper;

import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.dto.ProveedorDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {

    // 1. Al Crear: Mapea idRubro al ID del objeto interno 'rubro' en la Entidad
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(source = "idRubro", target = "rubro.id")
    Proveedor toEntity(ProveedorDTO.Create createDto);

    // 2. Al Responder: Extrae el ID y el Nombre desde la entidad 'Rubro' hacia los campos del DTO
    @Mapping(source = "rubro.id", target = "idRubro")
    @Mapping(source = "rubro.nombre", target = "nombreRubro")
    ProveedorDTO.SimpleResponse toSimpleResponse(Proveedor proveedor);

    // 3. Al Actualizar: Mapea idRubro en la edición
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(source = "idRubro", target = "rubro.id")
    void updateEntityFromDto(ProveedorDTO.Update updateDto, @MappingTarget Proveedor proveedor);
}