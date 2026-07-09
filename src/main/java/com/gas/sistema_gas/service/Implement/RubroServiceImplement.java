package com.gas.sistema_gas.service.Implement;

import com.gas.sistema_gas.Model.Rubro;
import com.gas.sistema_gas.Mapper.RubroMapper;
import com.gas.sistema_gas.Repository.RubroRepository;
import com.gas.sistema_gas.dto.RubroDTO;
import com.gas.sistema_gas.service.RubroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RubroServiceImplement implements RubroService {

    @Autowired
    private RubroRepository repository;

    @Autowired
    private RubroMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<RubroDTO.SimpleResponse> listAll() {
        // Igual que en Categorías: traemos TODO sin filtros usando el findAll() limpio
        return repository.findAll().stream()
            .filter(rubro -> rubro.getEstado() != 2) 
            .map(mapper::toSimpleResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RubroDTO.SimpleResponse findById(Long id) {
        Rubro rubro = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rubro no encontrado con el ID: " + id));
        return mapper.toSimpleResponse(rubro);
    }

    @Override
    @Transactional
    public void create(RubroDTO.Create createDto) {
        Rubro rubro = mapper.toEntity(createDto);
        rubro.setEstado(1); // Por defecto entra activo
        repository.save(rubro);
    }

    @Override
    @Transactional
    public void update(Long id, RubroDTO.Update updateDto) {
        Rubro rubro = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rubro no encontrado con el ID: " + id));
        
        mapper.updateEntityFromDto(updateDto, rubro);
        repository.save(rubro);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Rubro rubro = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rubro no encontrado con el ID: " + id));
    
        rubro.setEstado(2);
        repository.save(rubro); // Lo guardamos actualizado
    }

    @Override
    @Transactional
    public void setState(Long id, Integer estado) {
        Rubro rubro = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rubro no encontrado con el ID: " + id));
        rubro.setEstado(estado);
        repository.save(rubro);
    }
}