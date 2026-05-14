package com.gas.sistema_gas.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "opciones")

public class Opcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_opciones")
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El ícono es obligatorio")
    private String icono;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String ruta;

    @Column(nullable = false)
    private Integer estado = 1;

    // Relación ManyToMany con Perfil (lado inverso)
    @ManyToMany(mappedBy = "opciones")
    @JsonIgnoreProperties("opciones")
    private List<Perfil> perfiles;
}