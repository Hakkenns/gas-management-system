package com.gas.sistema_gas.Repository;

import com.gas.sistema_gas.Model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    List<Perfil> findByEstado(Integer estado);
    List<Perfil> findByEstadoNot(Integer estado);
    boolean existsByNombrePerfil(String nombrePerfil);
}