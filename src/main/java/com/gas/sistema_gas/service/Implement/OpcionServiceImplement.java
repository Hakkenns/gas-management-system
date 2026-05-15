package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.OpcionMapper;
import com.gas.sistema_gas.Model.Opcion;
import com.gas.sistema_gas.Repository.OpcionRepository;
import com.gas.sistema_gas.dto.OpcionDTO;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.transaction.Transactional;

@Service
public class OpcionServiceImplement implements OpcionService {

    @Autowired
    private OpcionRepository repository;
    
    @Autowired
    private OpcionMapper opcionMapper;

    @Override
    @Transactional
    public List<OpcionDTO.SimpleResponse> listAll() {
        return repository.findAll().stream()
                .filter(o -> o.getEstado() == 1)
                .map(opcionMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<OpcionDTO.SimpleResponse> listByPerfilId(Long perfilId) {
        if (perfilId == null) return listAll();

        // Si es administrador (perfil id == 1) mostramos todas las opciones activas
        if (perfilId.equals(1L)) {
            return repository.findAll().stream()
                .filter(o -> o.getEstado() == 1)
                .map(opcionMapper::toSimpleResponse)
                .collect(Collectors.toList());
        }

        // Mostrar solo opciones activas que estén asociadas al perfil (no incluir opciones globales sin perfiles)
        return repository.findAll().stream()
            .filter(o -> o.getEstado() == 1)
            .filter(o -> o.getPerfiles() != null && o.getPerfiles().stream().anyMatch(p -> p.getId().equals(perfilId)))
            .map(opcionMapper::toSimpleResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OpcionDTO.SimpleResponse createOpcion(OpcionDTO.Create createDto) {
        Opcion opcion = opcionMapper.toEntity(createDto);
        opcion.setEstado(1);
        return opcionMapper.toSimpleResponse(repository.save(opcion));
    }

    @Override
    @Transactional
    public OpcionDTO.SimpleResponse updateOpcion(Long id, OpcionDTO.Update updateDto) {
        Opcion opcion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La opción no existe"));
        
        opcionMapper.updateEntityFromDto(updateDto, opcion);
        return opcionMapper.toSimpleResponse(repository.save(opcion));
    }

    @Override
    @Transactional
    public void deleteOpcion(Long id) {
        Opcion opcion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La opción no existe"));
        
        // Eliminación lógica
        opcion.setEstado(0);
        repository.save(opcion);
    }

    @Override
    @Transactional
    public OpcionDTO.SimpleResponse findById(Long id) {
        return repository.findById(id)
                .map(opcionMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La opción no existe"));
    }

    
}