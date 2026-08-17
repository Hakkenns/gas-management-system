package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public class CajaDTO {

    public record AperturaRequest(
            @NotNull(message = "El monto inicial es obligatorio")
            @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
            @Digits(integer = 10, fraction = 2, message = "El monto inicial debe tener como maximo 2 decimales")
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

    public record CierreRequest(
            @NotNull(message = "El monto declarado es obligatorio")
            @DecimalMin(value = "0.00", message = "El monto declarado no puede ser negativo")
            @Digits(integer = 10, fraction = 2, message = "El monto declarado debe tener como maximo 2 decimales")
            BigDecimal montoDeclarado,
            String observaciones) {
    }

    public record CierreResponse(
            Long idSesionCaja,
            String codigoCaja,
            LocalDateTime fechaHoraCierre,
            BigDecimal montoEsperado,
            BigDecimal montoDeclarado,
            BigDecimal diferencia,
            String estado) {
    }

    public record LiquidacionRequest(
            @NotNull(message = "El empleado custodio es obligatorio")
            Long empleadoCustodioId,
            @NotNull(message = "El monto de liquidación es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto de liquidación debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "El monto de liquidación debe tener como maximo 2 decimales")
            BigDecimal monto,
            String observacion) {
    }

    public record LiquidacionResponse(
            Long idMovimientoEgresoCustodia,
            Long idMovimientoIngresoCaja,
            Long idSesionCaja,
            Long empleadoCustodioId,
            BigDecimal montoLiquidado,
            BigDecimal saldoAnterior,
            BigDecimal saldoPendiente,
            LocalDateTime fechaHora,
            String referencia) {
    }

    public record CustodiaPendienteResponse(
            Long empleadoId,
            String nombreEmpleado,
            BigDecimal saldoPendiente,
            Integer estadoEmpleado) {
    }

    public record EstadoResponse(
            String codigoCaja,
            Boolean activa,
            Boolean sesionAbierta,
            Long idSesionCaja,
            LocalDateTime fechaHoraApertura,
            BigDecimal efectivoEsperado) {
    }
}
