package com.gas.sistema_gas.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.Opcion;

public interface OpcionRepository extends JpaRepository<Opcion, Long>{

    List<Opcion> findByEstado(Integer estado);
    List<Opcion> findByPadreIsNullAndEstado(Integer estado);
    

}