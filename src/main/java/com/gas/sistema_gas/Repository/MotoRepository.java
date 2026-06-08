package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Moto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MotoRepository extends JpaRepository<Moto, Long> {

    boolean existsByPlaca(String placa);
    @Query("SELECT m FROM Moto m WHERE m.estado = 1 AND m.id NOT IN " +
            "(SELECT a.moto.id FROM AsignacionMoto a WHERE a.estado = 'ACTIVA')")
    List<Moto> findMotosDisponibles();
}
