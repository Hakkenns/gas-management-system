package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.NotBlank;

public class MetodoPagoDTO {
    
    public record Create(
        @NotBlank(message = "El nombre del método de pago es obligatorio")
        String nombre
    ){}

    public record Response(
        Long id,
        String nombre
    ){}
}
