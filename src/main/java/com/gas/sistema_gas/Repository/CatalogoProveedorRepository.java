package com.gas.sistema_gas.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.CatalogoProveedor;
import com.gas.sistema_gas.Model.Producto;

public interface CatalogoProveedorRepository extends JpaRepository<CatalogoProveedor, Long> {

    // LA CONSULTA DE BLINDAJE PARA LA LUPA DE COMPRAS (Usa JPQL)
    @Query("SELECT cp.producto FROM CatalogoProveedor cp " +
            "WHERE cp.proveedor.id = :idProveedor " +
            "AND cp.producto.estado = 1")
    List<Producto> findProductosByProveedorId(@Param("idProveedor") Long idProveedor);

    // 🛑 VALIDACIÓN DE SEGURIDAD: Evita duplicar el mismo producto para el mismo
    // proveedor
    boolean existsByProveedorIdAndProductoId(Long idProveedor, Long idProducto);

    // 🗑️ ELIMINACIÓN AUTOMÁTICA: Útil cuando desmarques un checkbox en la pantalla
    void deleteByProveedorIdAndProductoId(Long idProveedor, Long idProducto);
}
