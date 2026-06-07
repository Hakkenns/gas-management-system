package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.PerfilDTO;

public interface PerfilService {
    PerfilDTO.Response create(PerfilDTO.Create dto);
    List<PerfilDTO.Response> listActive();
    List<PerfilDTO.Response> listNotDeleted();
    PerfilDTO.Response findById(Long id);
    PerfilDTO.Response update(Long id, PerfilDTO.Create dto);
    PerfilDTO.Response setState(Long id, Integer estado);
    PerfilDTO.Response assignOptions(Long id, java.util.List<Long> idOpciones);
    void delete(Long id);
}