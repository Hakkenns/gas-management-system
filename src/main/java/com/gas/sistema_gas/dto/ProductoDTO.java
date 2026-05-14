package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public class ProductoDTO {
    
    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        String urlImagen,
        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.00", message= "El precio de compra no puede ser negativo")
        BigDecimal precioCompra,
        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
        BigDecimal precioVenta,
        boolean requiereEnvase,
        @NotNull(message = "La categoría es obligatoria")
        Long idCategoria,
        @NotNull(message = "El proveedor es obligatorio")
        Long idProveedor,
        @Min(value = 0, message = "El stock de llenos no puede ser negativo")
        Integer stockLlenos,
        @Min(value = 0, message = "El stock de vacíos no puede ser negativo")
        Integer stockVacios,
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo
    ){}

    public record SimpleResponse(
        Long id, 
        String nombre,
        String nombreCategoria,
        String nombreProveedor,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        boolean requiereEnvase,
        Integer stockLlenos,
        Integer stockVacios,
        Integer stockMinimo,
        Integer estado
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        String urlImagen,
        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal precioCompra,
        @DecimalMin(value = "0.01")
        BigDecimal precioVenta,
        boolean requiereEnvase,
        @NotNull
        Long idCategoria,
        @NotNull
        Long idProveedor,
        Integer stockLlenos,
        Integer stockVacios,
        Integer stockMinimo,
        Integer estado
    ){}
}