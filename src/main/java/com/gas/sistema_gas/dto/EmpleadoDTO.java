package com.gas.sistema_gas.dto;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EmpleadoDTO {

        public record Create(

                @NotBlank(message = "El DNI es obligatorio")
                @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
                String dni,
                @NotBlank(message = "El nombre es obligatorio")
                String nombre,
                @NotNull(message = "El sueldo base es obligatorio")
                @DecimalMin(value = "0.00", message = "El sueldo no puede ser negativo")
                BigDecimal sueldoBase,
                @NotBlank(message = "El teléfono es obligatorio")
                @Pattern(regexp = "9\\d{8}", message = "El teléfono debe empezar con 9 y tener 9 dígitos")
                String telefono
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

                @NotBlank(message = "El DNI es obligatorio")
                @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
                String dni,

                @NotBlank(message = "El nombre es obligatorio")
                String nombre,

                @NotNull(message = "El sueldo base es obligatorio")
                @DecimalMin(value = "0.00", message = "El sueldo no puede ser negativo" )
                BigDecimal sueldoBase,

                @NotBlank(message = "El teléfono es obligatorio")
                @Pattern(regexp = "9\\d{8}", message = "El teléfono debe empezar con 9 y tener 9 dígitos")
                String telefono

        ){}
}
