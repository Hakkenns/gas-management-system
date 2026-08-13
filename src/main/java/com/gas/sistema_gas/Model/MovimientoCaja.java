package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Immutable
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "movimiento_caja")
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_movimiento_caja")
    private Long id;

    @NotNull(message = "La Caja es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sesion_caja")
    private SesionCaja sesionCaja;

    @NotNull(message = "La fecha del movimiento es obligatoria")
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @NotNull(message = "El sentido del movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SentidoMovimiento sentido;

    @NotNull(message = "El origen del movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrigenMovimiento origen;

    @NotNull(message = "El canal de fondos es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_fondos", nullable = false, length = 40)
    private CanalFondos canalFondos;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @NotNull(message = "El usuario responsable es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_responsable", nullable = false)
    private Usuario usuarioResponsable;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 100)
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo")
    private MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", unique = true)
    private PedidoPago pedidoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado_custodio")
    private Empleado empleadoCustodio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_movimiento_original")
    private MovimientoCaja movimientoOriginal;
}
