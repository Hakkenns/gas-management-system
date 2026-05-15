package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.dto.CategoriaDTO;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Categoria toEntity(CategoriaDTO.Create createDto);

    CategoriaDTO.SimpleResponse toSimpleResponse(Categoria categoria);
}
