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
                .filter(categoria -> categoria.getEstado() != 2) // Excluimos estado = 2 (Eliminadas)
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

        configurarBanderasPorTipoUnidad(categoria, createDto.tipoUnidad());

        return categoriaMapper.toSimpleResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaDTO.SimpleResponse updateCategory(Long id, CategoriaDTO.Update updateDto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));

        // VALIDACIÓN MEJORADA: Permite guardar si es el mismo nombre de este registro,
        // pero bloquea si intenta usar el nombre de OTRA categoría existente.
        categoriaRepository.findByNombre(updateDto.nombre()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El nombre de la categoría ya está en uso por otro registro");
            }
        });

        categoria.setNombre(updateDto.nombre());
        categoria.setDescripcion(updateDto.descripcion());

        configurarBanderasPorTipoUnidad(categoria, updateDto.tipoUnidad());

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

    // =========================================================================
    // MÉTODO PRIVADO AUXILIAR: Centraliza la lógica del Switch para reutilizarla
    // =========================================================================
    private void configurarBanderasPorTipoUnidad(Categoria categoria, String tipoUnidad) {
        if (tipoUnidad == null) {
            tipoUnidad = "POR UNIDADES / PIEZAS";
        }

        switch (tipoUnidad.toUpperCase().trim()) {
            case "POR KILOS (KG)":
                categoria.setUnidadMedida("KG");
                categoria.setRequiereCapacidad(true);
                categoria.setEtiquetaCapacidad("Capacidad (kg)");
                categoria.setManejaEnvase(true);
                break;

            case "POR LITROS (L)":
                categoria.setUnidadMedida("L");
                categoria.setRequiereCapacidad(true);
                categoria.setEtiquetaCapacidad("Contenido (Litros)");
                categoria.setManejaEnvase(true);
                break;

            case "POR METROS (M)":
                categoria.setUnidadMedida("M");
                categoria.setRequiereCapacidad(true);
                categoria.setEtiquetaCapacidad("Longitud (Metros)");
                categoria.setManejaEnvase(false);
                break;

            case "POR UNIDADES / PIEZAS":
            default:
                categoria.setUnidadMedida("UND");
                categoria.setRequiereCapacidad(false);
                categoria.setEtiquetaCapacidad(null);
                categoria.setManejaEnvase(false);
                break;
        }
    }
}
