package com.gas.sistema_gas.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.Caja;
import com.gas.sistema_gas.Model.EstadoSesionCaja;
import com.gas.sistema_gas.Model.SesionCaja;

import jakarta.persistence.LockModeType;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {

    Optional<SesionCaja> findByCajaAndEstado(Caja caja, EstadoSesionCaja estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SesionCaja s WHERE s.caja = :caja AND s.estado = :estado")
    Optional<SesionCaja> findByCajaAndEstadoForUpdate(@Param("caja") Caja caja,
            @Param("estado") EstadoSesionCaja estado);

    long countByCajaAndEstado(Caja caja, EstadoSesionCaja estado);
}
