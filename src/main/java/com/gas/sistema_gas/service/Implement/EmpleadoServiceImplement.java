package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.EmpleadoMapper;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.dto.EmpleadoDTO;
import com.gas.sistema_gas.service.EmpleadoService;



@Service
public class EmpleadoServiceImplement implements EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private EmpleadoMapper empleadoMapper;

    @Override
    @Transactional(readOnly = true)     // útil para optimizar el rendimiento de lectura
    public List<EmpleadoDTO.SimpleResponse> listAll() {
        return empleadoRepository.findByEstado(1).stream()
                .map(empleadoMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmpleadoDTO.SimpleResponse createEmployee(EmpleadoDTO.Create dto) {

        // Validar DNI duplicado
        if (empleadoRepository.existsByDni(dto.dni())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El DNI ya está registrado");
        }

        // Validar teléfono duplicado
        if (empleadoRepository.existsByTelefono(dto.telefono())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El teléfono ya está registrado");
        }

        Empleado empleado = empleadoMapper.toEntity(dto);
        Empleado guardado = empleadoRepository.save(empleado);
        return empleadoMapper.toSimpleResponse(guardado);
    }

    @Override
    @Transactional
    public EmpleadoDTO.SimpleResponse updateEmployee(Long id, EmpleadoDTO.Update updateDto) {

        Empleado emp = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empleado no encontrado"));

        // Validar DNI
        if (!emp.getDni().equals(updateDto.dni())) {
            if (empleadoRepository.existsByDni(updateDto.dni())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El DNI ya está registrado");
            }
        }

        // Validar teléfono
        if (!emp.getTelefono().equals(updateDto.telefono())) {
            if (empleadoRepository.existsByTelefono(updateDto.telefono())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El teléfono ya está registrado");
            }
        }

        empleadoMapper.updateEntityFromDTO(updateDto, emp);
        Empleado actualizado = empleadoRepository.save(emp);
        return empleadoMapper.toSimpleResponse(actualizado);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {

        Empleado emp = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empleado no encontrado"));

        emp.setEstado(0);
        empleadoRepository.save(emp);
    }

    @Override
    @Transactional(readOnly = true) // Útil para el rendimiento de lectura
    public EmpleadoDTO.SimpleResponse findById(Long id) {
        return empleadoRepository.findByIdAndEstado(id, 1)
                .map(empleadoMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empleado no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoDTO.SimpleResponse> listEmpleadosSinUsuario() {
        return empleadoRepository.findEmpleadosSinUsuario().stream()
                .map(empleadoMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }
}