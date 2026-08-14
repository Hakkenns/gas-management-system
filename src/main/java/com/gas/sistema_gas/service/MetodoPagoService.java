package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.MetodoPagoDTO;
import com.gas.sistema_gas.Model.MetodoPago;

public interface MetodoPagoService {
    List<MetodoPagoDTO.Response> listActive();
    MetodoPagoDTO.Response create(MetodoPagoDTO.Create dto);
    MetodoPago obtenerActivo(Long idMetodo);
    String validarYNormalizarNumeroOperacion(MetodoPago metodo, String numOperacion);
}
