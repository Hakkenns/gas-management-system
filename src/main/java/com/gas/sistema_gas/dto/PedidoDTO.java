package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PedidoDTO {
    
    public record Create(
        Long idCliente,
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dniCliente,
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombreCliente,
        @NotBlank(message = "La dirección del cliente es obligatoria")
        String direccionCliente,
        String referenciaCliente,
        @NotBlank(message = "El teléfono del cliente es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefonoCliente,
        Long idEmpleado,
        Long idUsuario,
        @NotNull(message = "El método pago es obligatorio")
        Long idMetodoPago,
        String observaciones,
        @NotNull(message = "El detalle de la venta es obligatorio")
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
        String codigo,
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
