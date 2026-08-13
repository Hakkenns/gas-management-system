package com.gas.sistema_gas.Model;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_detalle")
    private Long idDetalle;

    @Column(nullable = false)
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio unitario es obligatorio")
    private BigDecimal precioUnitario;

    /**
     * Vacíos que deben recibirse al entregar un CANJE a domicilio.
     * Cero indica que no hay retorno pendiente o que ya fue aplicado.
     */
    @Column(name = "cantidad_canje")
    @Min(value = 0, message = "La cantidad de canje no puede ser negativa")
    private Integer cantidadCanje = 0;

    /**
     * Identifica una línea facturable de envase vendido. No representa
     * contenido sujeto a lotes, reservas ni consumo PEPS.
     */
    @Column(name = "es_envase_vendido", nullable = false)
    private Boolean esEnvaseVendido = false;

    // Relación ManyToOne con Pedido
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    @NotNull(message = "El pedido es obligatorio")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Pedido pedido;

    // Relación ManyToOne con Producto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    @NotNull(message = "El producto es obligatorio")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Producto producto;
}
