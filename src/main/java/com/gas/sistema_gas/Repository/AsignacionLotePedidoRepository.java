package com.gas.sistema_gas.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.AsignacionLotePedido;
import jakarta.persistence.LockModeType;

public interface AsignacionLotePedidoRepository extends JpaRepository<AsignacionLotePedido, Long> {

    boolean existsByIdDetallePedidoAndEstado(
        Long idDetallePedido,
        AsignacionLotePedido.EstadoAsignacion estado
    );

    boolean existsByPedido_Id(Long idPedido);

    boolean existsByPedido_IdAndEstado(
        Long idPedido,
        AsignacionLotePedido.EstadoAsignacion estado
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM AsignacionLotePedido a
        WHERE a.pedido.id = :idPedido
          AND a.estado = :estado
        ORDER BY a.idAsignacion ASC
    """)
    List<AsignacionLotePedido> findByPedidoIdAndEstadoForUpdate(
        @Param("idPedido") Long idPedido,
        @Param("estado") AsignacionLotePedido.EstadoAsignacion estado
    );

    // Verifica si existe cualquier trazabilidad (DESCONTADA o DEVUELTA)
    // para una lista de IDs de lotes. No requiere bloqueo pesimista porque
    // la anulacion ya mantiene bloqueados Productos y Lotes.
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END " +
           "FROM AsignacionLotePedido a " +
           "WHERE a.lote.id IN :idsLotes")
    boolean existsByLoteIdIn(@Param("idsLotes") List<Long> idsLotes);
}
