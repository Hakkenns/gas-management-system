package com.gas.sistema_gas.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "inventario")

public class Inventario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_inventario")
    private Long id;

    @Column(name = "stock_llenos", nullable = false)
    @Min(value = 0, message = "El stock de llenos no pueden ser negativos")
    private Integer stockLlenos = 0;

    @Column(name = "stock_vacios", nullable = false)
    @Min(value = 0, message = "El stock de vacios no puede ser negativo")
    private Integer stockVacios = 0;

    // Relación de @OnToOne hacia la entidad producto

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false, unique = true)
    @NotNull(message = "El producto es obligatorio")
    private Producto producto;
}
