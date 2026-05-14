package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.EmpleadoMapper;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.dto.EmpleadoDTO;
import com.gas.sistema_gas.service.EmpleadoService;

import jakarta.transaction.Transactional;

@Service
public class EmpleadoServiceImplement implements EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private EmpleadoMapper empleadoMapper;

    @Override
    @Transactional
    public List<EmpleadoDTO.SimpleResponse> listAll() {

        return empleadoRepository.findAll().stream()
                .filter(e -> e.getEstado() == 1)
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
    public EmpleadoDTO.SimpleResponse updateEmployee(Long id, EmpleadoDTO.Create updateDto) {

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

        // Actualizar datos
        emp.setNombre(updateDto.nombre());
        emp.setDni(updateDto.dni());
        emp.setSueldoBase(updateDto.sueldoBase());
        emp.setTelefono(updateDto.telefono());

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
    @Transactional
    public EmpleadoDTO.SimpleResponse findById(Long id) {

        return empleadoRepository.findById(id)
                .map(empleadoMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empleado no encontrado"));
    }
}