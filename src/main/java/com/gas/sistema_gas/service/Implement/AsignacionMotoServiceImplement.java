package com.gas.sistema_gas.service.Implement;

import com.gas.sistema_gas.dto.AsignacionMotoDTO;
import com.gas.sistema_gas.Mapper.AsignacionMotoMapper;
import com.gas.sistema_gas.Model.AsignacionMoto;
import com.gas.sistema_gas.Model.AsignacionMoto.EstadoAsignacion;
import com.gas.sistema_gas.Repository.AsignacionMotoRepository;
import com.gas.sistema_gas.service.AsignacionMotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionMotoServiceImplement implements AsignacionMotoService {

    private final AsignacionMotoRepository asignacionMotoRepository;
    private final AsignacionMotoMapper asignacionMotoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionMotoDTO.SimpleResponse> listAll() {
        return asignacionMotoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(asignacionMotoMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void asignar(AsignacionMotoDTO.Create create) {
        // CORREGIDO: Acceso directo a métodos del Record (sin "get")
        if (asignacionMotoRepository.findByEmpleadoIdAndEstado(create.empleadoId(), EstadoAsignacion.ACTIVA).isPresent()) {
            throw new IllegalArgumentException("El empleado ya cuenta con una asignación de moto ACTIVA.");
        }

        // CORREGIDO: Acceso directo a métodos del Record (sin "get")
        if (asignacionMotoRepository.findByMotoIdAndEstado(create.motoId(), EstadoAsignacion.ACTIVA).isPresent()) {
            throw new IllegalArgumentException("La moto seleccionada ya está asignada a otro empleado actualmente.");
        }

        AsignacionMoto nuevaAsignacion = asignacionMotoMapper.toEntity(create);
        nuevaAsignacion.setEstado(EstadoAsignacion.ACTIVA);
        asignacionMotoRepository.save(nuevaAsignacion);
    }

    @Override
    @Transactional
    public void finalizar(Long id) {
        AsignacionMoto asignacion = asignacionMotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada."));

        if (asignacion.getEstado() == EstadoAsignacion.FINALIZADA) {
            throw new IllegalArgumentException("Esta asignación ya fue finalizada previamente.");
        }

        asignacion.setEstado(EstadoAsignacion.FINALIZADA);
        asignacion.setFechaDevolucion(LocalDateTime.now());
        asignacionMotoRepository.save(asignacion);
    }
}