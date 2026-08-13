package com.gas.sistema_gas.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.Caja;

import jakarta.persistence.LockModeType;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Caja c WHERE c.codigo = :codigo")
    Optional<Caja> findByCodigoForUpdate(@Param("codigo") String codigo);
}
