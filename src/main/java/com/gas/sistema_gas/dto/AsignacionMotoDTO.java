package com.gas.sistema_gas.dto;

import com.gas.sistema_gas.Model.AsignacionMoto.EstadoAsignacion;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class AsignacionMotoDTO {

    // 1. RECORD PARA LA CREACIÓN DE LA ASIGNACIÓN
    public record Create(
            @NotNull(message = "Debe seleccionar un empleado obligatorio")
            Long empleadoId,

            @NotNull(message = "Debe seleccionar una moto obligatoria")
            Long motoId
    ){}

    // 2. RECORD PARA LA RESPUESTA SIMPLE (Llenar tu tabla HTML)
    public record SimpleResponse(
            Long id,
            Long empleadoId,
            String empleadoNombre,
            Long motoId,
            String motoPlaca,
            String motoModelo,
            LocalDateTime fechaAsignacion,
            LocalDateTime fechaDevolucion,
            EstadoAsignacion estado
    ){}

    // 3. RECORD PARA LA ACTUALIZACIÓN (Útil por si necesitas reasignar o cambiar estados manualmente)
    public record Update(
            @NotNull(message = "El ID de asignación es necesario para actualizar")
            Long id,

            @NotNull(message = "Debe especificar el empleado")
            Long empleadoId,

            @NotNull(message = "Debe especificar la moto")
            Long motoId,

            EstadoAsignacion estado
    ){}
}