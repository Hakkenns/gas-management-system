package com.gas.sistema_gas.Mapper;

import com.gas.sistema_gas.Model.Compra;
import com.gas.sistema_gas.dto.CompraDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompraMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "idProveedor", target = "proveedor.id")
    Compra toEntity(CompraDTO.Create createDto);

    @Mapping(source = "proveedor.nombre", target = "nombreProveedor")
    @Mapping(source = "usuario.userName", target = "nombreUsuario")
    @Mapping(source = "fechaCompra", target = "fechaCompra", dateFormat = "dd/MM/yyyy HH:mm")
    CompraDTO.SimpleResponse toSimpleResponse(Compra compra);
}