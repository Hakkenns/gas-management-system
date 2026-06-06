package com.gas.sistema_gas.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public class PerfilDTO {

    public record Create(
        @NotBlank(message = "El nombre de perfil es obligatorio")
        String nombrePerfil,
        String descripcion,
        List<Long> idOpciones // Recibimos solo los IDs de los permisos
    ){}

    public record Response(
        Long id,
        String nombrePerfil,
        String descripcion,
        Integer estado,
        List<OpcionDTO.SimpleResponse> opciones // Mostramos el detalle de las opciones
    ){}

    public record Simple(
        Long id,
        String nombrePerfil
    ){}
}