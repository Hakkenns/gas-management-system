package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import com.gas.sistema_gas.Repository.RubroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.ProveedorMapper;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Repository.ProveedorRepository;
import com.gas.sistema_gas.dto.ProveedorDTO;
import com.gas.sistema_gas.service.ProveedorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Esto inyecta los final (Mapper y Repository) automáticamente
public class ProveedorServiceImplement implements ProveedorService {
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private  ProveedorMapper proveedorMapper;
    @Autowired
    private RubroRepository rubroRepository;

    @Override
    @Transactional
    public List<ProveedorDTO.SimpleResponse> listAll() {
        return proveedorRepository.findAll().stream()
                .filter(categoria -> categoria.getEstado() != 2)
                .map(proveedorMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProveedorDTO.SimpleResponse create(ProveedorDTO.Create dto) {
        if (proveedorRepository.existsByRuc(dto.ruc())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El RUC ya está registrado");
        }
        Proveedor proveedor = proveedorMapper.toEntity(dto);
        return proveedorMapper.toSimpleResponse(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public ProveedorDTO.SimpleResponse update(Long id, ProveedorDTO.Update updateDto) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));

        // 1. Validar el RUC único (solo si cambió)
        if (!proveedor.getRuc().equalsIgnoreCase(updateDto.ruc())) {
            if (proveedorRepository.existsByRuc(updateDto.ruc())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El RUC ya está registrado por otro proveedor");
            }
        }

        // 2. Buscamos la entidad del nuevo Rubro seleccionado en el formulario
        com.gas.sistema_gas.Model.Rubro nuevoRubro = rubroRepository.findById(updateDto.idRubro())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El rubro seleccionado no existe"));

        // 3. Rompemos temporalmente la relación vieja para evitar el error de Hibernate
        proveedor.setRubro(null); 

        // 4. Actualizamos los campos planos (Nombre, Teléfono, Correo, RUC) usando tu mapper
        proveedorMapper.updateEntityFromDto(updateDto, proveedor);

        // 5. Asignamos manualmente la nueva entidad Rubro completa
        proveedor.setRubro(nuevoRubro);

        // 6. Guardamos los cambios de manera limpia
        return proveedorMapper.toSimpleResponse(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
        proveedor.setEstado(2); // Borrado lógico
        proveedorRepository.save(proveedor);
    }

    @Override
    @Transactional
    public ProveedorDTO.SimpleResponse setState(Long id, Integer estado) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
        proveedor.setEstado(estado);
        return proveedorMapper.toSimpleResponse(proveedorRepository.save(proveedor));
    }

    @Override
    public ProveedorDTO.SimpleResponse findById(Long id) {
        return proveedorRepository.findById(id)
                .map(proveedorMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
    }
}