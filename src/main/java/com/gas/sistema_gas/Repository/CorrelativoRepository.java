package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Correlativo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorrelativoRepository extends JpaRepository<Correlativo, Integer> {

    /**
     * Busca un correlativo por tipo y serie sin bloquearlo.
     */
    Optional<Correlativo> findByTipoAndSerie(String tipo, String serie);

    /**
     * Busca un correlativo por tipo y serie con bloqueo para operaciones de incremento.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie")
    Optional<Correlativo> findWithLockByTipoAndSerie(@Param("tipo") String tipo, @Param("serie") String serie);

    /**
     * Verifica si existe un correlativo para un tipo y serie específico
     */
    boolean existsByTipoAndSerie(String tipo, String serie);

}
