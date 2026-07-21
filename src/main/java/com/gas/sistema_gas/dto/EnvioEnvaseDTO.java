package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EnvioEnvaseDTO {

    public record DeudorResponse(
        Long idControl,
        Long idCliente,
        String nombreCliente,
        String telefonoCliente,
        String direccionCliente,
        Long idPedido,
        String codigoPedido,
        LocalDateTime fechaEntrega,
        String productoNombre,
        Integer cantidadPrestada,
        Integer cantidadDevuelta,
        Integer cantidadPendiente,
        String estado,
        Long idProducto
    ) {}

    public record DevolucionRequest(
        Long idControl,
        Integer cantidadDevolver
    ) {}
}