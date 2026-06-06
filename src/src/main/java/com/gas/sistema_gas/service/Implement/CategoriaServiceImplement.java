package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.CategoriaMapper;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Repository.CategoriaRepository;
import com.gas.sistema_gas.dto.CategoriaDTO;
import com.gas.sistema_gas.service.CategoriaService;

import jakarta.transaction.Transactional;

@Service
public class CategoriaServiceImplement implements CategoriaService {
    
    @Autowired
    private CategoriaMapper categoriaMapper;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public List<CategoriaDTO.SimpleResponse> listAll() {
        return categoriaRepository.findAll().stream()
                .filter(categoria -> categoria.getEstado() != 2) // Excluimos las categorías con estado = 2 (Eliminadas)
                .map(categoriaMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoriaDTO.SimpleResponse createCategory(CategoriaDTO.Create createDto) {
        if (categoriaRepository.existsByNombre(createDto.nombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya existe");
        }

        Categoria categoria = categoriaMapper.toEntity(createDto);
        return categoriaMapper.toSimpleResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaDTO.SimpleResponse updateCategory(Long id, CategoriaDTO.Update updateDto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));

        categoria.setNombre(updateDto.nombre());
        categoria.setDescripcion(updateDto.descripcion());

        return categoriaMapper.toSimpleResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void setState(Long id, Integer estado) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));

        categoria.setEstado(estado);
        categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));

        categoria.setEstado(2);
        categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public CategoriaDTO.SimpleResponse findById(Long id) {
        return categoriaRepository.findById(id)
                .map(categoriaMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
    }
}

