package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.dto.ProductoDTO;

@Mapper(componentModel = "spring") 
public interface ProductoMapper {
    //para converti de dto a entidad
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Producto toEntity(ProductoDTO.Create createDto);
    
    //para respuesta simple
    @Mapping(source = "categoria.id", target = "idCategoria")
    @Mapping(source = "categoria.nombre", target = "nombreCategoria")
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "estado", target = "estado")
    @Mapping(target = "tieneHistorialLotes", constant = "false")
    @Mapping(target = "stockDisponible", expression = "java(producto.getStockDisponible())")
    ProductoDTO.SimpleResponse toSimpleResponse(Producto producto);

    //paraupdate esto es el qeui nos esta fallando
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "urlImagen", ignore = true)
    @Mapping(target = "precioCompra", ignore = true)
    @Mapping(target = "precioVenta", ignore = true)
    @Mapping(target = "stockLlenos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ProductoDTO.Update updateDto, @MappingTarget Producto producto);
}