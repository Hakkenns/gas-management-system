package com.gas.sistema_gas.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gas.sistema_gas.dto.PedidoDTO;

public interface PedidoService {
    
    List<PedidoDTO.SimpleResponse> listAll();
    List<PedidoDTO.SimpleResponse> listByTipoVenta(String tipoVenta);
    List<PedidoDTO.SimpleResponse> listByTipoVentaAndEmpleadoId(String tipoVenta, Long empleadoId);
    PedidoDTO.SimpleResponse createOrder(PedidoDTO.Create createDto, Long idUsuarioLogueado); 
    PedidoDTO.SimpleResponse updateOrder(Long id, PedidoDTO.Update updateDto); 
    PedidoDTO.SimpleResponse updateEstadoPedido(Long id, String nuevoEstado);
    void deleteOrder(Long id);
    PedidoDTO.SimpleResponse findById(Long id);
    PedidoDTO.SimpleResponse findByIdAndEmpleadoId(Long id, Long empleadoId);
    boolean existsByIdAndEmpleadoId(Long id, Long empleadoId);
    PedidoDTO.EditResponse getEditData(Long id);
    long countSalesToday();
    List<PedidoDTO.SimpleResponse> listEntregadosByEmpleadoId(Long empleadoId);
    Page<PedidoDTO.SimpleResponse> listEntregadosByEmpleadoIdWithFilters(Long empleadoId, String buscar, String metodoPago, java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin, org.springframework.data.domain.Pageable pageable);
}