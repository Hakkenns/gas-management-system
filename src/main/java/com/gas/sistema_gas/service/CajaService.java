package com.gas.sistema_gas.service;

import java.math.BigDecimal;

import com.gas.sistema_gas.dto.CajaDTO;

public interface CajaService {

    CajaDTO.AperturaResponse abrirCaja(Long usuarioId, BigDecimal montoInicial, String observaciones);
}
