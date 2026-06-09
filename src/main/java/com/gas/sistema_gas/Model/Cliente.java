package com.gas.sistema_gas.Model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // <-- Ignora los campos internos de Hibernate
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // <-- Evita que busque la lista de clientes

@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // <-- Indicar que es un primary key
    @EqualsAndHashCode.Include  // <-- Comparaciones rápidas por ID
    @Column(name = "id_cliente")  // <-- Nombre de la columna en la base de datos
    private Long id;

    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caractéres")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 8, unique = true)
    @Pattern(regexp = "\\d{8}|^$", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @Pattern(regexp = "^$|\\d{9}", message = "El teléfono debe tener 9 dígitos")
    @Column(length = 9)
    private String telefono;

    @Column
    private String direccion;

    @Column(length = 150)
    private String referencia;

    @Email(message = "Debe proporcionar un formato de email válido")  // Este valida el @ y el .com
    private String correo;

    @Column(nullable = false)
    private Integer estado = 1;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}
