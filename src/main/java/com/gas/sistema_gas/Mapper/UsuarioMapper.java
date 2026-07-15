package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.dto.LoginDTO;
import com.gas.sistema_gas.dto.UsuarioDTO;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "perfil", ignore = true)
    @Mapping(target = "empleado", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Usuario toEntity(UsuarioDTO.Create createDto);

    @Mapping(source = "perfil.nombrePerfil", target = "nombrePerfil")
    @Mapping(source = "perfil.id", target = "idPerfil")
    @Mapping(source = "empleado.nombre", target = "nombre")
    UsuarioDTO.SimpleResponse toSimpleResponse(Usuario usuario);

    @Mapping(source = "empleado.nombre", target = "nombre")
    @Mapping(source = "empleado.dni", target = "dni")
    @Mapping(source = "empleado.telefono", target = "telefono")
    @Mapping(source = "perfil.nombrePerfil", target = "perfil")
    UsuarioDTO.PerfilResponse toPerfilResponse(Usuario usuario);

    @Mapping(source = "perfil.nombrePerfil", target = "nombrePerfil")
    @Mapping(source = "perfil.id", target = "idPerfil")
    @Mapping(source = "empleado.nombre", target = "nombre")
    @Mapping(source = "userName", target = "username")
    LoginDTO.Response toLoginResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "perfil", ignore = true)
    @Mapping(target = "empleado", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void updateEntityFromDto(UsuarioDTO.Update updateDto, @MappingTarget Usuario usuario);
}
