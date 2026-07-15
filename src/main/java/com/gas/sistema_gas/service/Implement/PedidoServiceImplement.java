package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.ClienteDTO;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.dto.PedidoDTO.EditResponse;
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
    private PedidoPagoRepository pedidoPagoRepository;
    @Autowired
    private MetodoPagoRepository metodoPagoRepository;
    @Autowired
    private CorrelativoService correlativoService;
    @Autowired
    private com.gas.sistema_gas.Repository.ControlEnvaseRepository controlEnvaseRepository;
    @Autowired
    private com.gas.sistema_gas.Repository.InventarioLoteRepository inventarioLoteRepository;
    @Autowired
    private com.gas.sistema_gas.service.InventarioLoteService inventarioLoteService;

    @Override
    @Transactional
    public List<PedidoDTO.SimpleResponse> listAll() {
        return pedidoRepository.findAll().stream()
                .map(this::mapToSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PedidoDTO.SimpleResponse> listByTipoVenta(String tipoVenta) {
        return pedidoRepository.findByTipoVentaWithMetodoPago(tipoVenta).stream()
                .map(this::mapToSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PedidoDTO.SimpleResponse> listByTipoVentaAndEmpleadoId(String tipoVenta, Long empleadoId) {
        return pedidoRepository.findByTipoVentaAndEmpleadoIdWithMetodoPago(tipoVenta, empleadoId).stream()
                .map(this::mapToSimpleResponse)
                .collect(Collectors.toList());
    }

    private PedidoDTO.SimpleResponse mapToSimpleResponse(Pedido pedido) {
        String nombreCliente = pedido.getCliente() != null ? pedido.getCliente().getNombre() : null;
        String direccionCliente = pedido.getCliente() != null ? pedido.getCliente().getDireccion() : null;
        String nombreEmpleado = pedido.getEmpleado() != null ? pedido.getEmpleado().getNombre() : null;
        String metodoPagoNombre = pedido.getMetodoPago() != null ? pedido.getMetodoPago().getNombre() : null;
        
        PedidoDTO.SimpleResponse response = pedidoMapper.toSimpleResponse(pedido);
        Long empleadoId = pedido.getEmpleado() != null ? pedido.getEmpleado().getId() : null;
        return new PedidoDTO.SimpleResponse(
            response.idPedido(),
            response.codigo(),
            response.fechaSolicitud(),
            nombreCliente,
            direccionCliente,
            empleadoId,
            pedido.getObservaciones(),
            nombreEmpleado,
            pedido.getEstadoPedido(),
            pedido.getEstadoPago(),
            response.montoTotal(),
            response.subtotal(),
            metodoPagoNombre,
            response.tipoVenta(),
            response.fechaLimitePago()
        );
    }

    @Override
    @Transactional
    public long countSalesToday() {
        LocalDateTime inicio = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1).minusNanos(1);
        return pedidoRepository.countByEstadoPedidoAndFechaSolicitudBetween("ENTREGADO", inicio, fin);
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse createOrder(PedidoDTO.Create createDto, Long idUsuarioLogueado) {
        if (createDto.detalles() == null || createDto.detalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe agregar al menos un detalle de venta");
        }

        Pedido pedido;
        if (createDto.idPedido() != null) {
            // Lógica de Actualización
            pedido = pedidoRepository.findById(createDto.idPedido())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El pedido a editar no fue encontrado"));

            // Revertir stock de productos
            List<DetallePedido> detallesAnteriores = detalleRepository.findByPedido_Id(pedido.getId());
            for (DetallePedido detalle : detallesAnteriores) {
                Producto producto = detalle.getProducto();
                BigDecimal cantidadDevolver = BigDecimal.valueOf(detalle.getCantidad());
                
                // Devolver stock al último lote de este producto
                List<com.gas.sistema_gas.Model.InventarioLote> lotes = inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(producto.getId());
                if (!lotes.isEmpty()) {
                    com.gas.sistema_gas.Model.InventarioLote ultimoLote = lotes.get(0);
                    ultimoLote.setCantidadActual(ultimoLote.getCantidadActual().add(cantidadDevolver));
                    inventarioLoteRepository.save(ultimoLote);
                }

                // Sincronizar el campo estático stock_llenos
                BigDecimal stockDisponible = inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(producto.getId())
                        .stream()
                        .map(l -> l.getCantidadActual() != null ? l.getCantidadActual() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                producto.setStockLlenos(stockDisponible);
                productoRepository.save(producto);
            }

            // Eliminar detalles, pagos y control de envases anteriores
            detalleRepository.deleteAll(detallesAnteriores);
            pedidoPagoRepository.deleteAll(pedidoPagoRepository.findByPedido_Id(pedido.getId()));
            controlEnvaseRepository.deleteByPedido_Id(pedido.getId());

        } else {
            // Lógica de Creación
            pedido = pedidoMapper.toEntity(createDto);
            pedido.setCodigo(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001"));
            pedido.setTipoVenta(createDto.tipoVenta() != null ? createDto.tipoVenta() : "DOMICILIO");
        }

        // Lógica de fecha límite de pago para créditos
        if (createDto.fechaLimitePago() != null) {
            LocalDateTime baseline = pedido.getFechaSolicitud() != null ? pedido.getFechaSolicitud() : LocalDateTime.now();
            LocalDateTime maxLimit = baseline.plusDays(2).withHour(23).withMinute(59).withSecond(59);
            if (createDto.fechaLimitePago().isAfter(maxLimit)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha límite de pago no puede superar los 2 días de plazo.");
            }
            pedido.setFechaLimitePago(createDto.fechaLimitePago());
        } else {
            pedido.setFechaLimitePago(null);
        }

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
                                createDto.telefonoCliente() == null ? "" : createDto.telefonoCliente(),
                                createDto.direccionCliente() == null ? "" : createDto.direccionCliente(),
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
            ClienteDTO.Create clienteDto = new ClienteDTO.Create(
                    createDto.nombreCliente(),
                    null,
                    createDto.telefonoCliente() == null ? "" : createDto.telefonoCliente(),
                    createDto.direccionCliente() == null ? "" : createDto.direccionCliente(),
                    createDto.referenciaCliente(),
                    null
            );
            Cliente nuevoCliente = clienteMapper.toEntity(clienteDto);
            cliente = clienteRepository.save(nuevoCliente);
        }

        Usuario usuario = usuarioRepository.findById(idUsuarioLogueado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe"));

        Empleado empleado = null;
        if (createDto.idEmpleado() != null) {
            empleado = empleadoRepository.findById(createDto.idEmpleado())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        } else if (usuario.getEmpleado() != null) {
            empleado = usuario.getEmpleado();
        }

        // 2. Asignar datos al pedido
        pedido.setCliente(cliente);
        pedido.setUsuario(usuario);
        pedido.setEmpleado(empleado);
        pedido.setFechaSolicitud(LocalDateTime.now());

        // Validación de motorizado para domicilio
        if ("DOMICILIO".equalsIgnoreCase(pedido.getTipoVenta()) && pedido.getEmpleado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un motorizado para ventas a domicilio.");
        }

        // Lógica de estados según tipo de venta
        if ("LOCAL".equalsIgnoreCase(pedido.getTipoVenta())) {
            pedido.setEstadoPedido("ENTREGADO");
            if (pedido.getFechaEntrega() == null) {
                pedido.setFechaEntrega(LocalDateTime.now());
            }
        } else {
            pedido.setEstadoPedido("PENDIENTE");
            if (createDto.estadoPedido() != null && !createDto.estadoPedido().isBlank()) {
                pedido.setEstadoPedido(createDto.estadoPedido());
                if ("ENTREGADO".equalsIgnoreCase(createDto.estadoPedido()) && pedido.getFechaEntrega() == null) {
                    pedido.setFechaEntrega(LocalDateTime.now());
                }
            }
        }

        pedido.setEstadoPago("PENDIENTE");
        // Inicializar valores monetarios para evitar errores de validación en el primer save
        pedido.setSubtotal(BigDecimal.ZERO);
        pedido.setMontoTotal(BigDecimal.ZERO);
        // Actualizar campos desde el DTO
        pedido.setObservaciones(createDto.observaciones());

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

            BigDecimal cantidadSolicitada = BigDecimal.valueOf(item.cantidad());
            
            // Calcular el stock real en caliente desde los lotes
            BigDecimal stockDisponible = inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(producto.getId())
                    .stream()
                    .map(l -> l.getCantidadActual() != null ? l.getCantidadActual() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (stockDisponible.compareTo(cantidadSolicitada) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para " + producto.getNombre());
            }

            // Descontar por PEPS en los lotes
            inventarioLoteService.descontarStockPorPEPS(producto.getId(), cantidadSolicitada);

            // Mantener sincronizado el campo estático stock_llenos de la tabla productos
            BigDecimal nuevoStockProducto = stockDisponible.subtract(cantidadSolicitada);
            producto.setStockLlenos(nuevoStockProducto);
            productoRepository.save(producto);

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedidoGuardado);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(precioUnitario);

            BigDecimal importeLinea = precioUnitario.multiply(BigDecimal.valueOf(item.cantidad()));
            montoAcumulado = montoAcumulado.add(importeLinea);

            detalleRepository.save(detalle);

            // Registrar préstamo de envases si corresponde
            if (item.cantidadPrestada() != null && item.cantidadPrestada() > 0) {
                if (item.cantidadPrestada() > item.cantidad()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La cantidad de envases prestados no puede ser mayor a la cantidad comprada.");
                }
                com.gas.sistema_gas.Model.ControlEnvase prestamo = new com.gas.sistema_gas.Model.ControlEnvase();
                prestamo.setPedido(pedidoGuardado);
                prestamo.setProducto(producto);
                prestamo.setCliente(pedidoGuardado.getCliente());
                prestamo.setCantidadPrestada(item.cantidadPrestada());
                prestamo.setEstado("PRESTADO");
                controlEnvaseRepository.save(prestamo);
            }
        }

        pedidoGuardado.setSubtotal(montoAcumulado);
        pedidoGuardado.setMontoTotal(montoAcumulado);

        List<PedidoDTO.PagoCreate> pagosDto = createDto.pagos();
        if ((pagosDto == null || pagosDto.isEmpty()) && createDto.idMetodoPago() != null) {
            pagosDto = List.of(new PedidoDTO.PagoCreate(
                    createDto.idMetodoPago(),
                    montoAcumulado,
                    createDto.numOperacion()
            ));
        }

        if (pagosDto == null) {
            pagosDto = List.of();
        }

        BigDecimal totalPagos = BigDecimal.ZERO;
        if (!pagosDto.isEmpty()) {
            for (PedidoDTO.PagoCreate pagoDto : pagosDto) {
                if (pagoDto == null || pagoDto.idMetodoPago() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada pago debe incluir un método de pago");
                }
                if (pagoDto.monto() == null || pagoDto.monto().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto de cada pago debe ser mayor a cero");
                }
                MetodoPago pagoMetodo = metodoPagoRepository.findById(pagoDto.idMetodoPago())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago no encontrado"));
                String numOperacionPago = pagoDto.numOperacion();
                if (numOperacionPago != null && numOperacionPago.isBlank()) {
                    numOperacionPago = null;
                }
                if (pagoMetodo.getNombre() != null && (
                        pagoMetodo.getNombre().equalsIgnoreCase("yape") ||
                        pagoMetodo.getNombre().equalsIgnoreCase("plin")
                ) && (numOperacionPago == null || numOperacionPago.isEmpty())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El número de operación es obligatorio para Yape y Plin");
                }

                PedidoPago pago = new PedidoPago();
                pago.setPedido(pedidoGuardado);
                pago.setMetodoPago(pagoMetodo);
                pago.setMonto(pagoDto.monto());
                pago.setNumOperacion(numOperacionPago);
                pedidoPagoRepository.save(pago);

                totalPagos = totalPagos.add(pagoDto.monto());
            }

            if (pedidoGuardado.getFechaLimitePago() == null) {
                boolean esPendienteDomicilio = "DOMICILIO".equalsIgnoreCase(pedidoGuardado.getTipoVenta()) 
                        && "PENDIENTE".equalsIgnoreCase(pedidoGuardado.getEstadoPedido());
                
                if (esPendienteDomicilio && totalPagos.compareTo(BigDecimal.ZERO) == 0) {
                    // Permitido: no hay pagos registrados aún porque se cobrará al entregar
                } else if (totalPagos.compareTo(montoAcumulado) != 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El total de los pagos debe ser igual al monto total de la venta al contado.");
                }
            } else {
                if (totalPagos.compareTo(montoAcumulado) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El total de los pagos no puede superar el monto total de la venta");
                }
            }

            PedidoDTO.PagoCreate pagoPrincipal = pagosDto.get(0);
            MetodoPago metodoPagoPrincipal = metodoPagoRepository.findById(pagoPrincipal.idMetodoPago()).orElse(null);
            pedidoGuardado.setMetodoPago(metodoPagoPrincipal);
            pedidoGuardado.setNumOperacion(pagoPrincipal.numOperacion());

            if (totalPagos.compareTo(montoAcumulado) == 0) {
                pedidoGuardado.setEstadoPago("PAGADO");
            } else {
                pedidoGuardado.setEstadoPago("CREDITO");
            }
        } else {
            if (pedidoGuardado.getFechaLimitePago() != null) {
                pedidoGuardado.setEstadoPago("CREDITO");
            } else {
                pedidoGuardado.setEstadoPago("PENDIENTE");
            }
        }

        return mapToSimpleResponse(pedidoRepository.save(pedidoGuardado));
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

        return mapToSimpleResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse updateEstadoPedido(Long id, String nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del pedido es obligatorio");
        }

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        String estadoActual = pedido.getEstadoPedido() != null ? pedido.getEstadoPedido().trim().toUpperCase() : "PENDIENTE";
        String estadoNormalizado = nuevoEstado.trim().toUpperCase();
        List<String> estadosValidos = List.of("PENDIENTE", "ACEPTADO", "CARGADO", "EN_CAMINO", "EN_DOMICILIO", "ENTREGADO", "ANULADO");
        if (!estadosValidos.contains(estadoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pedido inválido: " + nuevoEstado);
        }

        List<String> secuencia = List.of("PENDIENTE", "ACEPTADO", "CARGADO", "EN_CAMINO", "EN_DOMICILIO", "ENTREGADO");
        int indiceActual = secuencia.indexOf(estadoActual);
        int indiceNuevo = secuencia.indexOf(estadoNormalizado);

        if (estadoNormalizado.equals("ANULADO")) {
            if (!"PENDIENTE".equals(estadoActual)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido solo puede cancelarse desde PENDIENTE");
            }
        } else if (indiceActual == -1 || indiceNuevo == -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pedido no permitido para este flujo");
        } else if (indiceNuevo != indiceActual + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del pedido no puede retroceder");
        }

        pedido.setEstadoPedido(estadoNormalizado);

        if ("ENTREGADO".equals(estadoNormalizado)) {
            pedido.setFechaEntrega(pedido.getFechaEntrega() != null ? pedido.getFechaEntrega() : LocalDateTime.now());
            pedido.setEstadoPago("PAGADO");
        }

        return mapToSimpleResponse(pedidoRepository.save(pedido));
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
            BigDecimal cantidadADevolver = BigDecimal.valueOf(detalle.getCantidad());
            
            // Devolver stock al último lote de este producto
            List<com.gas.sistema_gas.Model.InventarioLote> lotes = inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(producto.getId());
            if (!lotes.isEmpty()) {
                com.gas.sistema_gas.Model.InventarioLote ultimoLote = lotes.get(0);
                ultimoLote.setCantidadActual(ultimoLote.getCantidadActual().add(cantidadADevolver));
                inventarioLoteRepository.save(ultimoLote);
            }

            // Sincronizar el campo estático stock_llenos
            BigDecimal stockDisponible = inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(producto.getId())
                    .stream()
                    .map(l -> l.getCantidadActual() != null ? l.getCantidadActual() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            producto.setStockLlenos(stockDisponible);
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
        return mapToSimpleResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse findByIdAndEmpleadoId(Long id, Long empleadoId) {
        Pedido pedido = pedidoRepository.findByIdAndEmpleadoId(id, empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no asignado a este motorizado"));
        return mapToSimpleResponse(pedido);
    }

    @Override
    @Transactional
    public boolean existsByIdAndEmpleadoId(Long id, Long empleadoId) {
        return pedidoRepository.findByIdAndEmpleadoId(id, empleadoId).isPresent();
    }

    @Override
    @Transactional
    public PedidoDTO.EditResponse getEditData(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        List<DetallePedido> detalles = detalleRepository.findByPedido_Id(id);
        List<PedidoPago> pagos = pedidoPagoRepository.findByPedido_Id(id);
        List<com.gas.sistema_gas.Model.ControlEnvase> prestamos = controlEnvaseRepository.findByPedido_Id(id);

        List<PedidoDTO.DetalleResponse> detallesDto = detalles.stream().map(det -> {
            Integer cantPrestada = prestamos.stream()
                .filter(p -> p.getProducto().getId().equals(det.getProducto().getId()))
                .map(com.gas.sistema_gas.Model.ControlEnvase::getCantidadPrestada)
                .findFirst()
                .orElse(0);
            return new PedidoDTO.DetalleResponse(
                det.getProducto().getId(),
                det.getProducto().getNombre(),
                det.getCantidad(),
                det.getPrecioUnitario(),
                cantPrestada
            );
        }).collect(Collectors.toList());

        List<PedidoDTO.PagoResponse> pagosDto = pagos.stream().map(pago ->
            new PedidoDTO.PagoResponse(
                pago.getMetodoPago().getId(),
                pago.getMetodoPago().getNombre(),
                pago.getMonto(),
                pago.getNumOperacion()
            )
        ).collect(Collectors.toList());

        return new PedidoDTO.EditResponse(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getCliente().getId(),
            pedido.getCliente().getDni(),
            pedido.getCliente().getNombre(),
            pedido.getCliente().getTelefono(),
            pedido.getCliente().getDireccion(),
            pedido.getCliente().getReferencia(),
            pedido.getEmpleado() != null ? pedido.getEmpleado().getId() : null,
            pedido.getObservaciones(),
            pedido.getEstadoPedido(),
            pedido.getTipoVenta(),
            pedido.getFechaLimitePago(),
            detallesDto,
            pagosDto
        );
    }

    @Override
    @Transactional
    public List<PedidoDTO.SimpleResponse> listEntregadosByEmpleadoId(Long empleadoId) {
        return pedidoRepository.findEntregadosByEmpleadoId(empleadoId).stream()
                .map(this::mapToSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Page<PedidoDTO.SimpleResponse> listEntregadosByEmpleadoIdWithFilters(Long empleadoId, String buscar, String metodoPago, LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable) {
        Page<Pedido> pedidosPage = pedidoRepository.findEntregadosByEmpleadoIdWithFilters(empleadoId, buscar, metodoPago, fechaInicio, fechaFin, pageable);
        return pedidosPage.map(this::mapToSimpleResponse);
    }
}