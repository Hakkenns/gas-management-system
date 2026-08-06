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
    // CON PESSIMISTIC_WRITE: exclusivamente para descuento físico de stock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT il FROM InventarioLote il " +
           "WHERE il.producto.id = :idProducto " +
           "AND il.cantidadActual > 0 " +
           "ORDER BY il.createdAt ASC")
    List<InventarioLote> findLotesDisponiblesPEPS(@Param("idProducto") Long idProducto);

    // CONSULTA DE SOLO LECTURA PEPS: Misma lógica que findLotesDisponiblesPEPS pero SIN bloqueo.
    // Para operaciones de lectura (precios PEPS, visualización) que no modifican lotes.
    @Query("SELECT il FROM InventarioLote il " +
           "WHERE il.producto.id = :idProducto " +
           "AND il.cantidadActual > 0 " +
           "ORDER BY il.createdAt ASC")
    List<InventarioLote> findLotesDisponiblesPEPSLectura(@Param("idProducto") Long idProducto);

    @Query("SELECT il FROM InventarioLote il " +
           "WHERE il.producto.id = :idProducto " +
           "AND il.cantidadActual > 0 " +
           "ORDER BY il.createdAt ASC")
    List<InventarioLote> findLotesParaDespachoMetros(@Param("idProducto") Long idProducto);

    // CONSULTA DE HISTORIAL COMPLETO: Trae absolutamente todos los lotes de un producto
    // (incluso los agotados con cantidadActual = 0) ordenados del más reciente al más antiguo.
    // Para el modal de historial y para obtener el último lote histórico.
    @Query("SELECT il FROM InventarioLote il WHERE il.producto.id = :idProducto ORDER BY il.createdAt DESC")
    List<InventarioLote> findHistorialCompletoByProductoIdOrderByCreatedAtDesc(@Param("idProducto") Long idProducto);

    // Método derivado: verifica si existe al menos un lote para el producto.
    boolean existsByProducto_Id(Long idProducto);

    // Buscar lotes asociados a una compra para poder deshacer su inventario si se anula la factura
    List<InventarioLote> findByCompraId(Long idCompra);

    // CONSULTA DE SOLO LECTURA: Descubre los IDs únicos de productos asociados
    // a los lotes de una compra, ordenados ASC. Se ejecuta después de bloquear
    // la Compra y antes de bloquear los Productos.
    @Query("SELECT DISTINCT l.producto.id " +
           "FROM InventarioLote l " +
           "WHERE l.compra.id = :idCompra " +
           "ORDER BY l.producto.id ASC")
    List<Long> findProductoIdsByCompraId(@Param("idCompra") Long idCompra);

    // CONSULTA PESIMISTA: Bloquea todos los lotes de una compra ordenados por ID ASC.
    // Se ejecuta después de bloquear los Productos.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l " +
           "FROM InventarioLote l " +
           "WHERE l.compra.id = :idCompra " +
           "ORDER BY l.id ASC")
    List<InventarioLote> findByCompraIdForUpdate(@Param("idCompra") Long idCompra);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l
        FROM InventarioLote l
        WHERE l.producto.id IN :idsProductos
        ORDER BY l.producto.id ASC, l.id ASC
    """)
    List<InventarioLote> findByProductoIdsForUpdate(
        @Param("idsProductos") List<Long> idsProductos
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
