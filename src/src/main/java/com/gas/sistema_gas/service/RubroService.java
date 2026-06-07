package com.gas.sistema_gas.service;

import com.gas.sistema_gas.dto.RubroDTO;
import java.util.List;

public interface RubroService {
    List<RubroDTO.SimpleResponse> listAll();
    RubroDTO.SimpleResponse findById(Long id);
    void create(RubroDTO.Create createDto);
    void update(Long id, RubroDTO.Update updateDto);
    void delete(Long id);
    void setState(Long id, Integer estado);
}