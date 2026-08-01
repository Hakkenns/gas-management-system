package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findByEstadoOrderByCreatedAtDesc(String estado);
    long countByEstado(String estado);
    List<Incidencia> findByEmpleadoIdOrderByCreatedAtDesc(Long empleadoId);
    List<Incidencia> findByPedidoId(Long pedidoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Incidencia i WHERE i.id = :id")
    Optional<Incidencia> findByIdForUpdate(@Param("id") Long id);
}
