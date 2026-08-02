
package com.gas.sistema_gas.Repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.InventarioLote;
import jakarta.persistence.LockModeType;

public interface InventarioLoteRepository extends JpaRepository<InventarioLote, Long> {

    // LA CONSULTA CRÍTICA PEPS: Busca los lotes de un producto que tengan stock vivo (cantidadActual > 0)
    // y los ordena de forma estricta desde el más antiguo hasta el más nuevo (createdAt ASC)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT il FROM InventarioLote il " +
           "WHERE il.producto.id = :idProducto " +
           "AND il.cantidadActual > 0 " +
           "ORDER BY il.createdAt ASC")
    List<InventarioLote> findLotesDisponiblesPEPS(@Param("idProducto") Long idProducto);

    // CONSULTA PARA EL NUEVO BOTÓN: Trae absolutamente todos los lotes de un producto
    // (incluso los agotados con cantidadActual = 0) para mostrarlos en el historial del modal
    @Query("SELECT il FROM InventarioLote il WHERE il.producto.id = :idProducto AND il.cantidadActual > 0 ORDER BY il.createdAt ASC")
    List<InventarioLote> findByProductoIdOrderByCreatedAtDesc(@Param("idProducto") Long idProducto);

    // Preparación del algoritmo de despacho por metros: lotes activos ordenados cronológicamente
    @Query("SELECT il FROM InventarioLote il WHERE il.producto.id = :idProducto AND il.cantidadActual > 0 ORDER BY il.createdAt ASC")
    List<InventarioLote> findLotesParaDespachoMetros(@Param("idProducto") Long idProducto);

    // Buscar lotes asociados a una compra para poder deshacer su inventario si se anula la factura
    List<InventarioLote> findByCompraId(Long idCompra);

    @Query("SELECT il FROM InventarioLote il WHERE il.producto.id = :idProducto AND il.proveedor.id = :idProveedor ORDER BY il.createdAt DESC")
    List<InventarioLote> findUltimoPrecioCosto(@Param("idProducto") Long idProducto, @Param("idProveedor") Long idProveedor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l
        FROM InventarioLote l
        WHERE l.id IN :ids
        ORDER BY l.id ASC
    """)
    List<InventarioLote> findAllByIdInForUpdate(
        @Param("ids") List<Long> ids
    );

    @Query("""
        SELECT COALESCE(SUM(l.cantidadActual), 0)
        FROM InventarioLote l
        WHERE l.producto.id = :idProducto
    """)
    BigDecimal sumCantidadActualByProductoId(
        @Param("idProducto") Long idProducto
    );
}

