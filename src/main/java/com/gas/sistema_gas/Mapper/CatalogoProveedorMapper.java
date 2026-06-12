package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.gas.sistema_gas.Model.CatalogoProveedor;
import com.gas.sistema_gas.dto.CatalogoProveedorDTO;

@Mapper(componentModel = "spring")
public interface CatalogoProveedorMapper {

    // 1. Quitamos los mapeos anidados que daban error. El Service se encargará de setear los objetos.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CatalogoProveedor toEntity(CatalogoProveedorDTO.Create createDto);

    // 2. Mapeamos solo la respuesta básica de forma directa
    @Mapping(target = "idCatalogo", source = "id")
    @Mapping(target = "idProveedor", source = "proveedor.id")
    @Mapping(target = "idProducto", source = "producto.id")
    // Dejamos los nombres fuera para asignarlos manualmente en el Service de forma segura
    @Mapping(target = "nombreProveedor", ignore = true)
    @Mapping(target = "nombreProducto", ignore = true)
    CatalogoProveedorDTO.SimpleResponse toSimpleResponse(CatalogoProveedor catalogo);
}

