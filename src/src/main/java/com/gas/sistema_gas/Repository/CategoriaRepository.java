package com.gas.sistema_gas.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    boolean existsByNombre(String nombre);
}