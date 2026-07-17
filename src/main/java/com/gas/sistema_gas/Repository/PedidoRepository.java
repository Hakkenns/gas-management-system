package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar por el código único (ej: PED-0001)
    Optional<Pedido> findByCodigo(String codigo);

    // Listar pedidos por estado (PENDIENTE, ENTREGADO, CANCELADO)
    List<Pedido> findByEstadoPedido(String estadoPedido);

    // Listar pedidos de un cliente específico
    List<Pedido> findByClienteId(Long idCliente);

    // Listar pedidos por tipo de venta
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.metodoPago WHERE p.tipoVenta = :tipoVenta")
    List<Pedido> findByTipoVentaWithMetodoPago(@Param("tipoVenta") String tipoVenta);

    // Listar pedidos por tipo de venta y empleado (motorizado) - excluye completados y anulados
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.metodoPago WHERE p.tipoVenta = :tipoVenta AND p.empleado.id = :empleadoId AND p.estadoPedido NOT IN ('ENTREGADO', 'ANULADO')")
    List<Pedido> findByTipoVentaAndEmpleadoIdWithMetodoPago(@Param("tipoVenta") String tipoVenta, @Param("empleadoId") Long empleadoId);

    @Modifying
    @Query("UPDATE Pedido p SET p.estadoPago = :estadoPago, p.metodoPago = :metodoPago WHERE p.id = :id")
    int updateEstadoPagoAndMetodoById(@Param("id") Long id,
                                     @Param("estadoPago") String estadoPago,
                                     @Param("metodoPago") MetodoPago metodoPago);

    // Buscar un pedido por ID y motorizado asignado
    @Query("SELECT p FROM Pedido p WHERE p.id = :id AND p.empleado.id = :empleadoId")
    Optional<Pedido> findByIdAndEmpleadoId(@Param("id") Long id, @Param("empleadoId") Long empleadoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);

    // Reporte de ventas entre fechas (Muy útil para el cierre de caja)
    @Query("SELECT p FROM Pedido p WHERE p.fechaSolicitud BETWEEN :inicio AND :fin")
    List<Pedido> findPedidosByRangoFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // Conteo de ventas completadas del día actual
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estadoPedido = :estadoPedido AND p.fechaSolicitud BETWEEN :inicio AND :fin")
    long countByEstadoPedidoAndFechaSolicitudBetween(@Param("estadoPedido") String estadoPedido, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // Contar pedidos del día para un motorizado usando la fecha de entrega real o la fecha de solicitud si aplica
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.empleado.id = :empleadoId AND p.estadoPedido = 'ENTREGADO' AND COALESCE(p.fechaEntrega, p.fechaSolicitud) >= :inicio AND COALESCE(p.fechaEntrega, p.fechaSolicitud) < :fin")
    long countPedidosHoyPorEmpleado(@Param("empleadoId") Long empleadoId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fin") LocalDateTime fin);

    // Contar entregas completas históricas para un motorizado
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.empleado.id = :empleadoId AND p.estadoPedido = :estadoPedido")
    long countEntregasPorEmpleadoYEstado(@Param("empleadoId") Long empleadoId,
                                         @Param("estadoPedido") String estadoPedido);

    // Obtener el último código generado para poder crear el siguiente (correlativo)
    @Query("SELECT MAX(p.codigo) FROM Pedido p")
    String findLastCodigo();

    // Listar pedidos entregados por un empleado/repartidor específico
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.metodoPago WHERE p.empleado.id = :empleadoId AND p.estadoPedido = 'ENTREGADO'")
    List<Pedido> findEntregadosByEmpleadoId(@Param("empleadoId") Long empleadoId);

    @Query("SELECT p FROM Pedido p WHERE p.empleado.id = :idEmpleado AND p.estadoPedido = 'ENTREGADO'")
    List<Pedido> findHistorialBase(@Param("idEmpleado") Long idEmpleado);

    // Buscar pedidos entregados con filtros dinámicos y paginación
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.metodoPago WHERE p.empleado.id = :empleadoId AND p.estadoPedido = 'ENTREGADO' " +
           "AND (:buscar IS NULL OR p.codigo LIKE %:buscar% OR p.cliente.nombre LIKE %:buscar%) " +
           "AND (:metodoPago IS NULL OR LOWER(p.metodoPago.nombre) = LOWER(:metodoPago)) " +
           "AND (:fechaInicio IS NULL OR p.fechaSolicitud >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaSolicitud <= :fechaFin)")
    Page<Pedido> findEntregadosByEmpleadoIdWithFilters(
        @Param("empleadoId") Long empleadoId,
        @Param("buscar") String buscar,
        @Param("metodoPago") String metodoPago,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        Pageable pageable
    );
}