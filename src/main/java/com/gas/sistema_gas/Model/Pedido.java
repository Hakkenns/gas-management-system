package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "pedidos") // 1. Cambiado a plural para seguir el estándar
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Mantiene el AUTO_INCREMENT
    @EqualsAndHashCode.Include
    @Column(name = "id_pedido")
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @NotBlank(message = "El código de pedido es obligatorio")
    private String codigo;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    // 2. Control de estados optimizado desde Java
    @Column(name = "estado_pedido", nullable = false, length = 20)
    private String estadoPedido = "PENDIENTE"; 

    @Column(name = "estado_pago", nullable = false, length = 20)
    private String estadoPago = "PENDIENTE";

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.00", message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto total no puede ser negativo")
    private BigDecimal montoTotal;

    @Column(length = 255)
    private String observaciones;

    // Relaciones seguras con Lazy Fetch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Empleado empleado;

    @Column(name = "tipo_venta", nullable = false, length = 20)
    private String tipoVenta = "DOMICILIO"; // LOCAL o DOMICILIO

    @Column(name = "fecha_limite_pago")
    private LocalDateTime fechaLimitePago;

    // 3. Auditoría automatizada de verdad (Compatibilidad total con la base de datos)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false, 
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false, 
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // 4. Asegura que la fecha de solicitud nunca vaya nula al insertar
    @PrePersist
    protected void onCreate() {
        if (this.fechaSolicitud == null) {
            this.fechaSolicitud = LocalDateTime.now();
        }
    }
}