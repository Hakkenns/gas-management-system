package com.gas.sistema_gas.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gas.sistema_gas.Model.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {
}