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
        BigDecimal capacidad,
        String unidadMedida,
        @NotNull(message = "La ganancia del producto es obligatoria")
        @DecimalMin(value = "0.00", message = "La ganancia no puede ser negativa")
        BigDecimal gananciaProducto,
        @NotNull(message = "El stock de vacíos es obligatorio")
        @Min(value = 0, message = "El stock de vacíos no puede ser negativo")
        Integer stockVacios,
        @NotNull(message = "El stock mínimo es obligatorio")
        @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo
    ){}

    public record SimpleResponse(
        Long id, 
        String nombre,
        String descripcion,
        String urlImagen,
        Long idCategoria,
        String nombreCategoria,
        BigDecimal capacidad,
        String unidadMedida,
        BigDecimal precioCompra,
        BigDecimal gananciaProducto,
        BigDecimal precioVenta,
        boolean requiereEnvase,
        BigDecimal stockLlenos,
        Integer stockVacios,
        BigDecimal stockMinimo,
        Integer estado
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        BigDecimal capacidad,
        String unidadMedida,
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
        @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo,
        Integer estado
    ){}
}