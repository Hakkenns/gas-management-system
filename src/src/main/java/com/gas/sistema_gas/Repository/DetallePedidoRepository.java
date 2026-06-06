package com.gas.sistema_gas.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Pedido;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    // Para la anulación y para listar los productos de un pedido
    List<DetallePedido> findByPedido_Id(Long idPedido);
    List<DetallePedido> findByPedido(Pedido pedido);
}