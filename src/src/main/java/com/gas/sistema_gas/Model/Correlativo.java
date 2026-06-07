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
@Table(name = "correlativos", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tipo", "serie"}, name = "serie_tipo_idx")
})
public class Correlativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_correlativo")
    private Integer id;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    @Column(nullable = false, length = 10)
    @NotBlank(message = "La serie es obligatoria")
    private String serie;

    @Column(name = "numero_actual", nullable = false)
    @NotNull(message = "El número actual es obligatorio")
    private Integer numeroActual = 0;
}
