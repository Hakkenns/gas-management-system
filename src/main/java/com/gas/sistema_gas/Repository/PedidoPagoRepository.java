package com.gas.sistema_gas.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;

public interface PedidoPagoRepository extends JpaRepository<PedidoPago, Long> {
    List<PedidoPago> findByPedido_Id(Long idPedido);
    List<PedidoPago> findByPedido(Pedido pedido);
    boolean existsByPedido_IdAndMontoGreaterThan(Long idPedido, BigDecimal monto);
}
