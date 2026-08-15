package com.gas.sistema_gas.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;

import jakarta.persistence.LockModeType;

public interface PedidoPagoRepository extends JpaRepository<PedidoPago, Long> {
    List<PedidoPago> findByPedido_Id(Long idPedido);
    List<PedidoPago> findByPedido(Pedido pedido);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PedidoPago p
            WHERE p.id IN :ids
            ORDER BY p.id ASC
            """)
    List<PedidoPago> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    boolean existsByPedido_IdAndMontoGreaterThan(Long idPedido, BigDecimal monto);
}
