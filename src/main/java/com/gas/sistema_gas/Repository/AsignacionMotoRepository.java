package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.AsignacionMoto;
import com.gas.sistema_gas.Model.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionMotoRepository extends JpaRepository<AsignacionMoto, Long> {

    // Listar todas las asignaciones ordenadas por las más recientes
    List<AsignacionMoto> findAllByOrderByCreatedAtDesc();

    // Verificar si un empleado ya tiene una moto asignada activa
    Optional<AsignacionMoto> findByEmpleadoIdAndEstado(Long empleadoId, AsignacionMoto.EstadoAsignacion estado);

    // Verificar si una moto ya está siendo usada de forma activa
    Optional<AsignacionMoto> findByMotoIdAndEstado(Long motoId, AsignacionMoto.EstadoAsignacion estado);

}