package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.ControlEnvase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ControlEnvaseRepository extends JpaRepository<ControlEnvase, Long> {
    
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