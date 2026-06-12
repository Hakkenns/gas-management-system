package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoriaDTO {
    
    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        @NotBlank(message = "La forma de venta es obligatoria")
        String tipoUnidad
    ){}

    public record SimpleResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer estado,
        String unidadMedida,
        Boolean requiereCapacidad,
        String etiquetaCapacidad,
        Boolean manejaEnvase
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        @NotBlank(message = "La forma de venta es obligatoria")
        String tipoUnidad
    ){}
}
