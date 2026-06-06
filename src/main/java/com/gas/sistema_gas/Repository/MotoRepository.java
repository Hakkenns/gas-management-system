package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Moto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotoRepository extends JpaRepository<Moto, Long> {

    boolean existsByPlaca(String placa);
}
