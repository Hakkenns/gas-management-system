package com.gas.sistema_gas.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gas.sistema_gas.Model.Rubro;
import java.util.List;

@Repository
public interface RubroRepository extends JpaRepository<Rubro, Long> {
    
    // Este método lo usa el Service para traer solo los rubros activos (estado = 1)
    List<Rubro> findByEstado(Integer estado);
}