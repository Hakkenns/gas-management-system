package com.gas.sistema_gas.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "perfiles")
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_perfil")
    private Long id;

    @Column(name = "nombre_perfil", nullable = false, length = 100)
    @NotBlank(message = "El nombre de perfil es obligatorio")
    private String nombrePerfil;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer estado = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "perfil_opcion", 
        joinColumns = @JoinColumn(name = "id_perfil"), 
        inverseJoinColumns = @JoinColumn(name = "id_opcion")
    )
    private List<Opcion> opciones;
}