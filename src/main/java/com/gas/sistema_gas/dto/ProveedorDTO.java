package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.*;

public class ProveedorDTO {

    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        
        @NotNull(message = "El rubro es obligatorio") // Cambiado a idRubro (Long)
        Long idRubro,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefono,

        @Email(message = "Formato de correo no válido")
        String correo,

        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
        String ruc
    ){}

    public record SimpleResponse(
        Long id,
        String nombre,
        Long idRubro,        // ID del rubro por si lo necesitas en JS
        String nombreRubro,  // Nombre del rubro para pintarlo en tu tabla HTML (ej: "Gas")
        String telefono,
        String correo,
        String ruc,
        Integer estado
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        
        @NotNull(message = "El rubro es obligatorio") // Cambiado a idRubro (Long)
        Long idRubro,
        
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefono,
        
        @Email(message = "Formato de correo no válido")
        String correo,
        
        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
        String ruc
    ){}
}