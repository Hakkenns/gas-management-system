package com.gas.sistema_gas.service;

import java.math.BigDecimal;
import java.util.List;

import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.dto.CajaDTO;

public interface CajaService {

    CajaDTO.AperturaResponse abrirCaja(Long usuarioId, BigDecimal montoInicial, String observaciones);

    CajaDTO.CierreResponse cerrarCaja(Long usuarioId, BigDecimal montoDeclarado, String observaciones);

    void registrarIngresosVentaLocal(List<PedidoPago> pagos, Long usuarioId);

    void registrarIngresosVentaDomicilio(List<PedidoPago> pagos, Long usuarioResponsableId);
}
