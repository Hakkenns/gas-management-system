package com.gas.sistema_gas.Mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.dto.InventarioLoteDTO;

@Mapper(componentModel = "spring")
public interface InventarioLoteMapper {

    // Traduce de la compra (Create) al modelo de Hibernate
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "compra", ignore = true)
    @Mapping(target = "cantidadActual", ignore = true)
    @Mapping(target = "metrosPorRollo", source = "metrosPorRollo")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventarioLote toEntity(InventarioLoteDTO.Create createDto);

    // Traduce del modelo de la base de datos a tu SimpleResponse estándar
    @Mapping(target = "id", source = "id")
    @Mapping(target = "idProducto", source = "producto.id")
    @Mapping(target = "idProveedor", source = "proveedor.id")
    @Mapping(target = "nombreProducto", ignore = true) // Seteado manualmente en Service
    @Mapping(target = "nombreProveedor", ignore = true) // Seteado manualmente en Service
    @Mapping(target = "metrosPorRollo", source = "metrosPorRollo")
    InventarioLoteDTO.SimpleResponse toSimpleResponse(InventarioLote lote);
}
