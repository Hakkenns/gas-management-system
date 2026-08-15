package com.gas.sistema_gas.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.Empleado;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Empleado e WHERE e.id = :id")
    Optional<Empleado> findByIdForUpdate(@Param("id") Long id);

    // metodo abstracto de retorno boolean para verificar la existencia de un DNI
    boolean existsByDni(String dni);

    // metodo abstracto de retorno boolean para verificar la existencia de una placa registrada
    boolean existsByTelefono(String telefono);

    // Metodo abstracto creado para listar empleados por estado = 1
    List<Empleado> findByEstado(Integer estado);

    // Metodo creado para buscar empleados con un estado activo
    Optional<Empleado> findByIdAndEstado(Long id, Integer estado);

    @Query(value = "SELECT e.* FROM empleados e WHERE e.estado = 1 AND e.id_empleado NOT IN (SELECT u.id_empleado FROM usuarios u WHERE u.id_empleado IS NOT NULL)", nativeQuery = true)
    List<Empleado> findEmpleadosSinUsuario();

    @Query("SELECT e FROM Empleado e WHERE e.id NOT IN " +
            "(SELECT a.empleado.id FROM AsignacionMoto a WHERE a.estado = 'ACTIVA')")
    List<Empleado> findEmpleadosDisponibles();

}
