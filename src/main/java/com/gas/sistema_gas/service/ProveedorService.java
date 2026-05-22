package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.ProveedorDTO;

public interface ProveedorService {
    ProveedorDTO.SimpleResponse create(ProveedorDTO.Create dto);
    ProveedorDTO.SimpleResponse update(Long id, ProveedorDTO.Update updateDto);
    ProveedorDTO.SimpleResponse setState(Long id, Integer estado);
    ProveedorDTO.SimpleResponse findById(Long id);
    List<ProveedorDTO.SimpleResponse> listAll();
    void delete(Long id);
}
