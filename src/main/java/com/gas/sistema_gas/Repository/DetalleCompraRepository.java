package com.gas.sistema_gas.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.gas.sistema_gas.Model.DetalleCompra;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    
    // Al usar "findByCompraId", Spring Data JPA busca automáticamente por el campo "id" dentro del objeto "compra"
    List<DetalleCompra> findByCompraId(Long idCompra);
    
    // Contar detalles activos (compra no anulada) de un producto, excluyendo una compra específica
    @Query("SELECT COUNT(dc) FROM DetalleCompra dc WHERE dc.producto.id = :productoId " +
           "AND dc.compra.id != :compraId AND dc.compra.situacion != :situacionAnulada")
    long countByProductoIdAndCompraIdNotAndCompraSituacionNot(
            @Param("productoId") Long productoId,
            @Param("compraId") Long compraId,
            @Param("situacionAnulada") Integer situacionAnulada);
}