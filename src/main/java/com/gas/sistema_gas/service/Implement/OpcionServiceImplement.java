package com.gas.sistema_gas.service.Implement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.OpcionMapper;
import com.gas.sistema_gas.Model.Opcion;
import com.gas.sistema_gas.Repository.OpcionRepository;
import com.gas.sistema_gas.dto.OpcionDTO;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.transaction.Transactional;

@Service
public class OpcionServiceImplement implements OpcionService {

    @Autowired
    private OpcionRepository repository;
    
    @Autowired
    private OpcionMapper opcionMapper;

    @Override
    @Transactional
    public List<OpcionDTO.SimpleResponse> listAll() {
        // Traemos solo los módulos principales activos
        List<OpcionDTO.SimpleResponse> opciones = repository.findByPadreIsNullAndEstado(1).stream()
                .map(opcion -> {
                    // Filtramos sus hijos para que solo viajen los que están activos
                    List<Opcion> hijosFiltrados = null;
                    if (opcion.getHijos() != null) {
                        hijosFiltrados = opcion.getHijos().stream()
                                .filter(h -> h.getEstado() == 1)
                                .collect(Collectors.toList());
                    }
                    
                    // Crear DTO con hijos mapeados recursivamente
                    OpcionDTO.SimpleResponse dto = opcionMapper.toSimpleResponse(opcion);
                    if (hijosFiltrados != null && !hijosFiltrados.isEmpty()) {
                        List<OpcionDTO.SimpleResponse> hijosDto = hijosFiltrados.stream()
                            .map(h -> opcionMapper.toSimpleResponse(h))
                            .collect(Collectors.toList());
                        // Usar reflexión para establecer los hijos en el DTO record
                        try {
                            java.lang.reflect.Field hijosField = dto.getClass().getDeclaredField("hijos");
                            hijosField.setAccessible(true);
                            hijosField.set(dto, hijosDto);
                        } catch (Exception e) {
                            // Si falla, devolver el DTO sin hijos
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return buildVentasGroup(opciones);
    }

    @Override
    @Transactional
    public List<OpcionDTO.SimpleResponse> listByPerfilId(Long perfilId) {
        if (perfilId == null) return listAll();

        // Si es administrador (id == 1) mostramos toda la estructura activa
        if (perfilId.equals(1L)) {
            return listAll();
        }

        // Para otros perfiles: traemos raíces activas y filtramos hijos asignados a su perfil
        List<OpcionDTO.SimpleResponse> opciones = repository.findByPadreIsNullAndEstado(1).stream()
            .map(opcion -> {
                // Filtrar hijos activos y con permiso del perfil
                List<Opcion> hijosFiltrados = null;
                if (opcion.getHijos() != null) {
                    hijosFiltrados = opcion.getHijos().stream()
                        .filter(h -> h.getEstado() == 1)
                        .filter(h -> h.getPerfiles() != null && h.getPerfiles().stream().anyMatch(p -> p.getId().equals(perfilId)))
                        .collect(Collectors.toList());
                }
                
                // Crear DTO con hijos mapeados recursivamente
                OpcionDTO.SimpleResponse dto = opcionMapper.toSimpleResponse(opcion);
                if (hijosFiltrados != null && !hijosFiltrados.isEmpty()) {
                    List<OpcionDTO.SimpleResponse> hijosDto = hijosFiltrados.stream()
                        .map(h -> opcionMapper.toSimpleResponse(h))
                        .collect(Collectors.toList());
                    // Usar reflexión para establecer los hijos en el DTO record
                    try {
                        java.lang.reflect.Field hijosField = dto.getClass().getDeclaredField("hijos");
                        hijosField.setAccessible(true);
                        hijosField.set(dto, hijosDto);
                    } catch (Exception e) {
                        // Si falla, devolver el DTO sin hijos
                    }
                }
                return dto;
            })
            // El menú padre se muestra si él mismo tiene permiso OR si le quedó algún hijo con permiso
            .filter(dto -> {
                // Verificar si la opción padre tiene permiso
                boolean padreTienePermiso = repository.findById(dto.id())
                    .map(o -> o.getPerfiles() != null && o.getPerfiles().stream().anyMatch(p -> p.getId().equals(perfilId)))
                    .orElse(false);
                return padreTienePermiso || (dto.hijos() != null && !dto.hijos().isEmpty());
            })
            .collect(Collectors.toList());

        return buildVentasGroup(opciones);
    }

    private List<OpcionDTO.SimpleResponse> buildVentasGroup(List<OpcionDTO.SimpleResponse> opciones) {
        List<OpcionDTO.SimpleResponse> result = new ArrayList<>();
        List<OpcionDTO.SimpleResponse> ventasChildren = new ArrayList<>();

        for (OpcionDTO.SimpleResponse opcion : opciones) {
            if (opcion.ruta() != null && ("ventas/local".equals(opcion.ruta()) || "ventas/domicilio".equals(opcion.ruta()))) {
                ventasChildren.add(new OpcionDTO.SimpleResponse(
                    opcion.id(),
                    "ventas/local".equals(opcion.ruta()) ? "Venta local" : "Venta domicilio",
                    "ventas/local".equals(opcion.ruta()) ? "fas fa-store" : "fas fa-motorcycle",
                    opcion.ruta(),
                    opcion.estado(),
                    List.of()
                ));
            } else {
                result.add(opcion);
            }
        }

        if (!ventasChildren.isEmpty()) {
            result.add(new OpcionDTO.SimpleResponse(
                -1L,
                "Ventas",
                "fas fa-cash-register",
                "ventas",
                1,
                ventasChildren
            ));
        }

        return result;
    }

    @Override
    @Transactional
    public OpcionDTO.SimpleResponse createOpcion(OpcionDTO.Create createDto) {
        Opcion opcion = opcionMapper.toEntity(createDto);
        opcion.setEstado(1);
        return opcionMapper.toSimpleResponse(repository.save(opcion));
    }

    @Override
    @Transactional
    public OpcionDTO.SimpleResponse updateOpcion(Long id, OpcionDTO.Update updateDto) {
        Opcion opcion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La opción no existe"));
        
        opcionMapper.updateEntityFromDto(updateDto, opcion);
        return opcionMapper.toSimpleResponse(repository.save(opcion));
    }

    @Override
    @Transactional
    public void deleteOpcion(Long id) {
        Opcion opcion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La opción no existe"));
        
        // Eliminación lógica
        opcion.setEstado(0);
        repository.save(opcion);
    }

    @Override
    @Transactional
    public OpcionDTO.SimpleResponse findById(Long id) {
        return repository.findById(id)
                .map(opcionMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La opción no existe"));
    }

    
}