package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.dto.MetodoPagoDTO;


@Mapper(componentModel = "spring")
public interface MetodoPagoMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    MetodoPago toEntity(MetodoPagoDTO.Create dto);
    
    MetodoPagoDTO.Response toSimpleResponse(MetodoPago metodoPago);
}
