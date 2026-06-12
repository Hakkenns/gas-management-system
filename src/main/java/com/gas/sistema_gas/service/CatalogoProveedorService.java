package com.gas.sistema_gas.service;

import java.util.List;
import com.gas.sistema_gas.dto.CatalogoProveedorDTO;
import com.gas.sistema_gas.Model.Producto;

public interface CatalogoProveedorService {

    List<CatalogoProveedorDTO.SimpleResponse> listarTodo();
    
    CatalogoProveedorDTO.SimpleResponse asociarProducto(CatalogoProveedorDTO.Create createDto);
    
    void desasociarProducto(Long idProveedor, Long idProducto);
    
    List<Producto> listarProductosPorProveedor(Long idProveedor);
    
    CatalogoProveedorDTO.SimpleResponse buscarPorId(Long id);
}
