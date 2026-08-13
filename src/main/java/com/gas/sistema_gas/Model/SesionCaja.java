package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import jakarta.validation.constraints.NotNull;
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
@Table(name = "sesion_caja")
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_sesion_caja")
    private Long id;

    @NotNull(message = "La Caja es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    @NotNull(message = "La fecha de apertura es obligatoria")
    @Column(name = "fecha_hora_apertura", nullable = false)
    private LocalDateTime fechaHoraApertura;

    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;

    @NotNull(message = "El estado de sesión es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesionCaja estado;

    @NotNull(message = "El usuario de apertura es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_apertura", nullable = false)
    private Usuario usuarioApertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cierre")
    private Usuario usuarioCierre;

    @Column(name = "monto_esperado_cierre", precision = 12, scale = 2)
    private BigDecimal montoEsperadoCierre;

    @Column(name = "monto_declarado_cierre", precision = 12, scale = 2)
    private BigDecimal montoDeclaradoCierre;

    @Column(name = "diferencia_cierre", precision = 12, scale = 2)
    private BigDecimal diferenciaCierre;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}
