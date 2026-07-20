package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CategoriaDTO {
    
    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        @NotBlank(message = "La forma de venta es obligatoria")
        String tipoUnidad,
        List<Double> capacidades
    ){}

    public record SimpleResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer estado,
        String unidadMedida,
        Boolean requiereCapacidad,
        String etiquetaCapacidad,
        Boolean manejaEnvase,
        List<Double> capacidades
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        @NotBlank(message = "La forma de venta es obligatoria")
        String tipoUnidad,
        List<Double> capacidades
    ){}

    // DTO para respuesta detallada con información de uso de capacidades
    public record DetalleResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer estado,
        String unidadMedida,
        Boolean requiereCapacidad,
        String etiquetaCapacidad,
        Boolean manejaEnvase,
        List<CapacidadInfoDTO> capacidades
    ){}
}
