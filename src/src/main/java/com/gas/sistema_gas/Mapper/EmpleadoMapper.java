package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.dto.EmpleadoDTO;

@Mapper(componentModel = "spring")
public interface EmpleadoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "descuentos", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Empleado toEntity(EmpleadoDTO.Create dto);

    @Mapping(target = "id", ignore = true)
    EmpleadoDTO.SimpleResponse toSimpleResponse(Empleado empleado);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "descuentos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(EmpleadoDTO.Update updateDTO,@MappingTarget Empleado empleeado);
}
