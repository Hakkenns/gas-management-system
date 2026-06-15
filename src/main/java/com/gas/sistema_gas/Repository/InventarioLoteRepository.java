
package com.gas.sistema_gas.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.InventarioLote;

public interface InventarioLoteRepository extends JpaRepository<InventarioLote, Long> {

    // LA CONSULTA CRÍTICA PEPS: Busca los lotes de un producto que tengan stock vivo (cantidadActual > 0)
    // y los ordena de forma estricta desde el más antiguo hasta el más nuevo (createdAt ASC)
    @Query("SELECT il FROM InventarioLote il " +
           "WHERE il.producto.id = :idProducto " +
           "AND il.cantidadActual > 0 " +
           "ORDER BY il.createdAt ASC")
    List<InventarioLote> findLotesDisponiblesPEPS(@Param("idProducto") Long idProducto);

    // CONSULTA PARA EL NUEVO BOTÓN: Trae absolutamente todos los lotes de un producto
    // (incluso los agotados con cantidadActual = 0) para mostrarlos en el historial del modal
    List<InventarioLote> findByProductoIdOrderByCreatedAtDesc(Long idProducto);

    // Buscar lotes asociados a una compra para poder deshacer su inventario si se anula la factura
    List<InventarioLote> findByCompraId(Long idCompra);
}

