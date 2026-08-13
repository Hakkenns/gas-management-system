package com.gas.sistema_gas.Repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.MovimientoCaja;
import com.gas.sistema_gas.Model.SentidoMovimiento;
import com.gas.sistema_gas.Model.SesionCaja;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

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
}
