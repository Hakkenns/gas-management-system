package com.gas.sistema_gas.service;

import java.util.List;
import com.gas.sistema_gas.dto.CompraDTO;
import com.gas.sistema_gas.Model.DetalleCompra;

public interface CompraService {
    CompraDTO.SimpleResponse create(CompraDTO.Create dto, Long idUsuarioLogueado);
    List<CompraDTO.SimpleResponse> listAll();
    List<DetalleCompra> listDetallesByCompraId(Long idCompra);
    CompraDTO.SimpleResponse anularCompra(Long idCompra);
}