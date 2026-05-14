package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final OpcionRepository opcionRepository; // Para validar los permisos

    @Override
    @Transactional
    public PerfilDTO.Response create(PerfilDTO.Create dto) {
        if (perfilRepository.existsByNombrePerfil(dto.nombrePerfil())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfil ya existe");
        }

        Perfil perfil = perfilMapper.toEntity(dto);
        
        // Cargar las opciones (permisos) seleccionadas
        if (dto.idOpciones() != null && !dto.idOpciones().isEmpty()) {
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
    public PerfilDTO.Response findById(Long id) {
        return perfilRepository.findById(id)
                .map(perfilMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
        perfil.setEstado(0);
        perfilRepository.save(perfil);
    }
}