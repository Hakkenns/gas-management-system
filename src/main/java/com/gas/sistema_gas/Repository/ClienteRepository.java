package com.gas.sistema_gas.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{
    
    // Buscar por teléfono es el uso más común al recibir una llamada
    Optional<Cliente> findByTelefono(String telefono);
    // Buscar por DNI para comprobantes
    Optional<Cliente> findByDni(String dni);
    boolean existsByDni(String dni);
    boolean existsByTelefono(String telefono);
    List<Cliente> findByEstado(Integer estado);
    Optional<Cliente> findByIdAndEstado(Long id, Integer estado);

}
