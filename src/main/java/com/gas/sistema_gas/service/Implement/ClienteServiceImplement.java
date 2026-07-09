package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.ClienteMapper;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.dto.ClienteDTO;
import com.gas.sistema_gas.service.ClienteService;

@Service
public class ClienteServiceImplement implements ClienteService {

    @Autowired
    private ClienteMapper clienteMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDTO.SimpleResponse> listAll() {
        return clienteRepository.findAll().stream()
                .filter(c -> c.getEstado() == 1)
                .map(clienteMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteDTO.SimpleResponse createClient(ClienteDTO.Create createDto) {

        // 1. Validar unicidad del Teléfono (Crucial para el negocio de Gas)
        if (clienteRepository.existsByTelefono(createDto.telefono())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con ese número de teléfono");
        }

        // 2. Validar unicidad del DNI solo si lo envían
        if (createDto.dni() != null && !createDto.dni().isBlank()) {
            if (clienteRepository.existsByDni(createDto.dni())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con ese DNI");
            }
        }

        // Mapeo de DTO a entidad
        Cliente cliente = clienteMapper.toEntity(createDto);

        // 3. Solución al NullPointerException y lógica de "Cliente Varios"
        if (cliente.getNombre() == null || cliente.getNombre().trim().isBlank()) {
            cliente.setNombre("Cliente Varios / Anónimo");
        }

        // Guardar registro
        Cliente clienteGuardado = clienteRepository.save(cliente);

        return clienteMapper.toSimpleResponse(clienteGuardado);
    }

    @Override
    @Transactional
    public ClienteDTO.SimpleResponse updateClient(Long id, ClienteDTO.Update updateDto) {

        // Buscar el cliente actual
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no existe"));

        // Validar si el teléfono a actualizar pertenece a otro cliente
        clienteRepository.findByTelefono(updateDto.telefono())
                .ifPresent(c -> {
                    if (!c.getId().equals(id)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "El teléfono ya está asignado a otro cliente");
                    }
                });

        // Actualizar datos del objeto mapeado
        clienteMapper.updateEntityFromDto(updateDto, cliente);

        if (cliente.getNombre() == null || cliente.getNombre().trim().isBlank()) {
            cliente.setNombre("Cliente Varios / Anónimo");
        }

        // Guardar los cambios explícitamente en la base de datos
        Cliente clienteActualizado = clienteRepository.save(cliente);

        return clienteMapper.toSimpleResponse(clienteActualizado);
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no existe"));

        // Eliminación lógica
        cliente.setEstado(2);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDTO.SimpleResponse findById(Long id) {
        return clienteRepository.findById(id)
                .filter(cliente -> cliente.getEstado() == 1) // Filtra para que no devuelva eliminados lógicos
                .map(clienteMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDTO.SimpleResponse findByDni(String dni) {
        return clienteRepository.findByDni(dni)
                .filter(cliente -> cliente.getEstado() == 1)
                .map(clienteMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }
}
