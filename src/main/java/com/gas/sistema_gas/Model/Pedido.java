package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_pedido")
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @NotBlank(message = "El código de pedido es obligatorio")
    private String codigo; // Agregado para coincidir con el UK del SQL

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "estado_pedido", nullable = false)
    private String estadoPedido = "PENDIENTE";

    @Column(name = "estado_pago", nullable = false)
    private String estadoPago = "PENDIENTE";

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.00", message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto total no puede ser negativo")
    private BigDecimal montoTotal;

    @Column(name = "num_operacion", length = 50)
    private String numOperacion;

    @Column(length = 255)
    private String observaciones;

    // Relaciones según tu SQL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado") // El repartidor/motorizado asignado
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo")
    private MetodoPago metodoPago;

    // Campos de auditoría
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.fechaSolicitud == null) {
            this.fechaSolicitud = LocalDateTime.now();
        }
    }
}