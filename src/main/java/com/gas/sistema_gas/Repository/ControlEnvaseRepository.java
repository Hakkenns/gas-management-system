package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.ControlEnvase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ControlEnvaseRepository extends JpaRepository<ControlEnvase, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ControlEnvase c WHERE c.id = :id")
    Optional<ControlEnvase> findByIdForUpdate(@Param("id") Long id);
    
    List<ControlEnvase> findByPedido_Id(Long idPedido);

    @Modifying
    @Query("DELETE FROM ControlEnvase c WHERE c.pedido.id = :idPedido")
    void deleteByPedido_Id(Long idPedido);

    // Deudores con envases pendientes (PRESTADO o PARCIAL)
    @Query("SELECT c FROM ControlEnvase c " +
           "WHERE c.estado IN ('PRESTADO', 'PARCIAL') " +
           "ORDER BY c.cliente.nombre ASC")
    List<ControlEnvase> findDeudoresPendientes();

    // Query para clientes que aún deben envases (estado PRESTADO o PARCIAL)
    @Query("SELECT c FROM ControlEnvase c " +
           "WHERE c.cliente.id = :idCliente " +
           "AND c.estado IN ('PRESTADO', 'PARCIAL') " +
           "ORDER BY c.createdAt ASC")
    List<ControlEnvase> findDeudasActivasByClienteId(@Param("idCliente") Long idCliente);
}
