package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
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
import com.gas.sistema_gas.Model.InventarioLote;
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

    private static final String MENSAJE_PAGO_REQUIERE_REEMBOLSO =
            "El pedido tiene pagos registrados y requiere un reembolso antes de poder cancelarse";

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
    private com.gas.sistema_gas.Repository.EvidenciaRepository evidenciaRepository;
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

    /**
     * Helper central de bloqueo pesimista de Productos.
     * Ordena IDs ASC, elimina duplicados, bloquea en una sola llamada y
     * verifica que todos los productos existan.
     */
    private Map<Long, Producto> bloquearProductosPorIds(Collection<Long> idsProductos) {
        if (idsProductos == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de productos no puede ser nula");
        }

        // Rechazar explicitamente cualquier ID null antes de continuar
        for (Long id : idsProductos) {
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La lista de productos contiene un ID nulo");
            }
        }

        Set<Long> idsUnicos = new LinkedHashSet<>(idsProductos);

        if (idsUnicos.isEmpty()) {
            return new java.util.HashMap<>();
        }

        List<Long> idsOrdenados = idsUnicos.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        List<Producto> productosBloqueados = productoRepository.findAllByIdInForUpdate(idsOrdenados);

        Map<Long, Producto> mapa = productosBloqueados.stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        if (mapa.size() != idsOrdenados.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uno o más productos no fueron encontrados");
        }

        return mapa;
    }

    private Set<Long> extraerIdsDeDetallesCreate(List<PedidoDTO.DetalleCreate> detalles) {
        return detalles.stream()
                .filter(d -> d != null && d.idProducto() != null)
                .map(PedidoDTO.DetalleCreate::idProducto)
                .collect(Collectors.toSet());
    }

    private Set<Long> extraerIdsDeDetallesPedido(List<DetallePedido> detalles) {
        return detalles.stream()
                .filter(d -> d != null && d.getProducto() != null && d.getProducto().getId() != null)
                .map(d -> d.getProducto().getId())
                .collect(Collectors.toSet());
    }

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

        // Obtener el método de pago desde pedido_pagos
        String metodoPagoNombre = null;
        java.util.List<PedidoPago> pagosPedido = pedidoPagoRepository.findByPedido_Id(pedido.getId());
        if (pagosPedido != null && !pagosPedido.isEmpty()) {
            PedidoPago primerPago = pagosPedido.get(0);
            if (primerPago.getMetodoPago() != null) {
                metodoPagoNombre = primerPago.getMetodoPago().getNombre();
            }
        }

        // Cargar evidencias del pedido (incluyendo PAGO y VUELTO)
        java.util.List<PedidoDTO.EvidenciaResponse> evidencias = pagosPedido.stream()
                .flatMap(pago -> {
                    java.util.List<com.gas.sistema_gas.Model.Evidencia> evs = evidenciaRepository.findByPedidoPago_Id(pago.getId());
                    return evs.stream();
                })
                .map(ev -> new PedidoDTO.EvidenciaResponse(
                        ev.getId(),
                        ev.getUrlImagen(),
                        ev.getTipoEvidencia()
                ))
                .collect(java.util.stream.Collectors.toList());
        
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
            response.fechaLimitePago(),
            evidencias
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
        // FASE 2B: La edición de pedidos existentes está temporalmente deshabilitada
        if (createDto.idPedido() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La edición de pedidos existentes está temporalmente deshabilitada"
            );
        }

        boolean hayDetallesGas = createDto.detalles() != null && !createDto.detalles().isEmpty();
        boolean hayEnvaseVenta = createDto.envaseMovimientos() != null 
            && "VENTA".equalsIgnoreCase(createDto.tipoMovimientoEnvase())
            && createDto.envaseMovimientos().stream().anyMatch(e -> e.cantidad() != null && e.cantidad() > 0);

        if (!hayDetallesGas && !hayEnvaseVenta) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe agregar al menos un detalle de venta");
        }

        // Lógica de Creación
        Pedido pedido = pedidoMapper.toEntity(createDto);
        pedido.setCodigo(correlativoService.incrementarYObtenerCodigo("VENTA_NOTA", "NV001"));
        pedido.setTipoVenta(createDto.tipoVenta() != null ? createDto.tipoVenta() : "DOMICILIO");
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
                                null,
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
                    null,
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
            if (!"CANJE".equalsIgnoreCase(createDto.tipoMovimientoEnvase())
                    && createDto.estadoPedido() != null && !createDto.estadoPedido().isBlank()) {
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
        List<PedidoDTO.EnvaseMovimientoCreate> envaseMvts = createDto.envaseMovimientos();
        String tipoMov = createDto.tipoMovimientoEnvase();
        boolean usarPrestamoLegado = tipoMov == null || tipoMov.isBlank();

        if (createDto.detalles() != null) {
            // FASE 4B-2A: Validar todos los detalles antes de procesar
            for (PedidoDTO.DetalleCreate item : createDto.detalles()) {
                if (item == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detalle de venta inválido");
                }
                if (item.idProducto() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del producto no puede ser nulo");
                }
                if (item.cantidad() == null || item.cantidad() < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La cantidad debe ser mayor a cero");
                }
            }
        }

        Map<Long, Integer> cantidadesCanjePorProducto = new java.util.HashMap<>();
        if ("CANJE".equalsIgnoreCase(tipoMov)) {
            if (envaseMvts == null || envaseMvts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El canje requiere al menos un movimiento de envase");
            }

            Map<Long, Integer> cantidadesDetallePorProducto = new java.util.HashMap<>();
            for (PedidoDTO.DetalleCreate detalle : createDto.detalles()) {
                cantidadesDetallePorProducto.merge(detalle.idProducto(), detalle.cantidad(), Integer::sum);
            }

            for (PedidoDTO.EnvaseMovimientoCreate movimiento : envaseMvts) {
                if (movimiento == null || movimiento.idProducto() == null
                        || movimiento.cantidad() == null || movimiento.cantidad() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El movimiento de canje debe incluir producto y cantidad mayor a cero");
                }
                cantidadesCanjePorProducto.merge(movimiento.idProducto(), movimiento.cantidad(), Integer::sum);
            }

            for (Map.Entry<Long, Integer> canje : cantidadesCanjePorProducto.entrySet()) {
                int cantidadDetalle = cantidadesDetallePorProducto.getOrDefault(canje.getKey(), 0);
                if (cantidadDetalle == 0 || canje.getValue() != cantidadDetalle) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La cantidad de envases canjeados debe corresponder a la venta del producto");
                }
            }
        }

        Set<Long> idsProductosDetalles = createDto.detalles() != null
                ? extraerIdsDeDetallesCreate(createDto.detalles())
                : new LinkedHashSet<>();
        Set<Long> idsProductosEnvase = envaseMvts != null
                ? envaseMvts.stream()
                        .filter(e -> e != null && e.idProducto() != null && e.cantidad() != null && e.cantidad() > 0)
                        .map(PedidoDTO.EnvaseMovimientoCreate::idProducto)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>();
        Set<Long> idsProductosABloquear = new LinkedHashSet<>(idsProductosDetalles);
        idsProductosABloquear.addAll(idsProductosEnvase);
        Map<Long, Producto> productosBloqueados = bloquearProductosPorIds(idsProductosABloquear);

        List<Long> idsProductosDetallesOrdenados = idsProductosDetalles.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        List<InventarioLote> lotesBloqueados = inventarioLoteRepository
                .findByProductoIdsForUpdate(idsProductosDetallesOrdenados);
        Map<Long, BigDecimal> stockActualPorProducto = new java.util.HashMap<>();
        for (InventarioLote lote : lotesBloqueados) {
            BigDecimal cantidadActual = lote.getCantidadActual() != null
                    ? lote.getCantidadActual()
                    : BigDecimal.ZERO;
            stockActualPorProducto.merge(
                    lote.getProducto().getId(), cantidadActual, BigDecimal::add);
        }

        if (createDto.detalles() != null) {
            for (PedidoDTO.DetalleCreate item : createDto.detalles()) {
                Producto producto = productosBloqueados.get(item.idProducto());

                BigDecimal precioUnitario = item.precioUnitario() != null && item.precioUnitario().compareTo(BigDecimal.ZERO) > 0
                        ? item.precioUnitario()
                        : producto.getPrecioVenta();

                BigDecimal cantidadSolicitada = BigDecimal.valueOf(item.cantidad());
                
                // Calcular con los lotes bloqueados después de bloquear Productos.
                BigDecimal stockRealLotes = stockActualPorProducto
                        .getOrDefault(producto.getId(), BigDecimal.ZERO);
                BigDecimal stockReservado = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;
                BigDecimal stockDisponible = stockRealLotes.subtract(stockReservado);

                if (stockDisponible.compareTo(cantidadSolicitada) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Stock insuficiente para " + producto.getNombre());
                }

                // Si es venta LOCAL (ENTREGADO directo): descontar stock real y NO reservar
                // Si es DOMICILIO (PENDIENTE): solo reservar stock
                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(pedidoGuardado);
                detalle.setProducto(producto);
                detalle.setCantidad(item.cantidad());
                detalle.setPrecioUnitario(precioUnitario);
                detalle.setEsEnvaseVendido(false);
                if ("CANJE".equalsIgnoreCase(tipoMov)
                        && "DOMICILIO".equalsIgnoreCase(pedido.getTipoVenta())) {
                    detalle.setCantidadCanje(item.cantidad());
                }

                BigDecimal importeLinea = precioUnitario.multiply(BigDecimal.valueOf(item.cantidad()));
                montoAcumulado = montoAcumulado.add(importeLinea);

                // Guardar detalle primero para obtener idDetalle
                DetallePedido detalleGuardado = detalleRepository.save(detalle);

                if ("LOCAL".equalsIgnoreCase(pedido.getTipoVenta())) {
                    // Descontar stock real por PEPS con trazabilidad
                    inventarioLoteService.descontarStockPorPEPS(detalleGuardado);
                    // Sincronizar stock_llenos desde lotes
                    BigDecimal stockActualLotes = stockRealLotes.subtract(cantidadSolicitada);
                    stockActualPorProducto.put(producto.getId(), stockActualLotes);
                    producto.setStockLlenos(stockActualLotes);
                } else {
                    // DOMICILIO: solo RESERVAR stock (sumar a stock_reservado)
                    BigDecimal nuevoReservado = stockReservado.add(cantidadSolicitada);
                    producto.setStockReservado(nuevoReservado);
                }
                productoRepository.save(producto);

                // Registrar préstamo de envases si corresponde
                if (usarPrestamoLegado && item.cantidadPrestada() != null && item.cantidadPrestada() > 0) {
                    if (item.cantidadPrestada() > item.cantidad()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "La cantidad de envases prestados no puede ser mayor a la cantidad comprada.");
                    }
                    Integer stockVaciosActual = producto.getStockVacios() != null ? producto.getStockVacios() : 0;
                    if (stockVaciosActual < item.cantidadPrestada()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Stock de envases insuficiente para " + producto.getNombre());
                    }
                    producto.setStockVacios(stockVaciosActual - item.cantidadPrestada());
                    productoRepository.save(producto);

                    com.gas.sistema_gas.Model.ControlEnvase prestamo = new com.gas.sistema_gas.Model.ControlEnvase();
                    prestamo.setPedido(pedidoGuardado);
                    prestamo.setProducto(producto);
                    prestamo.setCliente(pedidoGuardado.getCliente());
                    prestamo.setCantidadPrestada(item.cantidadPrestada());
                    prestamo.setEstado("PRESTADO");
                    LocalDateTime fechaPrestamo = LocalDateTime.now();
                    prestamo.setFechaPrestamo(fechaPrestamo);
                    prestamo.setFechaLimiteDevolucion(fechaPrestamo.toLocalDate().plusDays(3));
                    prestamo.setFechaDevolucion(null);
                    prestamo.setTipoPrestamo("NORMAL");
                    controlEnvaseRepository.save(prestamo);
                }
            }
        }

        // =====================================================================
        // PROCESAR MOVIMIENTO DE ENVASES (nuevo modal de Movimiento de Envases)
        // =====================================================================
        if (envaseMvts != null && !envaseMvts.isEmpty()) {
            for (PedidoDTO.EnvaseMovimientoCreate envMvt : envaseMvts) {
                if (envMvt.idProducto() == null || envMvt.cantidad() == null || envMvt.cantidad() <= 0) {
                    continue;
                }

                Producto productoEnvase = productosBloqueados.get(envMvt.idProducto());
                if (productoEnvase == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto de envase no encontrado");
                }

                if ("VENTA".equalsIgnoreCase(tipoMov)) {
                    Integer stockVaciosActual = productoEnvase.getStockVacios() != null
                            ? productoEnvase.getStockVacios()
                            : 0;
                    if (stockVaciosActual < envMvt.cantidad()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Stock de envases insuficiente para " + productoEnvase.getNombre());
                    }
                    productoEnvase.setStockVacios(stockVaciosActual - envMvt.cantidad());
                    productoRepository.save(productoEnvase);

                    // Venta de envases: agregar como detalle de pedido (suma al total)
                    BigDecimal precioUnitarioEnvase = envMvt.precioUnitario() != null
                        ? envMvt.precioUnitario()
                        : productoEnvase.getPrecioVenta();

                    DetallePedido detalleEnvase = new DetallePedido();
                    detalleEnvase.setPedido(pedidoGuardado);
                    detalleEnvase.setProducto(productoEnvase);
                    detalleEnvase.setCantidad(envMvt.cantidad());
                    detalleEnvase.setPrecioUnitario(precioUnitarioEnvase);
                    detalleEnvase.setEsEnvaseVendido(true);
                    detalleRepository.save(detalleEnvase);

                    BigDecimal importeLineaEnvase = precioUnitarioEnvase.multiply(BigDecimal.valueOf(envMvt.cantidad()));
                    montoAcumulado = montoAcumulado.add(importeLineaEnvase);

                } else if ("CANJE".equalsIgnoreCase(tipoMov)) {
                    if (!Boolean.TRUE.equals(productoEnvase.getRequiereEnvase())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "El producto no requiere envase para registrar un canje");
                    }
                    if ("LOCAL".equalsIgnoreCase(pedido.getTipoVenta())) {
                        Integer stockVaciosActual = productoEnvase.getStockVacios() != null
                                ? productoEnvase.getStockVacios()
                                : 0;
                        productoEnvase.setStockVacios(Math.addExact(stockVaciosActual, envMvt.cantidad()));
                        productoRepository.save(productoEnvase);
                    }

                } else if ("PRESTAMO".equalsIgnoreCase(tipoMov)) {
                    String tipoPrestamo = envMvt.tipoPrestamo() == null || envMvt.tipoPrestamo().isBlank()
                            ? "NORMAL"
                            : envMvt.tipoPrestamo().trim().toUpperCase(Locale.ROOT);
                    if ("LEGADO".equals(tipoPrestamo)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "No se pueden crear préstamos de tipo LEGADO");
                    }
                    if (!"NORMAL".equals(tipoPrestamo) && !"ESPECIAL".equals(tipoPrestamo)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Tipo de préstamo inválido");
                    }

                    LocalDateTime fechaPrestamo = LocalDateTime.now();
                    LocalDate fechaLimiteDevolucion = null;
                    if ("NORMAL".equals(tipoPrestamo)) {
                        LocalDate fechaInicial = fechaPrestamo.toLocalDate();
                        LocalDate fechaMaxima = fechaInicial.plusDays(3);
                        if (envMvt.fechaLimiteDevolucion() == null || envMvt.fechaLimiteDevolucion().isBlank()) {
                            fechaLimiteDevolucion = fechaMaxima;
                        } else {
                            try {
                                fechaLimiteDevolucion = LocalDate.parse(envMvt.fechaLimiteDevolucion());
                            } catch (DateTimeParseException error) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "La fecha límite de devolución no tiene un formato válido");
                            }
                            if (fechaLimiteDevolucion.isBefore(fechaInicial)
                                    || fechaLimiteDevolucion.isAfter(fechaMaxima)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "La fecha límite de devolución debe estar entre hoy y los próximos 3 días");
                            }
                        }
                    } else {
                        if (!Boolean.TRUE.equals(pedidoGuardado.getCliente().getPrestamoIlimitado())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "El cliente no está autorizado para préstamos especiales");
                        }
                        if (envMvt.fechaLimiteDevolucion() != null && !envMvt.fechaLimiteDevolucion().isBlank()) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "El préstamo especial no utiliza fecha límite de devolución");
                        }
                    }

                    // Préstamo de envases: registrar en control_envase (no suma al total)
                    Integer stockVaciosActual = productoEnvase.getStockVacios() != null
                            ? productoEnvase.getStockVacios()
                            : 0;
                    if (stockVaciosActual < envMvt.cantidad()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Stock de envases insuficiente para " + productoEnvase.getNombre());
                    }
                    productoEnvase.setStockVacios(stockVaciosActual - envMvt.cantidad());
                    productoRepository.save(productoEnvase);

                    com.gas.sistema_gas.Model.ControlEnvase prestamoEnvase = new com.gas.sistema_gas.Model.ControlEnvase();
                    prestamoEnvase.setPedido(pedidoGuardado);
                    prestamoEnvase.setProducto(productoEnvase);
                    prestamoEnvase.setCliente(pedidoGuardado.getCliente());
                    prestamoEnvase.setCantidadPrestada(envMvt.cantidad());
                    prestamoEnvase.setEstado("PRESTADO");
                    prestamoEnvase.setFechaPrestamo(fechaPrestamo);
                    prestamoEnvase.setFechaLimiteDevolucion(fechaLimiteDevolucion);
                    prestamoEnvase.setFechaDevolucion(null);
                    prestamoEnvase.setTipoPrestamo(tipoPrestamo);

                    controlEnvaseRepository.save(prestamoEnvase);

                    // Descontar stock_vacios del producto cuando se presta un envase
                }
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
                if (pagoDto == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pago inválido en la solicitud");
                }
                if (pagoDto.idMetodoPago() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del método de pago no puede ser nulo");
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
            recalcularEstadoPago(pedido);
        }

        return mapToSimpleResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoDTO.SimpleResponse updateEstadoPedido(Long id, String nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del pedido es obligatorio");
        }

        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        String estadoActual = pedido.getEstadoPedido() != null ? pedido.getEstadoPedido().trim().toUpperCase() : "PENDIENTE";
        String estadoNormalizado = nuevoEstado.trim().toUpperCase();
        List<String> estadosValidos = List.of("PENDIENTE", "ACEPTADO", "CARGADO", "EN_CAMINO", "EN_DOMICILIO", "ENTREGADO", "ANULADO", "RECHAZADO", "CLIENTE_AUSENTE", "CANCELADO");
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
            validarPedidoSinPagosPositivos(pedido.getId());
        } else if (estadoNormalizado.equals("RECHAZADO") || estadoNormalizado.equals("CANCELADO")) {
            // Estados de incidencia - permitir desde EN_DOMICILIO
            if (!"EN_DOMICILIO".equals(estadoActual)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido solo puede marcarse como " + estadoNormalizado + " desde EN_DOMICILIO");
            }
            validarPedidoSinPagosPositivos(pedido.getId());
        } else if (estadoNormalizado.equals("CLIENTE_AUSENTE")) {
            // CLIENTE_AUSENTE solo desde EN_DOMICILIO
            if (!"EN_DOMICILIO".equals(estadoActual)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido solo puede marcarse como CLIENTE_AUSENTE desde EN_DOMICILIO");
            }
        } else if ("CLIENTE_AUSENTE".equals(estadoActual) && "ACEPTADO".equals(estadoNormalizado)) {
            // Permitir reinicio del flujo desde CLIENTE_AUSENTE hacia ACEPTADO
            // FASE 2B: Validar que el stock del pedido fue devuelto exactamente antes de reservar
            inventarioLoteService.validarStockDevueltoParaReactivacion(pedido.getId());
            // Reservar stock antes de cambiar el estado
            reservarStockParaReactivacion(pedido);
        } else if (indiceActual == -1 || indiceNuevo == -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pedido no permitido para este flujo");
        } else if (indiceNuevo != indiceActual + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del pedido no puede retroceder");
        }

        // ===== CONTROL DE INVENTARIO SEGÚN ESTADOS =====

        if ("ANULADO".equals(estadoNormalizado)) {
            anularPedidoPendienteConReversion(pedido);
        } else {
            pedido.setEstadoPedido(estadoNormalizado);

                // Si pasa a CARGADO: descuenta stock real y libera reserva (idempotente)
            if ("CARGADO".equals(estadoNormalizado) && !"CARGADO".equals(estadoActual)
                    && !"EN_CAMINO".equals(estadoActual) && !"EN_DOMICILIO".equals(estadoActual)
                    && !"ENTREGADO".equals(estadoActual)) {

                // FASE 4B-2A: Orden de bloqueo: Pedido (ya bloqueado) → Productos → Lotes PEPS
                List<DetallePedido> detalles = detalleRepository.findByPedido_Id(pedido.getId());
                List<DetallePedido> detallesContenido = detalles.stream()
                        .filter(detalle -> !Boolean.TRUE.equals(detalle.getEsEnvaseVendido()))
                        .toList();
                Set<Long> idsDetalles = extraerIdsDeDetallesPedido(detallesContenido);
                Map<Long, Producto> productosBloqueados = bloquearProductosPorIds(idsDetalles);

                for (DetallePedido detalle : detallesContenido) {
                    Long idProd = detalle.getProducto() != null ? detalle.getProducto().getId() : null;
                    if (idProd == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detalle sin producto asociado");
                    }
                    Producto producto = productosBloqueados.get(idProd);
                    if (producto == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
                    }
                    BigDecimal cant = BigDecimal.valueOf(detalle.getCantidad());

                    // Descontar stock real por PEPS con trazabilidad
                    inventarioLoteService.descontarStockPorPEPS(detalle);

                    // Sincronizar stock_llenos desde lotes (usando el Producto bloqueado)
                    BigDecimal stockActualLotes = inventarioLoteRepository.sumCantidadActualByProductoId(producto.getId());
                    producto.setStockLlenos(stockActualLotes);

                    // Restar la reserva (libera el stock_reservado)
                    BigDecimal reservadoActual = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;
                    if (reservadoActual.compareTo(cant) < 0) {
                        // FASE 4B-2A: No ocultar inconsistencias. Lanzar CONFLICT para que el rollback revierta el descuento PEPS.
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "La reserva del producto es menor que la cantidad a cargar: " + producto.getNombre());
                    }
                    producto.setStockReservado(reservadoActual.subtract(cant));

                    productoRepository.save(producto);
                }
            }

            if ("ENTREGADO".equals(estadoNormalizado)) {
                pedido.setFechaEntrega(pedido.getFechaEntrega() != null ? pedido.getFechaEntrega() : LocalDateTime.now());
                recalcularEstadoPago(pedido);
            }
        }

        return mapToSimpleResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public void desasignarPedido(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        // Solo se puede desasignar si está en estado PENDIENTE
        if (!"PENDIENTE".equalsIgnoreCase(pedido.getEstadoPedido())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Solo se puede desasignar un pedido en estado PENDIENTE");
        }

        // Quitar la asignación del empleado (motorizado)
        pedido.setEmpleado(null);
        pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        String estadoActual = pedido.getEstadoPedido() != null ? pedido.getEstadoPedido().trim().toUpperCase() : "PENDIENTE";

        if ("ANULADO".equals(estadoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido ya está anulado");
        }

        if (!"PENDIENTE".equals(estadoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede anular un pedido en estado PENDIENTE");
        }

        validarPedidoSinPagosPositivos(pedido.getId());
        anularPedidoPendienteConReversion(pedido);
        pedidoRepository.save(pedido);
    }

    private void validarPedidoSinPagosPositivos(Long idPedido) {
        if (pedidoPagoRepository.existsByPedido_IdAndMontoGreaterThan(idPedido, BigDecimal.ZERO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MENSAJE_PAGO_REQUIERE_REEMBOLSO);
        }
    }

    /**
     * Revierte una anulación de pedido pendiente. El pedido debe llegar ya bloqueado por el llamador.
     */
    private void anularPedidoPendienteConReversion(Pedido pedido) {
        List<DetallePedido> detalles = detalleRepository.findByPedido_Id(pedido.getId());
        Map<Long, BigDecimal> reservasPorProducto = new java.util.HashMap<>();
        Map<Long, Integer> vaciosAReponerPorProducto = new java.util.HashMap<>();
        for (DetallePedido detalle : detalles) {
            Long idProducto = detalle.getProducto() != null ? detalle.getProducto().getId() : null;
            if (idProducto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detalle sin producto asociado");
            }
            if (Boolean.TRUE.equals(detalle.getEsEnvaseVendido())) {
                vaciosAReponerPorProducto.merge(idProducto, detalle.getCantidad(), Math::addExact);
            } else {
                reservasPorProducto.merge(idProducto, BigDecimal.valueOf(detalle.getCantidad()), BigDecimal::add);
            }
        }

        List<Long> idsProductosPrestamos = controlEnvaseRepository.findProductoIdsByPedidoId(pedido.getId());
        Set<Long> idsProductosAnulacion = new LinkedHashSet<>(reservasPorProducto.keySet());
        idsProductosAnulacion.addAll(vaciosAReponerPorProducto.keySet());
        idsProductosAnulacion.addAll(idsProductosPrestamos);
        Map<Long, Producto> productosBloqueados = bloquearProductosPorIds(idsProductosAnulacion);

        List<com.gas.sistema_gas.Model.ControlEnvase> controlesEnvase = controlEnvaseRepository
                .findByPedido_IdForUpdate(pedido.getId());
        for (com.gas.sistema_gas.Model.ControlEnvase control : controlesEnvase) {
            int cantidadPrestada = control.getCantidadPrestada() != null ? control.getCantidadPrestada() : 0;
            int cantidadDevuelta = control.getCantidadDevuelta() != null ? control.getCantidadDevuelta() : 0;
            int pendiente = Math.max(0, cantidadPrestada - cantidadDevuelta);

            if (pendiente > 0) {
                if (control.getProducto() == null || control.getProducto().getId() == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "El préstamo pendiente no tiene producto asociado");
                }
                Long idProducto = control.getProducto().getId();
                if (!productosBloqueados.containsKey(idProducto)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "El producto del préstamo pendiente no fue bloqueado");
                }
                vaciosAReponerPorProducto.merge(idProducto, pendiente, Math::addExact);
            }
        }

        for (Map.Entry<Long, BigDecimal> entry : reservasPorProducto.entrySet()) {
            Producto producto = productosBloqueados.get(entry.getKey());
            BigDecimal reservadoActual = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;
            if (reservadoActual.compareTo(entry.getValue()) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La reserva del producto es menor que la cantidad del pedido");
            }
        }

        for (Map.Entry<Long, BigDecimal> entry : reservasPorProducto.entrySet()) {
            Producto producto = productosBloqueados.get(entry.getKey());
            BigDecimal reservadoActual = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;
            producto.setStockReservado(reservadoActual.subtract(entry.getValue()));
            productoRepository.save(producto);
        }

        for (Map.Entry<Long, Integer> entry : vaciosAReponerPorProducto.entrySet()) {
            Producto producto = productosBloqueados.get(entry.getKey());
            int stockVaciosActual = producto.getStockVacios() != null ? producto.getStockVacios() : 0;
            producto.setStockVacios(Math.addExact(stockVaciosActual, entry.getValue()));
            productoRepository.save(producto);
        }

        controlEnvaseRepository.deleteByPedido_Id(pedido.getId());

        pedido.setEstadoPedido("ANULADO");
    }

    /**
     * Reserva stock para reactivar un pedido desde CLIENTE_AUSENTE hacia ACEPTADO.
     * Verifica que el stock disponible (stock real de lotes - stock reservado) sea suficiente.
     * No modifica InventarioLote, no modifica stockLlenos, no llama descontarStockPorPEPS.
     * Lanza CONFLICT si no hay stock disponible suficiente.
     */
    private void reservarStockParaReactivacion(Pedido pedido) {
        List<DetallePedido> detalles = detalleRepository.findByPedido_Id(pedido.getId());

        // FASE 4B-2A: Bloquear todos los productos únicos en una sola llamada
        List<DetallePedido> detallesContenido = detalles.stream()
                .filter(detalle -> !Boolean.TRUE.equals(detalle.getEsEnvaseVendido()))
                .toList();
        Set<Long> idsDetalles = extraerIdsDeDetallesPedido(detallesContenido);
        Map<Long, Producto> productosBloqueados = bloquearProductosPorIds(idsDetalles);

        for (DetallePedido detalle : detallesContenido) {
            Long idProd = detalle.getProducto() != null ? detalle.getProducto().getId() : null;
            if (idProd == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detalle sin producto asociado");
            }
            Producto producto = productosBloqueados.get(idProd);
            if (producto == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
            }
            BigDecimal cantidadAReservar = BigDecimal.valueOf(detalle.getCantidad());

            // Obtener la suma real de InventarioLote.cantidadActual del producto
            BigDecimal stockRealLotes = inventarioLoteRepository.sumCantidadActualByProductoId(producto.getId());

            // Obtener Producto.stockReservado, usando cero si es null
            BigDecimal stockReservado = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;

            // Calcular stock disponible
            BigDecimal stockDisponible = stockRealLotes.subtract(stockReservado);

            // Si stockDisponible es menor que la cantidad del detalle, lanzar excepción
            if (stockDisponible.compareTo(cantidadAReservar) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para reactivar el pedido: " + producto.getNombre());
            }

            // Si existe stock, sumar la cantidad a Producto.stockReservado
            BigDecimal nuevoReservado = stockReservado.add(cantidadAReservar);
            producto.setStockReservado(nuevoReservado);
            productoRepository.save(producto);
        }
    }

    /**
     * Libera el stockReservado de todos los detalles de un pedido.
     * No toca InventarioLote, no llama descontarStockPorPEPS, no modifica stockLlenos.
     * Lanza CONFLICT si la reserva es menor que la cantidad del detalle.
     */
    private void liberarReservaPedido(Pedido pedido) {
        List<DetallePedido> detalles = detalleRepository.findByPedido_Id(pedido.getId());

        // FASE 4B-2A: Bloquear todos los productos únicos en una sola llamada
        Set<Long> idsDetalles = extraerIdsDeDetallesPedido(detalles);
        Map<Long, Producto> productosBloqueados = bloquearProductosPorIds(idsDetalles);

        // FASE 4B-2A: Fase de validación completa primero
        Map<Long, BigDecimal> cantidadesPorProducto = new java.util.HashMap<>();
        for (DetallePedido detalle : detalles) {
            Long idProd = detalle.getProducto() != null ? detalle.getProducto().getId() : null;
            if (idProd == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detalle sin producto asociado");
            }
            BigDecimal cant = BigDecimal.valueOf(detalle.getCantidad());
            cantidadesPorProducto.merge(idProd, cant, BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productosBloqueados.get(entry.getKey());
            if (producto == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
            }
            BigDecimal totalALiberar = entry.getValue();
            BigDecimal reservadoActual = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;

            if (reservadoActual.compareTo(totalALiberar) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La reserva del producto es menor que la cantidad del pedido");
            }
        }

        // FASE 4B-2A: Fase de escritura (solo cuando todos los productos son válidos)
        for (Map.Entry<Long, BigDecimal> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productosBloqueados.get(entry.getKey());
            BigDecimal totalALiberar = entry.getValue();
            BigDecimal reservadoActual = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;
            producto.setStockReservado(reservadoActual.subtract(totalALiberar));
            productoRepository.save(producto);
        }
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
                cantPrestada,
                det.getCantidadCanje()
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

    /**
     * Recalcula el estadoPago del pedido a partir de la suma REAL de pedido_pagos
     * comparada con el montoTotal. El estadoPago es independiente del estadoPedido:
     * un pedido puede estar ENTREGADO y seguir PENDIENTE, CREDITO o con pago parcial.
     */
    private void recalcularEstadoPago(Pedido pedido) {
        BigDecimal montoTotal = pedido.getMontoTotal() != null ? pedido.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal totalPagado = pedidoPagoRepository.findByPedido_Id(pedido.getId()).stream()
                .map(pago -> pago.getMonto() != null ? pago.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagado.compareTo(montoTotal) >= 0 && montoTotal.compareTo(BigDecimal.ZERO) > 0) {
            pedido.setEstadoPago("PAGADO");
        } else if (totalPagado.compareTo(BigDecimal.ZERO) > 0 || pedido.getFechaLimitePago() != null) {
            pedido.setEstadoPago("CREDITO");
        } else {
            pedido.setEstadoPago("PENDIENTE");
        }
    }
}
