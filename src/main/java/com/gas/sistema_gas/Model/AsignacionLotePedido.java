package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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
@Table(
    name = "asignacion_lote_pedido",
    indexes = {
        @Index(name = "idx_asignacion_pedido", columnList = "id_pedido"),
        @Index(name = "idx_asignacion_producto", columnList = "id_producto"),
        @Index(name = "idx_asignacion_lote", columnList = "id_lote"),
        @Index(name = "idx_asignacion_detalle", columnList = "id_detalle_pedido"),
        @Index(name = "idx_asignacion_estado", columnList = "estado")
    }
)
public class AsignacionLotePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_asignacion")
    private Long idAsignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private InventarioLote lote;

    @Column(name = "id_detalle_pedido", nullable = false)
    private Long idDetallePedido;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadDescontada;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadDevuelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum EstadoAsignacion {
        DESCONTADA,
        DEVUELTA
    }
}
