package com.gas.sistema_gas.service;

import java.util.List;
import com.gas.sistema_gas.Model.Envase;
import com.gas.sistema_gas.dto.EnvioEnvaseDTO;

public interface EnvaseService {
    List<EnvioEnvaseDTO.DeudorResponse> listarDeudoresPendientes();
    void registrarDevolucion(EnvioEnvaseDTO.DevolucionRequest request);
    List<Envase> listarTodos();
    Envase obtenerPorId(Long id);
    Envase guardar(Envase envase);
    void eliminar(Long id);
}
