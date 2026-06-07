package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CompraDTO {

    public record Create(
        @NotNull(message = "El proveedor es obligatorio")
        Long idProveedor,

        @NotBlank(message = "El número de documento es obligatorio")
        String numDocumento,

        String fechaCompra, // Llega desde el input del formulario

        @NotNull(message = "El monto total es obligatorio")
        BigDecimal montoTotal,

        @NotEmpty(message = "Debe añadir al menos un producto")
        List<DetalleItem> detalles
    ){}

    public record DetalleItem(
        Long idProducto,
        Integer cantidad,
        BigDecimal precioCostoUnitario
    ){}

    public record SimpleResponse(
        Long id,
        String numDocumento,
        String nombreProveedor,
        BigDecimal montoTotal,
        String fechaCompra,
        String nombreUsuario,
        Integer situacion
    ){}
}