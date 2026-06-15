package com.gas.sistema_gas.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventarioLoteDTO {

    // 1. Estructura estándar para crear el lote desde compras
    public record Create(
        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull(message = "El proveedor es obligatorio")
        Long idProveedor,

        @NotNull(message = "La cantidad inicial es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
        BigDecimal cantidadInicial,

        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
        BigDecimal precioCompra,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
        BigDecimal precioVenta,

        Long idCompra
    ){}

    // 2. Tu Record estándar 'Update' adaptado para cambiar el precio de venta
    public record Update(
        @NotNull(message = "El nuevo precio de venta es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
        BigDecimal precioVenta
    ){}

    // 3. Tu Record estándar 'SimpleResponse' para el listado del modal
    public record SimpleResponse(
        Long id,
        Long idProducto,
        String nombreProducto,
        Long idProveedor,
        String nombreProveedor,
        BigDecimal cantidadInicial,
        BigDecimal cantidadActual,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ){}
}
