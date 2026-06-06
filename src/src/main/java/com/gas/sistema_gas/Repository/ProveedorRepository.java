package com.gas.sistema_gas.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gas.sistema_gas.Model.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long>{
    Optional<Proveedor> findByRuc(String ruc);
    boolean existsByRuc(String ruc);
}
