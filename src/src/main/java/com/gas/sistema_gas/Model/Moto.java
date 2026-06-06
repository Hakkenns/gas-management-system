package com.gas.sistema_gas.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "motos")
public class Moto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_moto")
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @NotBlank(message = "La placa es obligatoria")
    private String placa;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El modelo es obligatorio")
    private String modelo;

    @Column(name = "anio")
    private Integer anio;

    @Column(nullable = false)
    private Integer estado = 1;

    // Auditoría (coincidiendo con tu SQL)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}