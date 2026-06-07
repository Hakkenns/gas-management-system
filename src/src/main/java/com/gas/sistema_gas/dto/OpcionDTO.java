package com.gas.sistema_gas.dto;

import java.util.List;

import jakarta.validation.constraints.*;

public class OpcionDTO {
    
    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        @NotBlank(message = "El ícono es obligatorio")
        String icono,
        @NotBlank(message = "La ruta es obligatoria")
        String ruta
    ){}

    public record SimpleResponse(
        Long id,
        String nombre,
        String icono,
        String ruta,
        Integer estado,
        List<OpcionDTO.SimpleResponse> hijos
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        @NotBlank(message = "El ícono es obligatorio")
        String icono,
        @NotBlank(message = "La ruta es obligatoria")
        String ruta,
        Integer estado
    ){}
}
