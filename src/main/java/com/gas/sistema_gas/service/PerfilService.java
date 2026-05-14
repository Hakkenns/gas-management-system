package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.dto.PerfilDTO;

public interface PerfilService {
    PerfilDTO.Response create(PerfilDTO.Create dto);
    List<PerfilDTO.Response> listActive();
    PerfilDTO.Response findById(Long id);
    void delete(Long id);
}