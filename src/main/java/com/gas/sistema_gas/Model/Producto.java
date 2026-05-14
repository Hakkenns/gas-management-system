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

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name ="url_imagen", length = 255)
    private String urlImagen;

    @Column(name = "precio_compra", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
    private BigDecimal precioVenta;

    // --- MANEJO DE STOCK SEGÚN TU SQL ---
    @Column(name = "stock_llenos", nullable = false)
    @NotNull(message = "El stock de llenos es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockLlenos = 0;

    @Column(name = "stock_vacios", nullable = false)
    @NotNull(message = "El stock de vacíos es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockVacios = 0;

    @Column(name = "stock_minimo", nullable = false)
    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo = 0;
    // ------------------------------------

    @Column(name = "requiere_envase", nullable = false)
    private Boolean requiereEnvase = false;

    @Column(nullable = false)
    private Integer estado = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    @NotNull(message = "La categoría es obligatoria")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    @NotNull(message = "El proveedor es obligatorio")
    private Proveedor proveedor;

    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}