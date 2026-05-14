package com.gas.sistema_gas.Mapper;

import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.dto.PerfilDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring"/* , uses = {OpcionMapper.class}*/)
public interface PerfilMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "opciones", ignore = true) // Se cargan manualmente en el Service
    Perfil toEntity(PerfilDTO.Create createDto);

    PerfilDTO.Response toResponse(Perfil entity);

    PerfilDTO.Simple toSimple(Perfil entity);
}