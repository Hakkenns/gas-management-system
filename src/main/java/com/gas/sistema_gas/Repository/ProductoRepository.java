package com.gas.sistema_gas.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.Producto;
import jakarta.persistence.LockModeType;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    // Buscar productos activos
    List<Producto> findByEstado(Integer estado);
    
    // Buscar productos por categoría
    List<Producto> findByCategoriaId(Long idCategoria);
    
    // Alerta de Stock Bajo (Crucial para el negocio)
    @Query("SELECT p FROM Producto p WHERE p.stockLlenos <= p.stockMinimo AND p.estado = 1")
    List<Producto> findProductosSinStock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Producto p
        WHERE p.id IN :ids
        ORDER BY p.id ASC
    """)
    List<Producto> findAllByIdInForUpdate(
        @Param("ids") List<Long> ids
    );
}
