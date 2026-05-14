package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.dto.CategoriaDTO;

public interface CategoriaService {

    List<CategoriaDTO.SimpleResponse> listAll();
    CategoriaDTO.SimpleResponse createCategory(CategoriaDTO.Create createDto);
    CategoriaDTO.SimpleResponse updateCategory(Long id, CategoriaDTO.Update updateDto);
    void deleteCategory(Long id);
    CategoriaDTO.SimpleResponse findById(Long id);
}