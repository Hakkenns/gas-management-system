package com.gas.sistema_gas.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.dto.CategoriaDTO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.gas.sistema_gas.Model.CategoriaCapacidad;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "unidadMedida", ignore = true)
    @Mapping(target = "requiereCapacidad", ignore = true)
    @Mapping(target = "etiquetaCapacidad", ignore = true)
    @Mapping(target = "manejaEnvase", ignore = true)
    @Mapping(target = "capacidades", ignore = true)
    Categoria toEntity(CategoriaDTO.Create createDto);

    default CategoriaDTO.SimpleResponse toSimpleResponse(Categoria categoria) {
        List<Double> capacidades = categoria.getCapacidades() != null
                ? categoria.getCapacidades().stream()
                        .map(c -> c.getValorCapacidad().doubleValue())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return new CategoriaDTO.SimpleResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getEstado(),
                categoria.getUnidadMedida(),
                categoria.getRequiereCapacidad(),
                categoria.getEtiquetaCapacidad(),
                categoria.getManejaEnvase(),
                capacidades);
    }
}
