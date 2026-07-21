package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "productos")

public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_producto")
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Lob
    @Column(name = "url_imagen", columnDefinition = "LONGTEXT")
    private String urlImagen;

    @Column(name = "precio_compra", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    private BigDecimal precioCompra;

    @Column(name = "ganancia_producto", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "La ganancia del producto es obligatoria")
    @DecimalMin(value = "0.00", message = "La ganancia no puede ser negativa")
    private BigDecimal gananciaProducto = BigDecimal.ZERO;

    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
    private BigDecimal precioVenta;

    // --- MANEJO DE STOCK SEGÚN TU SQL ---
        // --- MANEJO DE STOCK OPTIMIZADO (SOPORTA METROS Y BALONES) ---
    @Column(name = "stock_llenos", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El stock de llenos es obligatorio")
    @DecimalMin(value = "0.00", message = "El stock no puede ser negativo")
    private BigDecimal stockLlenos = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El stock mínimo es obligatorio")
    @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
    private BigDecimal stockMinimo = BigDecimal.ZERO;
    
    // El stock de envases vacíos se queda igual (Integer) porque el gas/agua se cuenta por unidades de envase enteras
    @Column(name = "stock_vacios", nullable = false)
    @NotNull(message = "El stock de vacíos es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockVacios = 0;
    

    @Column(name = "stock_reservado", nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal stockReservado = BigDecimal.ZERO;

    @Transient
    public BigDecimal getStockDisponible() {
        BigDecimal real = (this.stockLlenos != null) ? this.stockLlenos : BigDecimal.ZERO;
        BigDecimal reservado = (this.stockReservado != null) ? this.stockReservado : BigDecimal.ZERO;
        return real.subtract(reservado);
    }

    // ------------------------------------

    @Column(name = "requiere_envase", nullable = false)
    private Boolean requiereEnvase = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envase_id", referencedColumnName = "id", nullable = true)
    private Envase envase;

    @Column(nullable = false)
    private Integer estado = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    @NotNull(message = "La categoría es obligatoria")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Categoria categoria;

    @Column(name = "capacidad", precision = 5, scale = 2)
    private BigDecimal capacidad;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
