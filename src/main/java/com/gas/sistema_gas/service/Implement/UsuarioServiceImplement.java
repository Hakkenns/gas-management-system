package com.gas.sistema_gas.service.Implement;

import com.gas.sistema_gas.Repository.PerfilRepository;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Model.Empleado;

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

    @Autowired
    private EmpleadoRepository empleadoRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO.SimpleResponse> listAll() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado() != 2) // Mostrar activos e inactivos, pero no eliminados
                .map(usuarioMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioDTO.SimpleResponse createUser(UsuarioDTO.Create createDto) {

        if (usuarioRepository.existsByCorreo(createDto.correo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya existe");
        }

        if (usuarioRepository.existsByUserName(createDto.userName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }

        // 1. Buscar y validar que el empleado exista en la base de datos
        Empleado empleado = empleadoRepository.findById(createDto.idEmpleado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado seleccionado no encontrado"));

        Usuario usuario = usuarioMapper.toEntity(createDto);

        // 2. Asociar el objeto Empleado real a la relación del Usuario
        usuario.setEmpleado(empleado);

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        Perfil perfil = perfilRepository.findById(createDto.idPerfil()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        usuario.setPerfil(perfil);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return usuarioMapper.toSimpleResponse(usuarioGuardado);
    }

    @Override
    @Transactional
    public UsuarioDTO.SimpleResponse updateUser(Long id, UsuarioDTO.Update updateDto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!usuario.getCorreo().equalsIgnoreCase(updateDto.correo())) {
            if (usuarioRepository.existsByCorreo(updateDto.correo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
            }
        }

        if (!usuario.getUserName().equalsIgnoreCase(updateDto.userName())) {
            if (usuarioRepository.existsByUserName(updateDto.userName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está registrado");
            }
        }

        Perfil perfil = perfilRepository.findById(updateDto.idPerfil())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        // NOTA: Se removió cualquier mutación del nombre aquí para mantener la persistencia limpia y normalizada.
        usuario.setUserName(updateDto.userName());
        usuario.setCorreo(updateDto.correo());
        usuario.setPerfil(perfil);

        if (updateDto.password() != null && !updateDto.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(updateDto.password()));
        }

        return usuarioMapper.toSimpleResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioDTO.SimpleResponse setState(Long id, Integer estado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setEstado(estado);
        return usuarioMapper.toSimpleResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setEstado(2);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO.SimpleResponse findById(Long id) {
        return usuarioRepository.findById(id)
                .map(usuarioMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO.PerfilResponse obtenerPerfilMotorizado(Long usuarioId) {
        Usuario usuario = usuarioRepository.findByIdAndEstado(usuarioId, 1)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getEmpleado() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado");
        }

        return usuarioMapper.toPerfilResponse(usuario);
    }
}
