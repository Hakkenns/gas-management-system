package com.gas.sistema_gas.service.Implement;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.UsuarioMapper;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.LoginDTO;
import com.gas.sistema_gas.service.AuthService;

@Service
public class AuthServiceImplement implements AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private UsuarioMapper usuarioMapper;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Autentica un usuario validando username y password
     */
    @Override
    public LoginDTO.Response authenticate(LoginDTO.Request request) {
        
        // Buscar usuario por username
        Usuario usuario = usuarioRepository.findByUserName(request.username())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, 
                "Usuario o contraseña incorrectos"
            ));

        // Validar que el usuario esté activo
        if (usuario.getEstado() == 0) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "El usuario está desactivado"
            );
        }

        // Comparar contraseña (la que envía el usuario con la guardada en BD)
        // Las contraseñas nuevas serán encriptadas con BCrypt
        // Las viejas pueden estar en plano o encriptadas
        boolean passwordMatches = passwordEncoder.matches(request.password(), usuario.getPassword());
        
        // Fallback: si no coincide con BCrypt, comparar en plano (para usuarios existentes)
        if (!passwordMatches && usuario.getPassword().equals(request.password())) {
            passwordMatches = true;
            // Aquí podrías encriptar la contraseña si la quieres actualizar
        }

        if (!passwordMatches) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, 
                "Usuario o contraseña incorrectos"
            );
        }

        // Convertir a DTO Response para enviar al cliente
        return usuarioMapper.toLoginResponse(usuario);
    }

    @Override
    public Optional<LoginDTO.Response> findByUsername(String username) {
        return usuarioRepository.findByUserName(username)
            .map(usuarioMapper::toLoginResponse);
    }
}
