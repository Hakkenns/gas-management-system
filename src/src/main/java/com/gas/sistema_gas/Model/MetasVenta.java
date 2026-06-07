package com.gas.sistema_gas.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.validator.constraints.Range;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "metas_venta")
public class MetasVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_metas")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    @Min(2020)
    private Integer anio;

    @Column(nullable = false)
    @Range(min = 1, max = 12)
    private Integer mes;

    @Column(name = "obj_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal objetivoVenta;

    @Column(name = "bono_meta", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonoMeta;

    @Column(nullable = false)
    private Integer estado = 1;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;
}