package com.gas.sistema_gas.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gas.sistema_gas.Model.Opcion;

public interface OpcionRepository extends JpaRepository<Opcion, Long>{

    List<Opcion> findByEstado(Integer estado);
    List<Opcion> findByPadreIsNullAndEstado(Integer estado);
    Optional<Opcion> findByRuta(String ruta);

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
            FROM Opcion o
            JOIN o.perfiles p
            WHERE o.ruta = :ruta
              AND o.estado = 1
              AND p.id = :perfilId
            """)
    boolean existsActiveByRutaAndPerfilId(@Param("ruta") String ruta, @Param("perfilId") Long perfilId);
    

}
