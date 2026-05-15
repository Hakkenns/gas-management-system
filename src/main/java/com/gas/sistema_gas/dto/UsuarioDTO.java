package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UsuarioDTO {

    public record Create(
        @NotBlank(message = "El nombre completo es obligatorio")
        String nombre,
        @NotBlank(message = "El username es obligatorio")
        String userName,
        @NotBlank(message = "La contraseña es obligatoria")
        String password,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String correo,
        @NotNull(message = "El ID de perfil es obligatorio")
        Long idPerfil
    ) {}


    public record SimpleResponse(
        Long id,
        String nombre,
        String userName,
        String correo,
        Integer estado,
        String nombrePerfil,
        Long idPerfil
    ) {}

     public record Update(
        @NotBlank(message = "El nombre completo es obligatorio")
        String nombre,
        @NotBlank(message = "El username es obligatorio")
        String userName,
        String password,     
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String correo,
        @NotNull(message = "El ID de perfil es obligatorio")
        Long idPerfil
    ) {}
}
