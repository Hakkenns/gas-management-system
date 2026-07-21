package com.gas.sistema_gas.service.Implement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.ControlEnvase;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.EnvioEnvaseDTO;
import com.gas.sistema_gas.service.EnvaseService;

import jakarta.transaction.Transactional;

@Service
public class EnvaseServiceImplement implements EnvaseService {

    @Autowired
    private ControlEnvaseRepository controlEnvaseRepository;

    @Autowired
    private ProductoRepository productoRepository;

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
        ControlEnvase control = controlEnvaseRepository.findById(request.idControl())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "El registro de control de envase no existe"));

        if (request.cantidadDevolver() == null || request.cantidadDevolver() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "La cantidad a devolver debe ser mayor a 0");
        }

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
        if (control.getProducto() != null) {
            Producto producto = control.getProducto();
            Integer stockVaciosActual = producto.getStockVacios() != null ? producto.getStockVacios() : 0;
            producto.setStockVacios(stockVaciosActual + request.cantidadDevolver());
            productoRepository.save(producto);
        }
    }
}