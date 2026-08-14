package com.gas.sistema_gas.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
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
@Table(name = "metodo_pago")

public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_metodo")
    private Long id;

    @NotBlank(message = "El código técnico del método de pago es obligatorio")
    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String codigo;

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank(message = "El nombre del método de pago es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo financiero del método de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_financiero", nullable = false, length = 20, updatable = false)
    private TipoFinancieroMetodoPago tipoFinanciero;

    @Column(nullable = false)
    private Integer estado = 1;
}
