package com.gas.sistema_gas.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gas.sistema_gas.Model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    
    Optional<Usuario> findByUserName(String userName);

    boolean existsByCorreo(String correo);

    boolean existsByUserName(String userName);
}