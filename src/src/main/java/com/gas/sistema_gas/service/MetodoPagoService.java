package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.MetodoPagoDTO;

public interface MetodoPagoService {
    List<MetodoPagoDTO.Response> listActive();
    MetodoPagoDTO.Response create(MetodoPagoDTO.Create dto);
}
