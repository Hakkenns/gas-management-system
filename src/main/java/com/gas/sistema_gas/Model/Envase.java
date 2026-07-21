package com.gas.sistema_gas.Model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "envases")
public class Envase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidad;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "precio_envase", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioEnvase;

    @Column(name = "stock_inicial", nullable = false)
    private Integer stockInicial = 0;

    @Column(nullable = false)
    private Boolean estado = true;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}
