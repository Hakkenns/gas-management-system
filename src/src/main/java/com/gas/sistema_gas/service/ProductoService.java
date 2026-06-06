package com.gas.sistema_gas.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.gas.sistema_gas.dto.ProductoDTO;


public interface ProductoService {

    List<ProductoDTO.SimpleResponse> listAll();
    ProductoDTO.SimpleResponse createProduct(ProductoDTO.Create createDto, MultipartFile archivoImagen);
    ProductoDTO.SimpleResponse createProduct(ProductoDTO.Create createDto, MultipartFile archivoImagen, String imagenBase64);
    ProductoDTO.SimpleResponse updateProduct(Long id, ProductoDTO.Update updateDto, MultipartFile archivoImagen);
    ProductoDTO.SimpleResponse updateProduct(Long id, ProductoDTO.Update updateDto, MultipartFile archivoImagen, String imagenBase64, Boolean quitarImagen);
    void removeImage(Long id);
    ProductoDTO.SimpleResponse setState(Long id, Integer estado);
    void deleteProduct(Long id);
    ProductoDTO.SimpleResponse findById(Long id);
    
    // Util para listar en el dashboard una tabla listando prodcuto scon bajo stock
    List<ProductoDTO.SimpleResponse> listLowStock();
}