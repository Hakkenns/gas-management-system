package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.ControlEnvase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ControlEnvaseRepository extends JpaRepository<ControlEnvase, Long> {
    
    List<ControlEnvase> findByPedido_Id(Long idPedido);

    @Modifying
    @Query("DELETE FROM ControlEnvase c WHERE c.pedido.id = :idPedido")
    void deleteByPedido_Id(Long idPedido);
}
