package com.gas.sistema_gas.service;


import java.util.List;

import com.gas.sistema_gas.dto.ClienteDTO;

public interface ClienteService{

    ClienteDTO.SimpleResponse createClient(ClienteDTO.Create createDto);
    ClienteDTO.SimpleResponse updateClient(Long id, ClienteDTO.Update updateDto);
    void deleteClient(Long id);
    ClienteDTO.SimpleResponse findById(Long id);
    List<ClienteDTO.SimpleResponse> listAll();
}