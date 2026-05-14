package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.OpcionDTO;

public interface OpcionService {

    List<OpcionDTO.SimpleResponse> listAll();
    OpcionDTO.SimpleResponse createOpcion(OpcionDTO.Create createDto);
    OpcionDTO.SimpleResponse updateOpcion(Long id, OpcionDTO.Update updateDto);
    void deleteOpcion(Long id);
    OpcionDTO.SimpleResponse findById(Long id);
    
}