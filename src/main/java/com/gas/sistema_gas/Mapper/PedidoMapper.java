package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.dto.PedidoDTO;

@Mapper(componentModel = "spring")
public interface PedidoMapper {
    
    //Al crear, ignoramos las relaciones porque se cargarán manualmente en el ServiceImplement
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "empleado", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    @Mapping(target = "fechaSolicitud", ignore = true)
    @Mapping(target = "montoTotal", ignore = true)
    @Mapping(target = "fechaEntrega", ignore = true)
    @Mapping(target = "estadoPago", ignore = true)
    @Mapping(target = "estadoPedido", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Pedido toEntity(PedidoDTO.Create createDto);

    // Para la respuesta, extraemos los nombres de las relaciones
    @Mapping(source = "id", target = "idPedido")
    @Mapping(source = "cliente.nombre", target = "nombreCliente")
    @Mapping(source = "empleado.nombre", target = "nombreEmpleado")
    @Mapping(source = "metodoPago.nombre", target = "metodoPago")
    PedidoDTO.SimpleResponse toSimpleResponse(Pedido pedido);

    // Actualizar campos modificables de pedido
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "empleado", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    @Mapping(target = "fechaSolicitud", ignore = true)
    @Mapping(target = "fechaEntrega", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "montoTotal", ignore = true)
    @Mapping(target = "observaciones", source = "observaciones")
    @Mapping(target = "tipoVenta", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(PedidoDTO.Update updateDto, @org.mapstruct.MappingTarget Pedido pedido);


}