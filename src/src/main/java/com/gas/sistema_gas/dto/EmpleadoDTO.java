package com.gas.sistema_gas.dto;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EmpleadoDTO {
        public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        @NotNull(message = "El sueldo base es obligatorio")
        @DecimalMin(value = "0.00", message = "El sueldo no puede ser negativo")
        BigDecimal sueldoBase,
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefono,
        @Column(nullable = false, length = 8, unique = true)
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni
    ){}

    public record SimpleResponse(
        Long id,
        String nombre,
        String dni,
        String telefono,
        BigDecimal sueldoBase,
        Integer estado
    ){}

        public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @Column(nullable = false, length = 8, unique = true)
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,

        @NotNull(message = "El sueldo base es obligatorio")
        @DecimalMin(value = "0.00", message = "El sueldo no puede ser negativo" )
        BigDecimal sueldoBase,
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefono
    ){}
}
