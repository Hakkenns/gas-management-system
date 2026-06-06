package com.gas.sistema_gas.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MotoDTO {
    public record Create(

            @NotBlank(message = "La placa es obligatoria")
            @Size(min = 6, max = 8, message = "La placa peruana debe tener entre 6 y 8 caracteres")
            String placa,

            @NotBlank(message = "La marca es obligatoria")
            String marca,

            @NotBlank(message = "El modelo es obligatorio")
            String modelo,

            @Column(name = "anio")
            Integer anio
    ){}

    public record SimpleResponse(
            Long id,
            String placa,
            String marca,
            String modelo,
            Integer anio,
            Integer estado
    ){}

    public record Update(

            @NotBlank(message = "La placa es obligatoria")
            @Size(min = 6, max = 8, message = "La placa peruana debe tener entre 6 y 8 caracteres")
            String placa,

            @NotBlank(message = "La marca es obligatoria")
            String marca,

            @NotBlank(message = "El modelo es obligatorio")
            String modelo,

            @Column(name = "anio")
            Integer anio
    ){}
}
