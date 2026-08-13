package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class CajaDTO {

    public record AperturaRequest(
            @NotNull(message = "El monto inicial es obligatorio")
            @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
            BigDecimal montoInicial,
            String observaciones) {
    }

    public record AperturaResponse(
            Long idSesionCaja,
            Long idMovimientoCaja,
            String codigoCaja,
            LocalDateTime fechaHoraApertura,
            BigDecimal montoInicial,
            String estado) {
    }
}
