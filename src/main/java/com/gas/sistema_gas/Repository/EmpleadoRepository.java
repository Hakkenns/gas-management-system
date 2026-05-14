package com.gas.sistema_gas.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    boolean existsByDni(String dni);
    boolean existsByTelefono(String telefono);
}
