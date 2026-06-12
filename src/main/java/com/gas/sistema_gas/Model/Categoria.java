package com.gas.sistema_gas.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categorias")

public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_categoria")
    private Long id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer estado = 1;


    @Column(name = "unidad_medida", nullable = false, length = 10)
    private String unidadMedida = "UND";

    @Column(name = "requiere_capacidad", nullable = false)
    private Boolean requiereCapacidad = false;

    @Column(name = "etiqueta_capacidad", length = 50)
    private String etiquetaCapacidad;

    @Column(name = "maneja_envase", nullable = false)
    private Boolean manejaEnvase = false;



    // Campos de auditoría según tu SQL
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}