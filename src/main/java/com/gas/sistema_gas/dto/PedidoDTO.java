package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class PedidoDTO {
    
    public record Create(
        @NotNull(message = "El cliente es obligatorio")
        Long idCliente,
        @NotNull(message = "El empleado es obligatorio")
        Long idEmpleado,
        @NotNull(message = "El usuario es obligatorio")
        Long idUsuario,
        @NotNull(message = "El método pago es obligatorio")
        Long idMetodoPago,
        String observaciones,
        List<DetalleCreate> detalles
    ){}

    // Para cada linea del carrito de compras
    public record DetalleCreate(
        Long idProducto,
        Integer cantidad,
        BigDecimal precioUnitario
    ){}

    public record SimpleResponse(
        Long idPedido,
        LocalDateTime fechaSolicitud,
        String nombreCliente,
        String nombreEmpleado,
        String estadoPedido,
        String estadoPago,
        BigDecimal montoTotal,
        BigDecimal subtotal,
        String metodoPago
    ){}

    public record Update(
        Long idMotorizado,
        String estadoPedido,
        String estadoPago,
        String numOperacion,
        String observaciones
    ){}

    /*
     * DTO opcional por si necesitas ver el detalle completo de un pedido específico
     
    public record FullResponse(
        Long id,
        String codigo,
        LocalDateTime fechaSolicitud,
        String nombreCliente,
        String direccionEntrega,
        String nombreEmpleado,
        BigDecimal subtotal,
        BigDecimal montoTotal,
        String estadoPedido,
        String metodoPago,
        List<DetalleResponse> detalles
    ){}


     * DTO para mostrar los productos dentro de un FullResponse
    public record DetalleResponse(
        Long idProducto,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal importe // (cantidad * precioUnitario)
  
     */
}
