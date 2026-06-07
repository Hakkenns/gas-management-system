package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UsuarioDTO {

    public record Create(
            @NotNull(message = "El empleado es obligatorio")
            Long idEmpleado, // Vincula la relación con el selector

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
            @NotBlank(message = "El username es obligatorio")
            String userName,

            String password, // Opcional al editar

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Formato de correo inválido")
            String correo,

            @NotNull(message = "El ID de perfil es obligatorio")
            Long idPerfil
    ) {}
}
