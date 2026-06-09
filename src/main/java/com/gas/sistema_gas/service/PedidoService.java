package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.PedidoDTO;

public interface PedidoService {
    
    List<PedidoDTO.SimpleResponse> listAll();
    PedidoDTO.SimpleResponse createOrder(PedidoDTO.Create createDto, Long idUsuarioLogueado); 
    PedidoDTO.SimpleResponse updateOrder(Long id, PedidoDTO.Update updateDto); 
    void deleteOrder(Long id);
    PedidoDTO.SimpleResponse findById(Long id);
    PedidoDTO.EditResponse getEditData(Long id);
}