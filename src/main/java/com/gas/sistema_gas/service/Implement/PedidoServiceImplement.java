package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.PedidoMapper;
import com.gas.sistema_gas.Mapper.ClienteMapper;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.ClienteDTO;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.service.CorrelativoService;
import com.gas.sistema_gas.service.PedidoService;

import jakarta.transaction.Transactional;

@Service
public class PedidoServiceImplement implements PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private PedidoMapper pedidoMapper;
    @Autowired
    private ClienteMapper clienteMapper;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpleadoRepository empleadoRepository;
    @Autowired
    private DetallePedidoRepository detalleRepository;
    @Autowired
    private MetodoPagoRepository metodoPagoRepository;
    @Autowired
    private CorrelativoService correlativoService;

    @Override
    @Transactional
    public List<PedidoDTO.SimpleResponse> listAll() {
        return pedidoRepository.findAll().stream()
                .map(pedidoMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse createOrder(PedidoDTO.Create createDto, Long idUsuarioLogueado) {
        if (createDto.detalles() == null || createDto.detalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe agregar al menos un detalle de venta");
        }

        Pedido pedido = pedidoMapper.toEntity(createDto);

        // 1. Validar Relaciones
        Cliente cliente;
        if (createDto.idCliente() != null) {
            cliente = clienteRepository.findById(createDto.idCliente())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        } else if (createDto.dniCliente() != null && !createDto.dniCliente().isBlank()) {
            cliente = clienteRepository.findByDni(createDto.dniCliente())
                    .filter(c -> c.getEstado() == 1)
                    .orElseGet(() -> {
                        ClienteDTO.Create clienteDto = new ClienteDTO.Create(
                                createDto.nombreCliente(),
                                createDto.dniCliente(),
                                createDto.telefonoCliente(),
                                createDto.direccionCliente(),
                                createDto.referenciaCliente(),
                                null
                        );
                        Cliente nuevoCliente = clienteMapper.toEntity(clienteDto);
                        return clienteRepository.save(nuevoCliente);
                    });
        } else {
            if (createDto.nombreCliente() == null || createDto.nombreCliente().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del cliente es obligatorio");
            }
            if (createDto.direccionCliente() == null || createDto.direccionCliente().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dirección del cliente es obligatoria");
            }
            if (createDto.telefonoCliente() == null || createDto.telefonoCliente().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El teléfono del cliente es obligatorio");
            }
            ClienteDTO.Create clienteDto = new ClienteDTO.Create(
                    createDto.nombreCliente(),
                    null,
                    createDto.telefonoCliente(),
                    createDto.direccionCliente(),
                    createDto.referenciaCliente(),
                    null
            );
            Cliente nuevoCliente = clienteMapper.toEntity(clienteDto);
            cliente = clienteRepository.save(nuevoCliente);
        }

        Usuario usuario = usuarioRepository.findById(idUsuarioLogueado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe"));
        MetodoPago metodoPago = metodoPagoRepository.findById(createDto.idMetodoPago())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago no encontrado"));

        Empleado empleado = null;
        if (createDto.idEmpleado() != null) {
            empleado = empleadoRepository.findById(createDto.idEmpleado())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        } else if (usuario.getEmpleado() != null) {
            empleado = usuario.getEmpleado();
        }

        String numOperacion = createDto.numOperacion();
        if (numOperacion != null && numOperacion.isBlank()) {
            numOperacion = null;
        }
        if (metodoPago.getNombre() != null && (
                metodoPago.getNombre().equalsIgnoreCase("yape") ||
                metodoPago.getNombre().equalsIgnoreCase("plin")
        ) && (numOperacion == null || numOperacion.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El número de operación es obligatorio para Yape y Plin");
        }

        // 2. Generar código definitivo de venta
        pedido.setCodigo(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001"));
        pedido.setCliente(cliente);
        pedido.setUsuario(usuario);
        pedido.setEmpleado(empleado);
        pedido.setMetodoPago(metodoPago);
        pedido.setNumOperacion(numOperacion);
        pedido.setFechaSolicitud(LocalDateTime.now());
        pedido.setEstadoPedido("PENDIENTE");
        pedido.setEstadoPago("PENDIENTE");

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        BigDecimal montoAcumulado = BigDecimal.ZERO;

        for (PedidoDTO.DetalleCreate item : createDto.detalles()) {
            Producto producto = productoRepository.findById(item.idProducto())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

            if (item.cantidad() == null || item.cantidad() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La cantidad debe ser mayor a cero");
            }

            BigDecimal precioUnitario = item.precioUnitario() != null && item.precioUnitario().compareTo(BigDecimal.ZERO) > 0
                    ? item.precioUnitario()
                    : producto.getPrecioVenta();

            if (producto.getStockLlenos() < item.cantidad()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para " + producto.getNombre());
            }

            producto.setStockLlenos(producto.getStockLlenos() - item.cantidad());
            productoRepository.save(producto);

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedidoGuardado);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(precioUnitario);

            BigDecimal importeLinea = precioUnitario.multiply(BigDecimal.valueOf(item.cantidad()));
            montoAcumulado = montoAcumulado.add(importeLinea);

            detalleRepository.save(detalle);
        }

        pedidoGuardado.setSubtotal(montoAcumulado);
        pedidoGuardado.setMontoTotal(montoAcumulado);

        return pedidoMapper.toSimpleResponse(pedidoRepository.save(pedidoGuardado));
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse updateOrder(Long id, PedidoDTO.Update updateDto) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        pedidoMapper.updateEntityFromDto(updateDto, pedido);

        if (updateDto.idMotorizado() != null) {
            Empleado motorizado = empleadoRepository.findById(updateDto.idMotorizado())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorizado no encontrado"));
            pedido.setEmpleado(motorizado);
        }

        if ("ENTREGADO".equalsIgnoreCase(updateDto.estadoPedido())) {
            pedido.setFechaEntrega(LocalDateTime.now());
            pedido.setEstadoPago("PAGADO");
        }

        return pedidoMapper.toSimpleResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        if ("ANULADO".equals(pedido.getEstadoPedido())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido ya está anulado");
        }

        // 3. Importante: Que el Repo tenga findByPedido(Pedido pedido)
        List<DetallePedido> detalles = detalleRepository.findByPedido(pedido);

        for (DetallePedido detalle : detalles) {
            Producto producto = detalle.getProducto();
            producto.setStockLlenos(producto.getStockLlenos() + detalle.getCantidad());
            productoRepository.save(producto);
        }

        pedido.setEstadoPedido("ANULADO");
        pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse findById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        return pedidoMapper.toSimpleResponse(pedido);
    }
}