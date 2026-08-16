package com.gas.sistema_gas.Repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.MovimientoCaja;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.SentidoMovimiento;
import com.gas.sistema_gas.Model.SesionCaja;
import com.gas.sistema_gas.dto.CajaDTO;

import jakarta.persistence.LockModeType;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m FROM MovimientoCaja m
            WHERE m.pedidoPago = :pedidoPago
            """)
    Optional<MovimientoCaja> findByPedidoPagoForUpdate(@Param("pedidoPago") PedidoPago pedidoPago);

    Optional<MovimientoCaja> findByPedidoPago(PedidoPago pedidoPago);

    @Query("""
            SELECT COALESCE(SUM(
                CASE
                    WHEN m.sentido = :ingreso THEN m.monto
                    WHEN m.sentido = :egreso THEN -m.monto
                    ELSE 0
                END
            ), 0)
            FROM MovimientoCaja m
            WHERE m.sesionCaja = :sesionCaja
              AND m.canalFondos = :canalFondos
            """)
    BigDecimal calcularSaldoPorSesionYCanal(@Param("sesionCaja") SesionCaja sesionCaja,
            @Param("canalFondos") CanalFondos canalFondos,
            @Param("ingreso") SentidoMovimiento ingreso,
            @Param("egreso") SentidoMovimiento egreso);

    @Query("""
            SELECT COALESCE(SUM(
                CASE
                    WHEN m.sentido = :ingreso THEN m.monto
                    WHEN m.sentido = :egreso THEN -m.monto
                    ELSE 0
                END
            ), 0)
            FROM MovimientoCaja m
            WHERE m.empleadoCustodio = :empleado
              AND m.canalFondos = :canalFondos
            """)
    BigDecimal calcularSaldoCustodiaPorEmpleado(@Param("empleado") Empleado empleado,
            @Param("canalFondos") CanalFondos canalFondos,
            @Param("ingreso") SentidoMovimiento ingreso,
            @Param("egreso") SentidoMovimiento egreso);

    @Query("""
            SELECT new com.gas.sistema_gas.dto.CajaDTO$CustodiaPendienteResponse(
                e.id,
                e.nombre,
                COALESCE(SUM(
                    CASE
                        WHEN m.sentido = :ingreso THEN m.monto
                        WHEN m.sentido = :egreso THEN -m.monto
                        ELSE 0
                    END
                ), 0),
                e.estado
            )
            FROM MovimientoCaja m
            JOIN m.empleadoCustodio e
            WHERE m.canalFondos = :canalFondos
            GROUP BY e.id, e.nombre, e.estado
            HAVING COALESCE(SUM(
                CASE
                    WHEN m.sentido = :ingreso THEN m.monto
                    WHEN m.sentido = :egreso THEN -m.monto
                    ELSE 0
                END
            ), 0) > 0
            ORDER BY e.nombre ASC, e.id ASC
            """)
    java.util.List<CajaDTO.CustodiaPendienteResponse> listarCustodiasPendientes(
            @Param("canalFondos") CanalFondos canalFondos,
            @Param("ingreso") SentidoMovimiento ingreso,
            @Param("egreso") SentidoMovimiento egreso);
}
