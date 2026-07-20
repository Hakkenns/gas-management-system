package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.CategoriaMapper;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Model.CategoriaCapacidad;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.CategoriaCapacidadRepository;
import com.gas.sistema_gas.Repository.CategoriaRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.CapacidadInfoDTO;
import com.gas.sistema_gas.dto.CategoriaDTO;
import com.gas.sistema_gas.service.CategoriaService;

import jakarta.transaction.Transactional;

@Service
public class CategoriaServiceImplement implements CategoriaService {

    @Autowired
    private CategoriaMapper categoriaMapper;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaCapacidadRepository categoriaCapacidadRepository;

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

        // Guardar capacidades asociadas
        if (createDto.capacidades() != null && !createDto.capacidades().isEmpty()) {
            List<CategoriaCapacidad> capacidades = createDto.capacidades().stream()
                    .map(valor -> {
                        CategoriaCapacidad cap = new CategoriaCapacidad();
                        cap.setCategoria(categoria);
                        cap.setValorCapacidad(BigDecimal.valueOf(valor));
                        return cap;
                    })
                    .collect(Collectors.toList());
            categoria.setCapacidades(capacidades);
        }

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

        // Actualizar capacidades: eliminar existentes y crear nuevas
        categoria.getCapacidades().clear();
        if (updateDto.capacidades() != null && !updateDto.capacidades().isEmpty()) {
            List<CategoriaCapacidad> nuevasCapacidades = updateDto.capacidades().stream()
                    .map(valor -> {
                        CategoriaCapacidad cap = new CategoriaCapacidad();
                        cap.setCategoria(categoria);
                        cap.setValorCapacidad(BigDecimal.valueOf(valor));
                        return cap;
                    })
                    .collect(Collectors.toList());
            categoria.getCapacidades().addAll(nuevasCapacidades);
        }

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

        // VALIDACIÓN DE INTEGRIDAD REFERENCIAL: Verificar si hay productos asociados
        if (!productoRepository.findByCategoriaId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta categoría no se puede eliminar porque tiene productos asociados.");
        }

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

    @Override
    @Transactional
    public CategoriaDTO.DetalleResponse findDetalleById(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));

        // Obtener productos de esta categoría para verificar capacidades en uso
        List<Producto> productos = productoRepository.findByCategoriaId(id);
        List<BigDecimal> capacidadesEnUso = productos.stream()
                .map(Producto::getCapacidad)
                .filter(cap -> cap != null)
                .distinct()
                .collect(Collectors.toList());

        // Construir lista de capacidades con flag enUso
        List<CapacidadInfoDTO> capacidadesInfo = categoria.getCapacidades().stream()
                .map(cap -> {
                    boolean enUso = capacidadesEnUso.contains(cap.getValorCapacidad());
                    return new CapacidadInfoDTO(cap.getValorCapacidad(), enUso);
                })
                .collect(Collectors.toList());

        return new CategoriaDTO.DetalleResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getEstado(),
                categoria.getUnidadMedida(),
                categoria.getRequiereCapacidad(),
                categoria.getEtiquetaCapacidad(),
                categoria.getManejaEnvase(),
                capacidadesInfo
        );
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
