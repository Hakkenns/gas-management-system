package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.RespuestaIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RespuestaIncidenciaRepository extends JpaRepository<RespuestaIncidencia, Long> {
    List<RespuestaIncidencia> findByIncidenciaIdOrderByCreatedAtDesc(Long incidenciaId);
    List<RespuestaIncidencia> findByIncidenciaEmpleadoIdOrderByCreatedAtDesc(Long empleadoId);
    // Solo respuestas NO leídas
    List<RespuestaIncidencia> findByIncidenciaEmpleadoIdAndLeidoFalseOrderByCreatedAtDesc(Long empleadoId);
    long countByIncidenciaEmpleadoId(Long empleadoId);
    long countByIncidenciaEmpleadoIdAndLeidoFalse(Long empleadoId);

    // Marcar todas las respuestas de un empleado como leídas
    @Modifying
    @Query("UPDATE RespuestaIncidencia r SET r.leido = true WHERE r.incidencia.empleado.id = :empleadoId AND r.leido = false")
    int marcarComoLeidasPorEmpleado(@Param("empleadoId") Long empleadoId);
}
