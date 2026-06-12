package com.gas.sistema_gas.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    boolean existsByNombre(String nombre);    
    Optional<Categoria> findByNombre(String nombre);
}