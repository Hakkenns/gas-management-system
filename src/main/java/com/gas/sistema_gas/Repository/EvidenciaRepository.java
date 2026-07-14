package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {
    // Aquí Hibernate ya sabe hacer guardar, buscar y eliminar evidencias automáticamente
}
