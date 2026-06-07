package com.gas.sistema_gas.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gas.sistema_gas.Model.MetodoPago;


public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {
    List<MetodoPago> findByEstado(Integer estado);
}
