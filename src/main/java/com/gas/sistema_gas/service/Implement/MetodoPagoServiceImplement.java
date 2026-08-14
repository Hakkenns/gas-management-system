package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.MetodoPagoMapper;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.dto.MetodoPagoDTO;
import com.gas.sistema_gas.service.MetodoPagoService;
import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.TipoFinancieroMetodoPago;

import jakarta.transaction.Transactional;

@Service
public class MetodoPagoServiceImplement implements MetodoPagoService {
    @Autowired
    private  MetodoPagoRepository repository;
    @Autowired
    private  MetodoPagoMapper mapper;

    @Override
    @Transactional
    public List<MetodoPagoDTO.Response> listActive() {
        return repository.findByEstado(1).stream()
                .map(mapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MetodoPagoDTO.Response create(MetodoPagoDTO.Create dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos del método de pago inválidos");
        }
        String codigo = normalizarCodigo(dto.codigo());
        if (dto.nombre() == null || dto.nombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del método de pago es obligatorio");
        }
        String nombre = dto.nombre().trim();
        if (dto.tipoFinanciero() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El tipo financiero del método de pago es obligatorio");
        }
        if (repository.findByCodigo(codigo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código de método de pago ya existe");
        }
        if (repository.findByNombre(nombre).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de método de pago ya existe");
        }

        MetodoPago metodo = mapper.toEntity(dto);
        metodo.setCodigo(codigo);
        metodo.setNombre(nombre);
        metodo.setTipoFinanciero(dto.tipoFinanciero());
        metodo.setEstado(1); // Siempre activo al crear
        try {
            return mapper.toSimpleResponse(repository.saveAndFlush(metodo));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El código o nombre del método de pago ya existe");
        }
    }

    @Override
    @Transactional
    public MetodoPago obtenerActivo(Long idMetodo) {
        if (idMetodo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del método de pago es obligatorio");
        }
        MetodoPago metodo = repository.findById(idMetodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago no encontrado"));
        if (!Integer.valueOf(1).equals(metodo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El método de pago está inactivo");
        }
        if (metodo.getTipoFinanciero() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El método de pago no tiene clasificación financiera válida");
        }
        return metodo;
    }

    @Override
    public String validarYNormalizarNumeroOperacion(MetodoPago metodo, String numOperacion) {
        if (metodo == null || metodo.getTipoFinanciero() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El método de pago no tiene clasificación financiera válida");
        }
        String normalizado = numOperacion == null ? null : numOperacion.trim();
        if (normalizado != null && normalizado.isEmpty()) {
            normalizado = null;
        }
        if (TipoFinancieroMetodoPago.EFECTIVO.equals(metodo.getTipoFinanciero())) {
            if (normalizado != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El efectivo no admite número de operación");
            }
            return null;
        }
        if (normalizado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El número de operación es obligatorio para pagos digitales");
        }
        if (normalizado.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El número de operación no puede superar 50 caracteres");
        }
        return normalizado;
    }

    private String normalizarCodigo(String codigo) {
        String normalizado = codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
        if (normalizado == null || normalizado.isEmpty() || !normalizado.matches("[A-Z0-9_]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El código debe usar solo letras mayúsculas, números o guion bajo");
        }
        if (normalizado.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El código no puede superar 50 caracteres");
        }
        return normalizado;
    }
}
