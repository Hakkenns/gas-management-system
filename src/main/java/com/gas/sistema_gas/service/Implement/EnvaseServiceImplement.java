package com.gas.sistema_gas.service.Implement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.ControlEnvase;
import com.gas.sistema_gas.Model.Envase;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.Repository.EnvaseRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.EnvioEnvaseDTO;
import com.gas.sistema_gas.service.EnvaseService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class EnvaseServiceImplement implements EnvaseService {

    @Autowired
    private ControlEnvaseRepository controlEnvaseRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EnvaseRepository envaseRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<Envase> listarTodos() {
        return envaseRepository.findAll().stream()
                .filter(envase -> Boolean.TRUE.equals(envase.getEstado()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Envase obtenerPorId(Long id) {
        return envaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El envase no existe"));
    }

    @Override
    @Transactional
    public Envase guardar(Envase envase) {
        return envaseRepository.save(envase);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Envase envase = obtenerPorId(id);
        envase.setEstado(false);
        envaseRepository.save(envase);
    }

    @Override
    @Transactional
    public List<EnvioEnvaseDTO.DeudorResponse> listarDeudoresPendientes() {
        List<ControlEnvase> deudas = controlEnvaseRepository.findDeudoresPendientes();

        return deudas.stream().map(ce -> {
            Integer prestada = ce.getCantidadPrestada() != null ? ce.getCantidadPrestada() : 0;
            Integer devuelta = ce.getCantidadDevuelta() != null ? ce.getCantidadDevuelta() : 0;
            Integer pendiente = prestada - devuelta;

            String nombreProducto = ce.getProducto() != null ? ce.getProducto().getNombre() : "N/A";
            if (ce.getProducto() != null && ce.getProducto().getCapacidad() != null) {
                nombreProducto += " - " + ce.getProducto().getCapacidad();
                String um = ce.getProducto().getUnidadMedida();
                if ("KG".equals(um)) nombreProducto += " kg";
                else if ("L".equals(um)) nombreProducto += " L";
                else if ("M".equals(um)) nombreProducto += " m";
            }

            return new EnvioEnvaseDTO.DeudorResponse(
                ce.getId(),
                ce.getCliente().getId(),
                ce.getCliente().getNombre(),
                ce.getCliente().getTelefono(),
                ce.getCliente().getDireccion(),
                ce.getPedido() != null ? ce.getPedido().getId() : null,
                ce.getPedido() != null ? ce.getPedido().getCodigo() : null,
                ce.getPedido() != null ? ce.getPedido().getFechaEntrega() : null,
                nombreProducto,
                prestada,
                devuelta,
                pendiente,
                ce.getEstado(),
                ce.getProducto() != null ? ce.getProducto().getId() : null
            );
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void registrarDevolucion(EnvioEnvaseDTO.DevolucionRequest request) {
        if (request.cantidadDevolver() == null || request.cantidadDevolver() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "La cantidad a devolver debe ser mayor a 0");
        }

        ControlEnvase controlInicial = controlEnvaseRepository.findById(request.idControl())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "El registro de control de envase no existe"));

        if (controlInicial.getProducto() == null || controlInicial.getProducto().getId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "El producto asociado al control de envase no existe");
        }

        Long idProducto = controlInicial.getProducto().getId();
        entityManager.detach(controlInicial);
        List<Producto> productosBloqueados = productoRepository.findAllByIdInForUpdate(List.of(idProducto));
        if (productosBloqueados.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "El producto asociado al control de envase no existe");
        }
        Producto productoBloqueado = productosBloqueados.get(0);

        ControlEnvase control = controlEnvaseRepository.findByIdForUpdate(request.idControl())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "El registro de control de envase no existe"));

        Integer prestada = control.getCantidadPrestada() != null ? control.getCantidadPrestada() : 0;
        Integer devueltaActual = control.getCantidadDevuelta() != null ? control.getCantidadDevuelta() : 0;
        Integer nuevaDevuelta = devueltaActual + request.cantidadDevolver();

        if (nuevaDevuelta > prestada) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "La cantidad a devolver excede el saldo pendiente");
        }

        // Actualizar cantidad devuelta
        control.setCantidadDevuelta(nuevaDevuelta);
        control.setFechaDevolucion(LocalDateTime.now());

        // Actualizar estado
        if (nuevaDevuelta >= prestada) {
            control.setEstado("SALDADO");
        } else {
            control.setEstado("PARCIAL");
        }

        controlEnvaseRepository.save(control);

        // Actualizar stock_vacios del producto (aumenta porque el envase vuelve al local)
        Integer stockVaciosActual = productoBloqueado.getStockVacios() != null ? productoBloqueado.getStockVacios() : 0;
        productoBloqueado.setStockVacios(stockVaciosActual + request.cantidadDevolver());
        productoRepository.save(productoBloqueado);
    }
}
