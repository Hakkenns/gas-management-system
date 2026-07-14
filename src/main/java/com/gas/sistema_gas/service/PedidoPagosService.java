package com.gas.sistema_gas.service;

import org.springframework.web.multipart.MultipartFile;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;
import com.gas.sistema_gas.Model.PedidoPago;

public interface PedidoPagosService {
    PedidoPago registrarPagoYape(PedidoPagoYapeDTO dto, MultipartFile evidencia);
}
