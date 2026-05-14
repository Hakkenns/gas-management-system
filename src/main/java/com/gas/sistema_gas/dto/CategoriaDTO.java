package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoriaDTO {
    
    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion
    ){}

    public record SimpleResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer estado
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion
    ){}
}
