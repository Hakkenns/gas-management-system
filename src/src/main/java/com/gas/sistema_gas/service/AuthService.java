package com.gas.sistema_gas.service;

import java.util.Optional;
import com.gas.sistema_gas.dto.LoginDTO;

public interface AuthService {
    /**
     * Autentica un usuario con sus credenciales
     * @param request con username y password
     * @return Response con datos del usuario si es correcto
     * @throws Exception si las credenciales son inválidas
     */
    LoginDTO.Response authenticate(LoginDTO.Request request);
    
    /**
     * Busca un usuario por username
     */
    Optional<LoginDTO.Response> findByUsername(String username);
}
