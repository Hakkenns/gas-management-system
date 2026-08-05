package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.InventarioLoteMapper;
import com.gas.sistema_gas.Model.AsignacionLotePedido;
import com.gas.sistema_gas.Model.Compra;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Repository.AsignacionLotePedidoRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.ProveedorRepository;
import com.gas.sistema_gas.dto.InventarioLoteDTO;
import com.gas.sistema_gas.service.InventarioLoteService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Inyección moderna por constructor (Cero líneas amarillas)
public class InventarioLoteServiceImplement implements InventarioLoteService {

    private final InventarioLoteRepository inventarioLoteRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final InventarioLoteMapper inventarioLoteMapper;
    private final AsignacionLotePedidoRepository asignacionLotePedidoRepository;

    @Override
    @Transactional
    public InventarioLoteDTO.SimpleResponse registrarLote(InventarioLoteDTO.Create createDto) {
        // 1. Validar que las entidades maestras existan físicamente en la BD
        Producto producto = productoRepository.findById(createDto.idProducto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe"));
        Proveedor proveedor = proveedorRepository.findById(createDto.idProveedor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El proveedor no existe"));

        // 2. Convertir el DTO en Entidad
        InventarioLote lote = inventarioLoteMapper.toEntity(createDto);
        
        // 3. Seteamos manualmente las relaciones y el stock inicial vivo
        lote.setProducto(producto);
        lote.setProveedor(proveedor);
        lote.setCantidadActual(createDto.cantidadInicial()); // Al nacer, el stock vivo es igual al inicial

        if (createDto.idCompra() != null) {
            Compra compra = new Compra();
            compra.setId(createDto.idCompra());
            lote.setCompra(compra);
        }

        InventarioLote guardado = inventarioLoteRepository.save(lote);

        // ================================================================
        // FIX: Sincronizar stockLlenos del Producto inmediatamente después
        // de registrar un nuevo lote, sumando en tiempo real la cantidadActual
        // de todos los lotes activos de este producto.
        // ================================================================
        BigDecimal stockTotalLotes = inventarioLoteRepository
                .sumCantidadActualByProductoId(producto.getId());

        // Si el stock cambió respecto al valor actual del producto, actualizamos
        if (stockTotalLotes.compareTo(producto.getStockLlenos() != null ? producto.getStockLlenos() : BigDecimal.ZERO) != 0) {
            producto.setStockLlenos(stockTotalLotes);
            productoRepository.save(producto);
        }

        // 4. Construimos la respuesta SimpleResponse rellenando los nombres reales
        return mapearASimpleResponseConNombres(guardado);
    }

    @Override
    @Transactional
    public InventarioLoteDTO.SimpleResponse actualizarPrecioVenta(Long idLote, InventarioLoteDTO.Update updateDto) {
        // Buscamos el lote en la base de datos
        InventarioLote lote = inventarioLoteRepository.findById(idLote)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El lote de inventario no existe"));

        //REGLA DE NEGOCIO: Actualizamos el precio de venta en caliente por la competencia
        lote.setPrecioVenta(updateDto.precioVenta());
        
        // Al guardarlo, MariaDB actualizará automáticamente el campo 'updated_at' por el trigger del SQL
        InventarioLote actualizado = inventarioLoteRepository.save(lote);

        return mapearASimpleResponseConNombres(actualizado);
    }

    @Override
    @Transactional
    public List<InventarioLoteDTO.SimpleResponse> listarLotesPorProducto(Long idProducto) {
        // Estilo exacto de tus Streams de Categorías para mapear la lista completa del
        // modal
        // Usa el historial completo DESC que incluye lotes agotados y muestra los más
        // recientes primero
        return inventarioLoteRepository.findHistorialCompletoByProductoIdOrderByCreatedAtDesc(idProducto).stream()
                .map(this::mapearASimpleResponseConNombres)
                .collect(Collectors.toList());
    }

    /**
     * Desconta el stock de los lotes disponibles según el método PEPS (Primero en
     * Entrar, Primero en Salir)
     * y registra la trazabilidad de qué lotes se consumieron.
     *
     * @param detallePedido El detalle del pedido con producto, cantidad y pedido
     *                      asociado
     */
    @Override
    @Transactional
    public void descontarStockPorPEPS(DetallePedido detallePedido) {
        // Validaciones de entrada
        if (detallePedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El detalle del pedido no puede ser nulo");
        }

        Long idDetalle = detallePedido.getIdDetalle();
        if (idDetalle == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El detalle del pedido debe estar guardado antes de descontar stock");
        }

        Pedido pedido = detallePedido.getPedido();
        if (pedido == null || pedido.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido del detalle no es válido");
        }

        Producto producto = detallePedido.getProducto();
        if (producto == null || producto.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto del detalle no es válido");
        }

        Integer cantidad = detallePedido.getCantidad();
        if (cantidad == null || cantidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad del detalle debe ser mayor a cero");
        }

        BigDecimal cantidadAVender = BigDecimal.valueOf(cantidad);

        // Verificar idempotencia: si ya existe una asignación activa para este detalle,
        // no procesar
        if (asignacionLotePedidoRepository.existsByIdDetallePedidoAndEstado(
                idDetalle,
                AsignacionLotePedido.EstadoAsignacion.DESCONTADA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El detalle del pedido ya tiene una asignación de lotes activa");
        }

        // Obtener lotes disponibles con bloqueo pesimista
        List<InventarioLote> lotesDisponibles = inventarioLoteRepository.findLotesDisponiblesPEPS(producto.getId());

        // Validar stock total disponible antes de modificar
        BigDecimal stockTotalDisponible = lotesDisponibles.stream()
                .map(l -> l.getCantidadActual() != null ? l.getCantidadActual() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (stockTotalDisponible.compareTo(cantidadAVender) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente en los lotes para cubrir la venta.");
        }

        BigDecimal cantidadRestante = cantidadAVender;

        for (InventarioLote lote : lotesDisponibles) {
            if (cantidadRestante.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal stockDisponibleLote = lote.getCantidadActual();
            BigDecimal cantidadDescontadaDelLote = BigDecimal.ZERO;

            if (stockDisponibleLote.compareTo(cantidadRestante) >= 0) {
                // Caso A: El lote tiene suficiente stock
                cantidadDescontadaDelLote = cantidadRestante;
                lote.setCantidadActual(stockDisponibleLote.subtract(cantidadRestante));
                cantidadRestante = BigDecimal.ZERO;
            } else {
                // Caso B: El lote no alcanza, lo vaciamos
                cantidadDescontadaDelLote = stockDisponibleLote;
                cantidadRestante = cantidadRestante.subtract(stockDisponibleLote);
                lote.setCantidadActual(BigDecimal.ZERO);
            }

            inventarioLoteRepository.save(lote);

            // Crear asignación de trazabilidad
            AsignacionLotePedido asignacion = new AsignacionLotePedido();
            asignacion.setPedido(pedido);
            asignacion.setProducto(producto);
            asignacion.setLote(lote);
            asignacion.setIdDetallePedido(idDetalle);
            asignacion.setCantidadDescontada(cantidadDescontadaDelLote);
            asignacion.setCantidadDevuelta(BigDecimal.ZERO);
            asignacion.setEstado(AsignacionLotePedido.EstadoAsignacion.DESCONTADA);
            asignacionLotePedidoRepository.save(asignacion);
        }

        // Validación final de seguridad
        if (cantidadRestante.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente en los lotes para cubrir la venta.");
        }
    }

    @Override
    @Transactional
    public void anularLotesCompra(Long idCompra) {
        // 1. Validar que idCompra no sea null
        if (idCompra == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de la compra es obligatorio");
        }

        // 2. Consultar IDs de productos asociados a los lotes de la compra (solo lectura)
        List<Long> idsProductosDescubiertos = inventarioLoteRepository.findProductoIdsByCompraId(idCompra);

        // 3. Eliminar duplicados defensivamente y ordenar ASC
        List<Long> idsProductosOrdenados = idsProductosDescubiertos.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 4. Si no existen lotes/productos asociados, permitir terminar sin eliminar ni modificar
        if (idsProductosOrdenados.isEmpty()) {
            return;
        }

        // 5. Bloquear Productos exactamente una vez (PESSIMISTIC_WRITE)
        List<Producto> productosBloqueados = productoRepository.findAllByIdInForUpdate(idsProductosOrdenados);

        // 6. Validar el conjunto exacto de productos bloqueados
        if (productosBloqueados == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Inconsistencia de inventario: no se pudieron bloquear los productos");
        }
        for (Producto producto : productosBloqueados) {
            if (producto == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: producto nulo en el conjunto bloqueado");
            }
            if (producto.getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: producto con ID nulo en el conjunto bloqueado");
            }
        }
        List<Long> idsProductosBloqueados = productosBloqueados.stream()
                .map(Producto::getId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (!idsProductosBloqueados.equals(idsProductosOrdenados)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Inconsistencia de inventario: los productos bloqueados no coinciden con los descubiertos");
        }

        // Crear mapa de Productos bloqueados para validacion
        Map<Long, Producto> mapaProductos = productosBloqueados.stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        // 7. Bloquear Lotes (PESSIMISTIC_WRITE) ordenados por ID ASC
        List<InventarioLote> lotes = inventarioLoteRepository.findByCompraIdForUpdate(idCompra);

        // 8. Validar antes de eliminar
        for (InventarioLote lote : lotes) {
            if (lote == null || lote.getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: lote invalido");
            }
            if (lote.getCompra() == null || lote.getCompra().getId() == null
                    || !lote.getCompra().getId().equals(idCompra)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: el lote no pertenece a la compra");
            }
            if (lote.getProducto() == null || lote.getProducto().getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: el lote no tiene producto valido");
            }
            if (!mapaProductos.containsKey(lote.getProducto().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: el producto del lote no coincide con los bloqueados");
            }
            if (lote.getCantidadInicial() == null || lote.getCantidadActual() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: cantidades nulas en el lote");
            }
            if (lote.getCantidadInicial().compareTo(BigDecimal.ZERO) < 0
                    || lote.getCantidadActual().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: cantidades negativas en el lote");
            }
            if (lote.getCantidadActual().compareTo(lote.getCantidadInicial()) != 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inconsistencia de inventario: el lote ya fue consumido total o parcialmente");
            }
        }

        // 9. Verificar que los IDs de productos en los Lotes coincidan con los descubiertos
        List<Long> idsProductosEnLotes = lotes.stream()
                .map(l -> l.getProducto().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (!idsProductosEnLotes.equals(idsProductosOrdenados)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Inconsistencia de inventario: productos del lote no coinciden con los descubiertos");
        }

        // 10. Obtener IDs unicos de lotes ordenados ASC
        List<Long> idsLotes = lotes.stream()
                .map(InventarioLote::getId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 11. Comprobar trazabilidad historica (DESCONTADA o DEVUELTA)
        boolean existeTrazabilidad = asignacionLotePedidoRepository.existsByLoteIdIn(idsLotes);

        // 12. Si existe alguna asignacion, rechazar sin eliminar ni modificar
        if (existeTrazabilidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Inconsistencia de inventario: existe trazabilidad para los lotes de la compra");
        }

        // 13. Solamente despues de completar todas las validaciones, eliminar lotes
        inventarioLoteRepository.deleteAll(lotes);

        // 14. Flush antes de recalcular stock
        inventarioLoteRepository.flush();

        // 15. Recalcular stockLlenos para cada Producto bloqueado
        for (Producto producto : productosBloqueados) {
            BigDecimal stockTotal = inventarioLoteRepository.sumCantidadActualByProductoId(producto.getId());
            // 16. Si SUM retorna null, usar BigDecimal.ZERO
            if (stockTotal == null) {
                stockTotal = BigDecimal.ZERO;
            }
            producto.setStockLlenos(stockTotal);
        }

        // 17. Guardar los Productos una sola vez
        productoRepository.saveAll(productosBloqueados);

        // 18. No modificar stockReservado
        // 19. No modificar stockVacios
        // 20. No eliminar DetalleCompra
        // 21. No marcar Compra como anulada dentro de este servicio
    }

    @Override
    @Transactional
    public void devolverStockDePedido(Long idPedido) {
        // 1. Validar ID de pedido
        if (idPedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del pedido es obligatorio");
        }

        // 2. Buscar asignaciones DESCONTADA con bloqueo pesimista
        List<AsignacionLotePedido> asignacionesDescontadas = asignacionLotePedidoRepository
                .findByPedidoIdAndEstadoForUpdate(
                        idPedido,
                        AsignacionLotePedido.EstadoAsignacion.DESCONTADA);

        // 3. Verificar si hay asignaciones DEVUELTA (ya devueltas)
        boolean existeDevuelta = asignacionLotePedidoRepository.existsByPedido_IdAndEstado(
                idPedido,
                AsignacionLotePedido.EstadoAsignacion.DEVUELTA);

        // 4. Validar estados del pedido
        if (asignacionesDescontadas.isEmpty() && existeDevuelta) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El stock de este pedido ya fue devuelto");
        }

        if (asignacionesDescontadas.isEmpty() && !existeDevuelta) {
            // Verificar si existe alguna asignación en cualquier estado
            boolean existeAsignacion = asignacionLotePedidoRepository.existsByPedido_Id(idPedido);
            if (!existeAsignacion) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El pedido no tiene trazabilidad de lotes; requiere ajuste manual de inventario");
            }
            // Si existe pero no hay DESCONTADA ni DEVUELTA, hay inconsistencia
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La trazabilidad de lotes del pedido es inconsistente");
        }

        // 5. Validar todas las asignaciones antes de modificar (trazabilidad
        // consistente)
        for (AsignacionLotePedido asignacion : asignacionesDescontadas) {
            if (asignacion.getLote() == null || asignacion.getLote().getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
            if (asignacion.getProducto() == null || asignacion.getProducto().getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
            if (asignacion.getCantidadDescontada() == null ||
                    asignacion.getCantidadDevuelta() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
            // Rechazar cantidades negativas
            if (asignacion.getCantidadDescontada().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
            if (asignacion.getCantidadDevuelta().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
            if (asignacion.getCantidadDescontada().compareTo(asignacion.getCantidadDevuelta()) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
        }

        // 6. Obtener IDs únicos de lotes y productos, ordenados ASC
        List<Long> idsProductos = asignacionesDescontadas.stream()
                .map(a -> a.getProducto().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<Long> idsLotes = asignacionesDescontadas.stream()
                .map(a -> a.getLote().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 7. Bloquear Productos con PESSIMISTIC_WRITE (antes que Lotes)
        List<Producto> productos = productoRepository.findAllByIdInForUpdate(idsProductos);

        // 8. Verificar que estén todos los Productos
        if (productos.size() != idsProductos.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La trazabilidad de lotes del pedido es inconsistente");
        }

        // 9. Bloquear Lotes con PESSIMISTIC_WRITE (después de Productos)
        List<InventarioLote> lotes = inventarioLoteRepository.findAllByIdInForUpdate(idsLotes);

        // 10. Verificar que estén todos los Lotes
        if (lotes.size() != idsLotes.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La trazabilidad de lotes del pedido es inconsistente");
        }

        // 11. Crear mapas para validación y actualización
        Map<Long, InventarioLote> mapaLotes = lotes.stream()
                .collect(Collectors.toMap(InventarioLote::getId, l -> l));
        Map<Long, Producto> mapaProductos = productos.stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        // 12. Validar que cada Lote corresponda al Producto de la asignación
        for (AsignacionLotePedido asignacion : asignacionesDescontadas) {
            InventarioLote lote = mapaLotes.get(asignacion.getLote().getId());
            if (lote == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
            // Validar que el lote pertenece al producto de la asignación
            if (lote.getProducto() == null || lote.getProducto().getId() == null ||
                    !lote.getProducto().getId().equals(asignacion.getProducto().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
            // Validar que el producto bloqueado existe
            if (mapaProductos.get(asignacion.getProducto().getId()) == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La trazabilidad de lotes del pedido es inconsistente");
            }
        }

        // 13. Agrupar la devolución pendiente por Lote (sin modificar lotes aún)
        Map<Long, BigDecimal> cantidadPendientePorLote = new HashMap<>();
        for (AsignacionLotePedido asignacion : asignacionesDescontadas) {
            BigDecimal cantidadPendiente = asignacion.getCantidadDescontada()
                    .subtract(asignacion.getCantidadDevuelta());
            cantidadPendientePorLote.merge(asignacion.getLote().getId(), cantidadPendiente, BigDecimal::add);
        }

        // 14. Actualizar cada Lote una sola vez (fuera del bucle de asignaciones)
        for (InventarioLote lote : lotes) {
            BigDecimal cantidadADevolver = cantidadPendientePorLote.getOrDefault(lote.getId(), BigDecimal.ZERO);
            BigDecimal stockActual = lote.getCantidadActual() != null ? lote.getCantidadActual() : BigDecimal.ZERO;
            lote.setCantidadActual(stockActual.add(cantidadADevolver));
        }

        // 15. Marcar todas las asignaciones como DEVUELTA
        for (AsignacionLotePedido asignacion : asignacionesDescontadas) {
            asignacion.setEstado(AsignacionLotePedido.EstadoAsignacion.DEVUELTA);
            asignacion.setCantidadDevuelta(asignacion.getCantidadDescontada());
        }

        // 16. Guardar cada lote una sola vez
        inventarioLoteRepository.saveAll(lotes);

        // 17. Guardar cada asignación una sola vez
        asignacionLotePedidoRepository.saveAll(asignacionesDescontadas);

        // 18. Flush para asegurar que los cambios se reflejen antes del recálculo
        inventarioLoteRepository.flush();

        // 19. Recalcular stockLlenos para cada producto
        for (Producto producto : productos) {
            BigDecimal stockTotal = inventarioLoteRepository.sumCantidadActualByProductoId(producto.getId());
            producto.setStockLlenos(stockTotal);
        }

        // 20. Guardar cada producto una sola vez
        productoRepository.saveAll(productos);
    }

    @Override
    @Transactional
    public void validarStockDevueltoParaReactivacion(Long idPedido) {
        // 1. Validar ID de pedido
        if (idPedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del pedido es obligatorio");
        }

        // 2. Verificar si hay asignaciones DESCONTADA (stock no devuelto)
        boolean existeDescontada = asignacionLotePedidoRepository.existsByPedido_IdAndEstado(
            idPedido,
            AsignacionLotePedido.EstadoAsignacion.DESCONTADA
        );

        if (existeDescontada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El stock del pedido aún no ha sido devuelto");
        }

        // 3. Verificar si existe alguna asignación
        boolean existeAsignacion = asignacionLotePedidoRepository.existsByPedido_Id(idPedido);
        if (!existeAsignacion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El pedido no tiene trazabilidad de lotes; requiere ajuste manual de inventario");
        }

        // 4. Si todas están DEVUELTA, finalizar correctamente (no modificar datos)
        // No hay necesidad de hacer nada más, el método simplemente valida
    }

    // =========================================================================
    // MÉTODO PRIVADO AUXILIAR: Garantiza el mapeo simétrico rellenando los textos
    // =========================================================================
    private InventarioLoteDTO.SimpleResponse mapearASimpleResponseConNombres(InventarioLote lote) {
        InventarioLoteDTO.SimpleResponse baseResponse = inventarioLoteMapper.toSimpleResponse(lote);
        return new InventarioLoteDTO.SimpleResponse(
                baseResponse.id(),
                baseResponse.idProducto(),
                lote.getProducto().getNombre(), // Inyectamos texto del producto
                baseResponse.idProveedor(),
                lote.getProveedor().getNombre(), // Inyectamos texto del proveedor
                baseResponse.cantidadInicial(),
                baseResponse.cantidadActual(),
                baseResponse.precioCompra(),
                baseResponse.precioVenta(),
                baseResponse.metrosPorRollo(),
                lote.getCreatedAt(),
                lote.getUpdatedAt());
    }
}