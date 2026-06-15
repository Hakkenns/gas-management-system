package com.gas.sistema_gas.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public class ProductoDTO {
    
    public record Create(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        boolean requiereEnvase,
        @NotNull(message = "La categoría es obligatoria")
        Long idCategoria,
        BigDecimal capacidad,
        String unidadMedida,
        @NotNull(message = "La ganancia del producto es obligatoria")
        @DecimalMin(value = "1.00", message = "La ganancia mínima es S/1.00")
        BigDecimal gananciaProducto,
        @NotNull(message = "El stock de vacíos es obligatorio")
        @Min(value = 0, message = "El stock de vacíos no puede ser negativo")
        Integer stockVacios,
        @NotNull(message = "El stock mínimo es obligatorio")
        @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo
    ){
        @AssertTrue(message = "La capacidad debe respetar las reglas según unidad: KG >= 10 entero, L >= 20 entero, M >= 50 con decimales permitidos.")
        public boolean isCapacidadValida() {
            return validarCapacidad(capacidad, unidadMedida);
        }

        @AssertTrue(message = "La ganancia debe ser al menos S/1.00 y en incrementos de S/0.10.")
        public boolean isGananciaValida() {
            return validarGanancia(gananciaProducto);
        }

        private static boolean validarCapacidad(BigDecimal capacidad, String unidadMedida) {
            // 🌟 CORREGIDO: Si es UND, la capacidad no importa y se aprueba de inmediato
            if (unidadMedida == null || unidadMedida.isBlank() || unidadMedida.equals("UND")) {
                return true; 
            }
            if (capacidad == null) {
                return false;
            }

            switch (unidadMedida) {
                case "KG":
                    return capacidad.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0
                            && capacidad.compareTo(BigDecimal.valueOf(10)) >= 0;
                case "L":
                    return capacidad.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0
                            && capacidad.compareTo(BigDecimal.valueOf(20)) >= 0;
                case "M":
                    return capacidad.compareTo(BigDecimal.valueOf(50)) >= 0;
                default:
                    return capacidad.compareTo(BigDecimal.ZERO) >= 0;
            }
        }

        private static boolean validarGanancia(BigDecimal ganancia) {
            if (ganancia == null || ganancia.compareTo(BigDecimal.valueOf(1.00)) < 0) {
                return false;
            }
            return ganancia.remainder(BigDecimal.valueOf(0.10)).compareTo(BigDecimal.ZERO) == 0;
        }
    }

    public record SimpleResponse(
        Long id, 
        String nombre,
        String descripcion,
        String urlImagen,
        Long idCategoria,
        String nombreCategoria,
        BigDecimal capacidad,
        String unidadMedida,
        BigDecimal precioCompra,
        BigDecimal gananciaProducto,
        BigDecimal precioVenta,
        boolean requiereEnvase,
        BigDecimal stockLlenos,
        Integer stockVacios,
        BigDecimal stockMinimo,
        boolean tieneHistorialLotes,
        Integer estado
    ){}

    public record Update(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String descripcion,
        BigDecimal capacidad,
        String unidadMedida,
        @NotNull(message = "La ganancia del producto es obligatoria")
        @DecimalMin(value = "1.00", message = "La ganancia mínima es S/1.00")
        BigDecimal gananciaProducto,
        boolean requiereEnvase,
        @NotNull
        Long idCategoria,
        @NotNull(message = "El stock de vacíos es obligatorio")
        @Min(value = 0, message = "El stock de vacíos no puede ser negativo")
        Integer stockVacios,
        @NotNull(message = "El stock mínimo es obligatorio")
        @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo,
        Integer estado
    ){
        @AssertTrue(message = "La capacidad debe respetar las reglas según unidad: KG >= 10 entero, L >= 20 entero, M >= 50 con decimales permitidos.")
        public boolean isCapacidadValida() {
            return validarCapacidad(capacidad, unidadMedida);
        }

        @AssertTrue(message = "La ganancia debe ser al menos S/1.00 y en incrementos de S/0.10.")
        public boolean isGananciaValida() {
            return validarGanancia(gananciaProducto);
        }

        private static boolean validarCapacidad(BigDecimal capacidad, String unidadMedida) {
            // 🌟 CORREGIDO: Si es UND, la capacidad no importa y se aprueba de inmediato
            if (unidadMedida == null || unidadMedida.isBlank() || unidadMedida.equals("UND")) {
                return true;
            }
            if (capacidad == null) {
                return false;
            }

            switch (unidadMedida) {
                case "KG":
                    return capacidad.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0
                            && capacidad.compareTo(BigDecimal.valueOf(10)) >= 0;
                case "L":
                    return capacidad.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0
                            && capacidad.compareTo(BigDecimal.valueOf(20)) >= 0;
                case "M":
                    return capacidad.compareTo(BigDecimal.valueOf(50)) >= 0;
                default:
                    return capacidad.compareTo(BigDecimal.ZERO) >= 0;
                }
        }

        private static boolean validarGanancia(BigDecimal ganancia) {
            if (ganancia == null || ganancia.compareTo(BigDecimal.valueOf(1.00)) < 0) {
                return false;
            }
            return ganancia.remainder(BigDecimal.valueOf(0.10)).compareTo(BigDecimal.ZERO) == 0;
        }
    }
}
