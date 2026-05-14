package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.CategoriaMapper;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Repository.CategoriaRepository;
import com.gas.sistema_gas.dto.CategoriaDTO;
import com.gas.sistema_gas.service.CategoriaService;

import jakarta.transaction.Transactional;

public class CategoriaServiceImplement implements CategoriaService{
    
    @Autowired
    private CategoriaMapper categoriaMapper;
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public List<CategoriaDTO.SimpleResponse> listAll(){
        return  categoriaRepository.findAll().stream()
                .filter(c -> c.getEstado() == 1)
                .map(categoriaMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoriaDTO.SimpleResponse createCategory(CategoriaDTO.Create createDto){


        // Verificar que el nombre de la categorai no exista
        if(categoriaRepository.existsByNombre(createDto.nombre())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombde de categoria ya existe");
        }

        Categoria categoria = categoriaMapper.toEntity(createDto);

        return categoriaMapper.toSimpleResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaDTO.SimpleResponse updateCategory(Long id, CategoriaDTO.Update updateDto){

        // Verificamos que la categoria exista
        Categoria categoria = categoriaRepository.findById(id)
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        
        // modificamos los datos de categoria
        categoria.setNombre(updateDto.nombre());
        categoria.setDescripcion(updateDto.descripcion());

        return categoriaMapper.toSimpleResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id){

        // Verificamos si la categoria existe
        Categoria categoria = categoriaRepository.findById(id)
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        
        // Eliminaciión lógica: Cambiar estado
        categoria.setEstado(0);

        // guardamos los cambios
        categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public CategoriaDTO.SimpleResponse findById(Long id){
        return  categoriaRepository.findById(id)
                .map(categoriaMapper::toSimpleResponse)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
    }
}
