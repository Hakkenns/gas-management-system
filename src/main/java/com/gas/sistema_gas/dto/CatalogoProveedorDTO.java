package com.gas.sistema_gas.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public class CatalogoProveedorDTO {

    // 1. Lo que viaja desde la pantalla cuando asocias un producto a un proveedor
    public record Create(
        @NotNull(message = "El proveedor es obligatorio")
        Long idProveedor,

        @NotNull(message = "El producto es obligatorio")
        Long idProducto
    ){}

    // 2. La respuesta limpia que el sistema enviará si necesitan listar las uniones
    public record SimpleResponse(
        Long idCatalogo,
        Long idProveedor,
        String nombreProveedor,
        Long idProducto,
        String nombreProducto
    ){}

    public record ProveedorCatalogoResponse(
        Long idProveedor,
        String ruc,
        String nombreProveedor,
        Long cantidadProductos
    ){}

    // Respuesta plana para el modal de Compras. Evita serializar entidades
    // JPA y sus relaciones lazy (categoría/envase) fuera de la transacción.
    public record ProductoCompraResponse(
        Long id,
        String nombre,
        BigDecimal precioVenta,
        BigDecimal gananciaProducto,
        Long idCategoria,
        String nombreCategoria,
        BigDecimal capacidad,
        String unidadMedida
    ){}
}
