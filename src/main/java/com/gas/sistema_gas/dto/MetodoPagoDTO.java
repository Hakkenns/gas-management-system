package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.gas.sistema_gas.Model.TipoFinancieroMetodoPago;

public class MetodoPagoDTO {
    
    public record Create(
        @NotBlank(message = "El código técnico del método de pago es obligatorio")
        String codigo,
        @NotBlank(message = "El nombre del método de pago es obligatorio")
        String nombre,
        @NotNull(message = "El tipo financiero del método de pago es obligatorio")
        TipoFinancieroMetodoPago tipoFinanciero
    ){}

    public record Response(
        Long id,
        String codigo,
        String nombre,
        TipoFinancieroMetodoPago tipoFinanciero,
        Integer estado
    ){}
}
