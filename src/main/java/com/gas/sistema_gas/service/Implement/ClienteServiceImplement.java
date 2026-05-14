package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.ClienteMapper;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.dto.ClienteDTO;
import com.gas.sistema_gas.service.ClienteService;

import jakarta.transaction.Transactional;

public class ClienteServiceImplement implements ClienteService{
    
    @Autowired
    private ClienteMapper clienteMapper;
    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    @Transactional
    public List<ClienteDTO.SimpleResponse> listAll(){
        return  clienteRepository.findAll().stream()
                .filter(c -> c.getEstado() == 1)
                .map(clienteMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }   

    @Override
    @Transactional
    public ClienteDTO.SimpleResponse createClient(ClienteDTO.Create createDto){

        // Validamos si el DNI del cliente viene vacío
        // Si el DNI no es nullo y el dni tampoco esta vacío entrar a la otra condición
        if(createDto.dni() !=null && !createDto.correo().isBlank()){    
            if(clienteRepository.existsByDni(createDto.dni())){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con ese DNI");
            }
        }

        // Mapeo de DTO a entidad
        Cliente cliente = clienteMapper.toEntity(createDto);

        // Lógica para un cliente vacío
        if(cliente.getDni() == null || cliente.getNombre().isBlank()){
            cliente.setNombre("Cliente Varios / Anónimo");
        }

        // Guardamos
        Cliente clienteGuardado = clienteRepository.save(cliente);

        return clienteMapper.toSimpleResponse(clienteGuardado);

    }

    @Override
    @Transactional
    public ClienteDTO.SimpleResponse updateClient(Long id, ClienteDTO.Update updateDto){

        // Buscamos el cliente Actual
        Cliente cliente = clienteRepository.findById(id)
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no existe"));
        
        clienteMapper.updateEntityFromDto(updateDto, cliente);

        return clienteMapper.toSimpleResponse(cliente);
    }

    @Override
    @Transactional
    public void deleteClient(Long id){

        // Buscamos al cliente por su id
        Cliente cliente = clienteRepository.findById(id)
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no existe"));

        // Eliminación lógica: Cambiamos de estado
        cliente.setEstado(0);

        // Guardamos los cambiamos
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public ClienteDTO.SimpleResponse findById(Long id){
        return  clienteRepository.findById(id)
                .map(clienteMapper::toSimpleResponse)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }
}
