package com.gas.sistema_gas.service.Implement;

import com.gas.sistema_gas.Repository.PerfilRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.UsuarioMapper;
import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.UsuarioDTO;
import com.gas.sistema_gas.service.UsuarioService;

@Service
public class UsuarioServiceImplement implements UsuarioService {

    @Autowired
    private UsuarioMapper usuarioMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO.SimpleResponse> listAll() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado() == 1) // Filtrar a todos los usuarios con estado 1
                .map(usuarioMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioDTO.SimpleResponse createUser(UsuarioDTO.Create createDto) {

        // Verificación de correo (Usando el DTO)
        if (usuarioRepository.existsByCorreo(createDto.correo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya existe");
        }

        if (usuarioRepository.existsByUserName(createDto.userName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }

        // Convetimos el DTO a Entidad Usuario
        Usuario usuario = usuarioMapper.toEntity(createDto);

        // 🔐 ENCRIPTAR CONTRASEÑA CON BCRYPT
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // Buscamos el Perfil real en la BD usando el ID del formulaopcionRepository
        Perfil perfil = perfilRepository.findById(createDto.idPerfil()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        // Vinculamos el usuario con su perfil correspondiente
        usuario.setPerfil(perfil);

        // PERSITENCIA: Guardamos en la base de datos y dentro de la variable
        // usuarioGuardado para el Mapper
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // RESPONSE: Convetirmos la entidad guardada a DTO SimpleResponse de respuesta
        return usuarioMapper.toSimpleResponse(usuarioGuardado);
    }

    @Override
    @Transactional
    public UsuarioDTO.SimpleResponse updateUser(Long id, UsuarioDTO.Update updateDto) {
        // 1. Buscamos al usuario actual
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Verifica si el correo que se envia es diferente al que ya tenia
        // verifica si ese correo es nuevo buscandolo en la base de datos
        if (!usuario.getCorreo().equalsIgnoreCase(updateDto.correo())) {
            if (usuarioRepository.existsByCorreo(updateDto.correo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
            }
        }

        if(!usuario.getUserName().equalsIgnoreCase(updateDto.userName())){
            if(usuarioRepository.existsByUserName(updateDto.userName())){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya esta registrado");
            }
        }

        // 3. Actualizamos el perfil si es necesario
        Perfil perfil = perfilRepository.findById(updateDto.idPerfil())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        // 2. Actualizamos los datos (sin cambiar la fecha de creación)
        
        usuario.setUserName(updateDto.userName());
        usuario.setCorreo(updateDto.correo());
        usuario.setPerfil(perfil);

        // 🔐 Si mandas contraseña nueva, encriptarla con BCrypt
        if (updateDto.password() != null && !updateDto.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(updateDto.password()));
        }

        usuario.setPerfil(perfil);

        return usuarioMapper.toSimpleResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {

        // 1. Buscamos al usuario actual
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Borrado lógico (cambiamos el estado)
        usuario.setEstado(0);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO.SimpleResponse findById(Long id) {
        return usuarioRepository.findById(id)
                .map(usuarioMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

}
