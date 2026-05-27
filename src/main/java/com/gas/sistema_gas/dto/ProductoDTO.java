package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public class ProductoDTO {
    
    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        boolean requiereEnvase,
        @NotNull(message = "La categoría es obligatoria")
        Long idCategoria,
        @NotNull(message = "La ganancia del producto es obligatoria")
        @DecimalMin(value = "0.00", message = "La ganancia no puede ser negativa")
        BigDecimal gananciaProducto,
        @NotNull(message = "El stock de vacíos es obligatorio")
        @Min(value = 0, message = "El stock de vacíos no puede ser negativo")
        Integer stockVacios,
        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo
    ){}

    public record SimpleResponse(
        Long id, 
        String nombre,
        String descripcion,
        String urlImagen,
        Long idCategoria,
        String nombreCategoria,
        BigDecimal precioCompra,
        BigDecimal gananciaProducto,
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
        @NotNull(message = "La ganancia del producto es obligatoria")
        @DecimalMin(value = "0.00")
        BigDecimal gananciaProducto,
        boolean requiereEnvase,
        @NotNull
        Long idCategoria,
        @NotNull(message = "El stock de vacíos es obligatorio")
        @Min(value = 0, message = "El stock de vacíos no puede ser negativo")
        Integer stockVacios,
        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo,
        Integer estado
    ){}
}