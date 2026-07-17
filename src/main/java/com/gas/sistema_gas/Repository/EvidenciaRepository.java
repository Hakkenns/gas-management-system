package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.function.Predicate;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {
    List<Evidencia> findByPedidoPago_Id(Long idPago);
}
