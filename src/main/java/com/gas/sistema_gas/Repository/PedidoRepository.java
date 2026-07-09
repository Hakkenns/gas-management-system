package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar por el código único (ej: PED-0001)
    Optional<Pedido> findByCodigo(String codigo);

    // Listar pedidos por estado (PENDIENTE, ENTREGADO, CANCELADO)
    List<Pedido> findByEstadoPedido(String estadoPedido);

    // Listar pedidos de un cliente específico
    List<Pedido> findByClienteId(Long idCliente);

    // Listar pedidos por tipo de venta
    List<Pedido> findByTipoVenta(String tipoVenta);

    // Listar pedidos por tipo de venta y empleado (motorizado)
    List<Pedido> findByTipoVentaAndEmpleadoId(String tipoVenta, Long idEmpleado);

    // Reporte de ventas entre fechas (Muy útil para el cierre de caja)
    @Query("SELECT p FROM Pedido p WHERE p.fechaSolicitud BETWEEN :inicio AND :fin")
    List<Pedido> findPedidosByRangoFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // Conteo de ventas completadas del día actual
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estadoPedido = :estadoPedido AND p.fechaSolicitud BETWEEN :inicio AND :fin")
    long countByEstadoPedidoAndFechaSolicitudBetween(@Param("estadoPedido") String estadoPedido, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // Obtener el último código generado para poder crear el siguiente (correlativo)
    @Query("SELECT MAX(p.codigo) FROM Pedido p")
    String findLastCodigo();
}