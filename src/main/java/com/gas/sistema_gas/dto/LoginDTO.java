package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginDTO {

    /**
     * DTO para recibir credenciales en el login
     */
    public record Request(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String username,
        
        @NotBlank(message = "La contraseña es obligatoria")
        String password
    ) {}

    /**
     * DTO para responder después de un login exitoso
     */
    public record Response(
        Long id,
        String nombre,
        String username,
        String correo,
        String nombrePerfil
    ) {}
}
