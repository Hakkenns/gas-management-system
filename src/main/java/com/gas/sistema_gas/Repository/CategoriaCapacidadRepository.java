package com.gas.sistema_gas.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.CategoriaCapacidad;

public interface CategoriaCapacidadRepository extends JpaRepository<CategoriaCapacidad, Long> {
    
    List<CategoriaCapacidad> findByCategoriaId(Long categoriaId);
    
    void deleteByCategoriaId(Long categoriaId);
}