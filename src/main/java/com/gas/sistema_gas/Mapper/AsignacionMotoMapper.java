package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gas.sistema_gas.Model.AsignacionMoto;
import com.gas.sistema_gas.dto.AsignacionMotoDTO;

@Mapper(componentModel = "spring")
public interface AsignacionMotoMapper {

    // Convertir de DTO de Creación a Entidad
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaAsignacion", ignore = true)
    @Mapping(target = "fechaDevolucion", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "empleadoId", target = "empleado.id")
    @Mapping(source = "motoId", target = "moto.id")
    AsignacionMoto toEntity(AsignacionMotoDTO.Create createDto);

    // Convertir Entidad a Respuesta Simple plana para la Tabla
    @Mapping(source = "empleado.id", target = "empleadoId")
    @Mapping(source = "empleado.nombre", target = "empleadoNombre")
    @Mapping(source = "moto.id", target = "motoId")
    @Mapping(source = "moto.placa", target = "motoPlaca")
    @Mapping(source = "moto.modelo", target = "motoModelo")
    AsignacionMotoDTO.SimpleResponse toSimpleResponse(AsignacionMoto asignacion);

    // Actualizar Entidad desde el DTO de Update
    @Mapping(target = "fechaAsignacion", ignore = true)
    @Mapping(target = "fechaDevolucion", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "empleadoId", target = "empleado.id")
    @Mapping(source = "motoId", target = "moto.id")
    void updateEntityFromDto(AsignacionMotoDTO.Update updateDto, @MappingTarget AsignacionMoto asignacion);
}