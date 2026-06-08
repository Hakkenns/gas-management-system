package com.gas.sistema_gas.service;

import java.util.List;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.dto.EmpleadoDTO;

public interface EmpleadoService {
    List<EmpleadoDTO.SimpleResponse> listAll();
    EmpleadoDTO.SimpleResponse createEmployee(EmpleadoDTO.Create createDto);
    EmpleadoDTO.SimpleResponse updateEmployee(Long id, EmpleadoDTO.Update updateDto);
    void deleteEmployee(Long id);
    EmpleadoDTO.SimpleResponse findById(Long id);
    List<EmpleadoDTO.SimpleResponse> listEmpleadosSinUsuario();
    List<EmpleadoDTO.SimpleResponse> listDisponibles();
}
