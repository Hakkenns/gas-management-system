package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findByEstadoOrderByCreatedAtDesc(String estado);
    long countByEstado(String estado);
    List<Incidencia> findByEmpleadoIdOrderByCreatedAtDesc(Long empleadoId);
}