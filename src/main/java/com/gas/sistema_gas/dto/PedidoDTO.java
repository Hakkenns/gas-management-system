package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PedidoDTO {
    
    public record Create(
        Long idPedido,
        Long idCliente,
        @Pattern(regexp = "^$|\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dniCliente,
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombreCliente,
        String direccionCliente,
        String referenciaCliente,
        @Pattern(regexp = "^$|\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefonoCliente,
        Long idEmpleado,
        Long idUsuario,
        Long idMetodoPago,
        String numOperacion,
        String observaciones,
        String estadoPedido,
        String tipoVenta,
        LocalDateTime fechaLimitePago,
        List<PagoCreate> pagos,
        @NotNull(message = "El detalle de la venta es obligatorio")
        List<DetalleCreate> detalles,
        // Movimiento de envases
        String tipoMovimientoEnvase, // "NINGUNO", "VENTA", "PRESTAMO", "CANJE"
        List<EnvaseMovimientoCreate> envaseMovimientos
    ){}

    // Para cada linea del carrito de compras
    public record DetalleCreate(
        Long idProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        Integer cantidadPrestada
    ){}

    // Para cada envase en el movimiento de envases
    public record EnvaseMovimientoCreate(
        Long idProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        String fechaLimiteDevolucion,
        String observacion,
        String tipoPrestamo
    ){}

    public record PagoCreate(
        Long idMetodoPago,
        BigDecimal monto,
        String numOperacion
    ){}

    public record SimpleResponse(
        Long idPedido,
        String codigo,
        LocalDateTime fechaSolicitud,
        String nombreCliente,
        String direccionCliente,
        Long empleadoId,
        String observaciones,
        String nombreEmpleado,
        String estadoPedido,
        String estadoPago,
        BigDecimal montoTotal,
        BigDecimal subtotal,
        String metodoPago,
        String tipoVenta,
        LocalDateTime fechaLimitePago,
        List<EvidenciaResponse> evidencias
    ){}

    public record Update(
        Long idMotorizado,
        String estadoPedido,
        String estadoPago,
        String numOperacion,
        String observaciones
    ){}

    public record EditResponse(
        Long idPedido,
        String codigo,
        Long idCliente,
        String dniCliente,
        String nombreCliente,
        String telefonoCliente,
        String direccionCliente,
        String referenciaCliente,
        Long idEmpleado,
        String observaciones,
        String estadoPedido,
        String tipoVenta,
        LocalDateTime fechaLimitePago,
        List<DetalleResponse> detalles,
        List<PagoResponse> pagos
    ){}

    public record DetalleResponse(
        Long idProducto,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        Integer cantidadPrestada,
        Integer cantidadCanje
    ){}

    public record PagoResponse(
        Long idMetodoPago,
        String metodoNombre,
        BigDecimal monto,
        String numOperacion
    ){}

    public record EvidenciaResponse(
        Long idEvidencia,
        String urlImagen,
        String tipoEvidencia
    ){}
}
