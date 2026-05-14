package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "empleados")

public class Empleado {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_empleado")
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Column(nullable = false, length = 8, unique = true)
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    private String dni;

    @Column(name = "sueldo_base", nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "El sueldo base no puede ser negativo")
    private BigDecimal sueldoBase = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Los descuentos no pueden ser negativos")
    private BigDecimal descuentos = BigDecimal.ZERO;

    @Column(nullable = false, length = 9, unique = true)
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
    private String telefono;

    @Column(nullable = false)
    private Integer estado = 1;

    // Campos de auditoría del SQL
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
