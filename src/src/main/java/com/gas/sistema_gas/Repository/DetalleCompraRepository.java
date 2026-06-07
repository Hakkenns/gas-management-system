package com.gas.sistema_gas.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gas.sistema_gas.Model.DetalleCompra;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    
    // Al usar "findByCompraId", Spring Data JPA busca automáticamente por el campo "id" dentro del objeto "compra"
    List<DetalleCompra> findByCompraId(Long idCompra);
}