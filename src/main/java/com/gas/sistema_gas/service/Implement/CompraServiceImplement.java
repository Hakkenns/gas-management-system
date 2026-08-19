package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.gas.sistema_gas.Mapper.CompraMapper;
import com.gas.sistema_gas.Model.*;
import com.gas.sistema_gas.Repository.*;
import com.gas.sistema_gas.dto.CompraDTO;
import com.gas.sistema_gas.dto.InventarioLoteDTO;
import com.gas.sistema_gas.service.CompraService;
import com.gas.sistema_gas.service.CorrelativoService;
import com.gas.sistema_gas.service.InventarioLoteService;
import jakarta.transaction.Transactional;

@Service
public class CompraServiceImplement implements CompraService {

    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private DetalleCompraRepository detalleCompraRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CompraMapper compraMapper;
    @Autowired
    private CorrelativoService correlativoService;
    @Autowired
    private InventarioLoteService inventarioLoteService;
    @Autowired
    private CatalogoProveedorRepository catalogoProveedorRepository;
    @Autowired
    private InventarioLoteRepository inventarioLoteRepository;

    @Override
    @Transactional
    public List<CompraDTO.SimpleResponse> listAll() {
        return compraRepository.findAll().stream()
                .map(compraMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompraDTO.SimpleResponse create(CompraDTO.Create dto, Long idUsuarioLogueado) {
        Proveedor proveedor = proveedorRepository.findById(dto.idProveedor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
        Usuario usuario = usuarioRepository.findById(idUsuarioLogueado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        for (CompraDTO.DetalleItem item : dto.detalles()) {
            if (!catalogoProveedorRepository.existsRelacionActiva(proveedor.getId(), item.idProducto())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto seleccionado no pertenece al catÃ¡logo activo del proveedor.");
            }
        }

        // Convertir DTO a Entidad base
        Compra compra = compraMapper.toEntity(dto);
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setSituacion(1);

        compra.setFechaCompra(LocalDateTime.now());

        // Generar correlativo definitivo al guardar
        String codigoCorrelativo = correlativoService.incrementarYObtenerCodigo("COMPRA_NOTA", "NC001");
        compra.setNumDocumento(codigoCorrelativo);

        Compra compraGuardada = compraRepository.save(compra);

        // Procesar la lista de productos agregados
        for (CompraDTO.DetalleItem item : dto.detalles()) {
            Producto producto = productoRepository.findById(item.idProducto())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

            BigDecimal cantidadComprada = BigDecimal.valueOf(item.cantidad());

            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(compraGuardada);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioCostoUnitario(item.precioCostoUnitario());
            detalleCompraRepository.save(detalle);

            // Registrar el lote vinculado a este proveedor/producto en el inventario de
            // lotes
            BigDecimal gananciaBase = producto.getGananciaProducto() != null ? producto.getGananciaProducto()
                    : BigDecimal.ZERO;
            BigDecimal metrosPorRollo = null;
            if ("M".equals(producto.getUnidadMedida())) {
                metrosPorRollo = producto.getCapacidad();
                if (metrosPorRollo == null || metrosPorRollo.compareTo(BigDecimal.ZERO) <= 0) {
                    metrosPorRollo = BigDecimal.valueOf(60).setScale(2);
                }
            }

            InventarioLoteDTO.Create loteDto = new InventarioLoteDTO.Create(
                    producto.getId(),
                    proveedor.getId(),
                    cantidadComprada,
                    item.precioCostoUnitario(),
                    item.precioCostoUnitario().add(gananciaBase),
                    metrosPorRollo,
                    compraGuardada.getId());
            inventarioLoteService.registrarLote(loteDto);
        }

        return compraMapper.toSimpleResponse(compraGuardada);
    }

    @Override
    @Transactional
    public CompraDTO.SimpleResponse anularCompra(Long idCompra) {
        // 1. Validar idCompra
        if (idCompra == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de la compra es obligatorio");
        }

        // 2. Bloquear Compra mediante findByIdForUpdate
        Compra compra = compraRepository.findByIdForUpdate(idCompra)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));

        // 4. Verificar situacion
        if (compra.getSituacion() != null && compra.getSituacion() == 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra ya está anulada");
        }

        // 5. Llamar exactamente una vez al servicio de inventario
        inventarioLoteService.anularLotesCompra(idCompra);

        // 6. Solo cuando el inventario termine correctamente, marcar como anulada
        compra.setSituacion(2);
        Compra compraActualizada = compraRepository.save(compra);
        return compraMapper.toSimpleResponse(compraActualizada);
    }

    @Override
    @Transactional
    public CompraDTO.DetailResponse getDetalleByCompraId(Long idCompra) {
        Compra compra = compraRepository.findById(idCompra)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(idCompra);
        Map<Long, List<BigDecimal>> metrosPorRolloPorProducto = inventarioLoteRepository.findByCompraId(idCompra)
                .stream()
                .filter(lote -> lote.getProducto() != null && lote.getProducto().getId() != null)
                .filter(lote -> lote.getMetrosPorRollo() != null
                        && lote.getMetrosPorRollo().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(
                        lote -> lote.getProducto().getId(),
                        Collectors.mapping(InventarioLote::getMetrosPorRollo, Collectors.toList())));

        List<CompraDTO.DetailItemResponse> items = detalles.stream().map(detalle -> {
            Producto producto = detalle.getProducto();
            List<BigDecimal> metrajes = metrosPorRolloPorProducto.getOrDefault(producto.getId(), List.of())
                    .stream()
                    .distinct()
                    .toList();
            BigDecimal metrosPorRollo = metrajes.size() == 1 ? metrajes.get(0) : null;
            BigDecimal capacidad = "M".equals(producto.getUnidadMedida())
                    ? metrosPorRollo
                    : producto.getCapacidad();
            BigDecimal subtotal = BigDecimal.valueOf(detalle.getCantidad())
                    .multiply(detalle.getPrecioCostoUnitario());
            return new CompraDTO.DetailItemResponse(
                    producto.getNombre(),
                    capacidad,
                    detalle.getCantidad(),
                    producto.getUnidadMedida(),
                    metrosPorRollo,
                    detalle.getPrecioCostoUnitario(),
                    subtotal);
        }).toList();

        return new CompraDTO.DetailResponse(
                compra.getId(),
                compra.getNumDocumento(),
                compra.getProveedor().getNombre(),
                compra.getUsuario().getUserName(),
                compra.getFechaCompra(),
                compra.getSituacion(),
                compra.getMontoTotal(),
                items);
    }

    @Override
    public List<DetalleCompra> listDetallesByCompraId(Long idCompra) {
        return detalleCompraRepository.findByCompraId(idCompra);
    }
}
