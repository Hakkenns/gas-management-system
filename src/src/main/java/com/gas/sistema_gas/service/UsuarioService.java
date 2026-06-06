package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.UsuarioDTO;

public interface UsuarioService {

    UsuarioDTO.SimpleResponse createUser(UsuarioDTO.Create createDto);
    List<UsuarioDTO.SimpleResponse> listAll();
    UsuarioDTO.SimpleResponse findById(Long id);
    UsuarioDTO.SimpleResponse updateUser(Long id, UsuarioDTO.Update createDto);
    UsuarioDTO.SimpleResponse setState(Long id, Integer estado);
    void deleteUser(Long id);
    
}
