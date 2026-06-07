package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gas.sistema_gas.Mapper.MetodoPagoMapper;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.dto.MetodoPagoDTO;
import com.gas.sistema_gas.service.MetodoPagoService;
import com.gas.sistema_gas.Model.MetodoPago;

import jakarta.transaction.Transactional;

@Service
public class MetodoPagoServiceImplement implements MetodoPagoService {
    @Autowired
    private  MetodoPagoRepository repository;
    @Autowired
    private  MetodoPagoMapper mapper;

    @Override
    @Transactional
    public List<MetodoPagoDTO.Response> listActive() {
        return repository.findByEstado(1).stream()
                .map(mapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MetodoPagoDTO.Response create(MetodoPagoDTO.Create dto) {
        MetodoPago metodo = mapper.toEntity(dto);
        metodo.setEstado(1); // Siempre activo al crear
        return mapper.toSimpleResponse(repository.save(metodo));
    }
}