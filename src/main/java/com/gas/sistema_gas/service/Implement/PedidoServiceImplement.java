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
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.service.PedidoService;

import jakarta.transaction.Transactional;

@Service
public class PedidoServiceImplement implements PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private PedidoMapper pedidoMapper;
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

    @Override
    @Transactional
    public List<PedidoDTO.SimpleResponse> listAll() {
        return pedidoRepository.findAll().stream()
                .map(pedidoMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse createOrder(PedidoDTO.Create createDto) {
        Pedido pedido = pedidoMapper.toEntity(createDto);

        // 1. Validar Relaciones
        Cliente cliente = clienteRepository.findById(createDto.idCliente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        Usuario usuario = usuarioRepository.findById(createDto.idUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe"));
        Empleado empleado = empleadoRepository.findById(createDto.idEmpleado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        MetodoPago metodoPago = metodoPagoRepository.findById(createDto.idMetodoPago())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago no encontrado"));

        // 2. Generar Código Correlativo Simple (Ej: PED-2026-0001)
        // Nota: Esto se puede mejorar con un método en el Repo, pero aquí te doy la
        // lógica base
        pedido.setCodigo("PED-" + System.currentTimeMillis()); // Generación rápida temporal

        pedido.setCliente(cliente);
        pedido.setUsuario(usuario);
        pedido.setEmpleado(empleado);
        pedido.setMetodoPago(metodoPago);
        pedido.setFechaSolicitud(LocalDateTime.now());
        pedido.setEstadoPedido("PENDIENTE");
        pedido.setEstadoPago("PENDIENTE");

        // Guardar cabecera inicial
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        BigDecimal montoAcumulado = BigDecimal.ZERO;

        // 3. Procesar Detalles y Stock
        for (PedidoDTO.DetalleCreate item : createDto.detalles()) {
            Producto producto = productoRepository.findById(item.idProducto())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

            // Validar Stock Llenos
            if (producto.getStockLlenos() < item.cantidad()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para " + producto.getNombre());
            }

            // Descontar Stock
            producto.setStockLlenos(producto.getStockLlenos() - item.cantidad());
            productoRepository.save(producto);

            // Crear Detalle
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedidoGuardado);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(item.precioUnitario());

            // Calculamos el importe de la línea
            BigDecimal importeLinea = item.precioUnitario().multiply(new BigDecimal(item.cantidad()));
            montoAcumulado = montoAcumulado.add(importeLinea);

            detalleRepository.save(detalle);
        }

        // 4. Actualizar Totales
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