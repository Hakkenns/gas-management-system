package com.gas.sistema_gas.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
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
@Table(name = "catalogo_proveedores")
public class CatalogoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_catalogo")
    private Long id;

    // Relación ManyToOne con la entidad Proveedor (Maestro)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    // Relación ManyToOne con la entidad Producto (Maestro)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    // Campo de auditoría de creación automática según el SQL
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    
}
