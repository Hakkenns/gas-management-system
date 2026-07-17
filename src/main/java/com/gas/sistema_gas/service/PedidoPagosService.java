package com.gas.sistema_gas.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;
import com.gas.sistema_gas.dto.ConfirmarEntregaMixtaDTO;
import com.gas.sistema_gas.Model.PedidoPago;

public interface PedidoPagosService {
    PedidoPago registrarPagoYape(PedidoPagoYapeDTO dto, MultipartFile evidencia);
    
    PedidoPago registrarPagoYape(PedidoPagoYapeDTO dto, MultipartFile evidencia, MultipartFile evidenciaVuelto);
    
    List<PedidoPago> registrarPagosMultiples(ConfirmarEntregaMixtaDTO dto, List<MultipartFile> evidencias);
    
    List<PedidoPago> registrarPagosMultiples(ConfirmarEntregaMixtaDTO dto, List<MultipartFile> evidencias, MultipartFile evidenciaVuelto);
    
    List<PedidoPago> findByPedidoId(Long idPedido);
}
