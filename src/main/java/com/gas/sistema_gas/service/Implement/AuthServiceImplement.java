package com.gas.sistema_gas.service.Implement;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    // Mapa en memoria para llevar el control de intentos fallidos y bloqueo temporal
    private final ConcurrentMap<String, FailedLogin> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutos

    private static class FailedLogin {
        final AtomicInteger attempts = new AtomicInteger(0);
        volatile long lockUntil = 0L;
    }

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
        // Validar bloqueo temporal (si aplica)
        long now = System.currentTimeMillis();
        FailedLogin fl = loginAttempts.get(usuario.getUserName());
        if (fl != null && fl.lockUntil > now) {
            long remainingMs = fl.lockUntil - now;
            long minutes = remainingMs / 60000;
            long seconds = (remainingMs % 60000) / 1000;
            throw new ResponseStatusException(
                HttpStatus.LOCKED,
                String.format("Cuenta bloqueada por %d minutos %d segundos", minutes, seconds)
            );
        }

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
            // Registrar intento fallido
            FailedLogin entry = loginAttempts.computeIfAbsent(usuario.getUserName(), k -> new FailedLogin());
            int attempts = entry.attempts.incrementAndGet();
            if (attempts >= MAX_ATTEMPTS) {
                entry.lockUntil = now + LOCK_DURATION_MS;
                entry.attempts.set(0);
                throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Cuenta bloqueada por 15 minutos. Intente más tarde."
                );
            }

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, 
                "Usuario o contraseña incorrectos"
            );
        }

        // Login exitoso: limpiar contador de intentos y devolver DTO
        loginAttempts.remove(usuario.getUserName());
        return usuarioMapper.toLoginResponse(usuario);
    }

    @Override
    public Optional<LoginDTO.Response> findByUsername(String username) {
        return usuarioRepository.findByUserName(username)
            .map(usuarioMapper::toLoginResponse);
    }
}
