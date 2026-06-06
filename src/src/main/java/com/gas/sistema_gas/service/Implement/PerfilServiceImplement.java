package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.PerfilMapper;
import com.gas.sistema_gas.Model.Opcion;
import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.Repository.OpcionRepository;
import com.gas.sistema_gas.Repository.PerfilRepository;
import com.gas.sistema_gas.dto.PerfilDTO;
import com.gas.sistema_gas.service.PerfilService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PerfilServiceImplement implements PerfilService {

    private final PerfilRepository perfilRepository;
    private final PerfilMapper perfilMapper;
    private final OpcionRepository opcionRepository;

    @Override
    @Transactional
    public PerfilDTO.Response create(PerfilDTO.Create dto) {
        if (perfilRepository.existsByNombrePerfil(dto.nombrePerfil())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfil ya existe");
        }

        Perfil perfil = perfilMapper.toEntity(dto);

        if (dto.idOpciones() != null && !dto.idOpciones().isEmpty()) {
            List<Opcion> opciones = opcionRepository.findAllById(dto.idOpciones());
            perfil.setOpciones(opciones);
        }

        return perfilMapper.toResponse(perfilRepository.save(perfil));
    }

    @Override
    @Transactional
    public PerfilDTO.Response update(Long id, PerfilDTO.Create dto) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        if (!perfil.getNombrePerfil().equals(dto.nombrePerfil())
                && perfilRepository.existsByNombrePerfil(dto.nombrePerfil())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfil ya existe");
        }

        perfil.setNombrePerfil(dto.nombrePerfil());
        perfil.setDescripcion(dto.descripcion());

        if (dto.idOpciones() != null) {
            List<Opcion> opciones = opcionRepository.findAllById(dto.idOpciones());
            perfil.setOpciones(opciones);
        }

        return perfilMapper.toResponse(perfilRepository.save(perfil));
    }

    @Override
    @Transactional
    public List<PerfilDTO.Response> listActive() {
        return perfilRepository.findByEstado(1).stream()
                .map(perfilMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PerfilDTO.Response> listNotDeleted() {
        return perfilRepository.findByEstadoNot(2).stream()
                .map(perfilMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PerfilDTO.Response findById(Long id) {
        return perfilRepository.findById(id)
                .map(perfilMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    }

    @Override
    @Transactional
    public PerfilDTO.Response setState(Long id, Integer estado) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        if (perfil.getId() != null && perfil.getId().equals(1L) && !estado.equals(1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El administrador no puede ser inactivado o eliminado");
        }

        perfil.setEstado(estado);
        return perfilMapper.toResponse(perfilRepository.save(perfil));
    }

    @Override
    @Transactional
    public PerfilDTO.Response assignOptions(Long id, List<Long> idOpciones) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        if (idOpciones == null || idOpciones.isEmpty()) {
            perfil.setOpciones(List.of());
        } else {
            perfil.setOpciones(opcionRepository.findAllById(idOpciones));
        }

        return perfilMapper.toResponse(perfilRepository.save(perfil));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        if (perfil.getId() != null && perfil.getId().equals(1L)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El administrador no puede ser eliminado");
        }

        perfil.setEstado(2);
        perfilRepository.save(perfil);
    }
}