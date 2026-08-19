package com.gas.sistema_gas.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.CatalogoProveedor;
import com.gas.sistema_gas.Model.Producto;

public interface CatalogoProveedorRepository extends JpaRepository<CatalogoProveedor, Long> {

    // LA CONSULTA DE BLINDAJE PARA LA LUPA DE COMPRAS (Usa JPQL)
    @Query("SELECT cp.producto FROM CatalogoProveedor cp " +
            "WHERE cp.proveedor.id = :idProveedor " +
            "AND cp.proveedor.estado = 1 " +
            "AND cp.producto.estado = 1")
    List<Producto> findProductosByProveedorId(@Param("idProveedor") Long idProveedor);

    // 🛑 VALIDACIÓN DE SEGURIDAD: Evita duplicar el mismo producto para el mismo
    // proveedor
    boolean existsByProveedorIdAndProductoId(Long idProveedor, Long idProducto);

    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END " +
            "FROM CatalogoProveedor cp " +
            "WHERE cp.proveedor.id = :idProveedor " +
            "AND cp.producto.id = :idProducto " +
            "AND cp.proveedor.estado = 1 " +
            "AND cp.producto.estado = 1")
    boolean existsRelacionActiva(@Param("idProveedor") Long idProveedor,
                                 @Param("idProducto") Long idProducto);

    @Query(value = "SELECT p.id, p.ruc, p.nombre, COUNT(DISTINCT pr.id) " +
            "FROM CatalogoProveedor cp JOIN cp.proveedor p JOIN cp.producto pr " +
            "WHERE p.estado = 1 AND pr.estado = 1 " +
            "AND (:texto = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR p.ruc LIKE CONCAT('%', :texto, '%') " +
            "OR LOWER(pr.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
            "GROUP BY p.id, p.ruc, p.nombre ORDER BY p.nombre ASC, p.id ASC",
           countQuery = "SELECT COUNT(DISTINCT p.id) " +
            "FROM CatalogoProveedor cp JOIN cp.proveedor p JOIN cp.producto pr " +
            "WHERE p.estado = 1 AND pr.estado = 1 " +
            "AND (:texto = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR p.ruc LIKE CONCAT('%', :texto, '%') " +
            "OR LOWER(pr.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))")
    Page<Object[]> buscarProveedoresActivos(
            @Param("texto") String texto, Pageable pageable);

    // 🗑️ ELIMINACIÓN AUTOMÁTICA: Útil cuando desmarques un checkbox en la pantalla
    void deleteByProveedorIdAndProductoId(Long idProveedor, Long idProducto);
}
