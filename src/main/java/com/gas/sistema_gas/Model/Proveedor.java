package com.gas.sistema_gas.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "proveedores")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

public class Proveedor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_proveedor")
    private Long id;    

    @Column(nullable = false, length = 150)
    @NotBlank(message = "El nombre de proveedor es obligatorio")
    private String nombre;

    @Column(length = 100)
    private String rubro;

    @Column(nullable = false, length = 9)
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos exactamente")
    private String telefono;

    @Column(length = 150)
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @Column(nullable = false, length = 11, unique = true)
    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos exactamente")
    private String ruc;

    @Column(nullable = false)
    private Integer estado = 1;
}