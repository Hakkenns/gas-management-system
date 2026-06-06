package com.gas.sistema_gas.service;

import com.gas.sistema_gas.dto.MotoDTO;

import java.util.List;

public interface MotoService {
    List<MotoDTO.SimpleResponse> listMoto();
    MotoDTO.SimpleResponse createMoto(MotoDTO.Create createDto);
    MotoDTO.SimpleResponse updateMoto(Long id, MotoDTO.Update updateDto);
    void deleteMoto(Long id);
    MotoDTO.SimpleResponse findById(Long id);
}
