package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.gas.sistema_gas.Mapper.CompraMapper;
import com.gas.sistema_gas.Model.*;
import com.gas.sistema_gas.Repository.*;
import com.gas.sistema_gas.dto.CompraDTO;
import com.gas.sistema_gas.service.CompraService;
import com.gas.sistema_gas.service.CorrelativoService;
import jakarta.transaction.Transactional;

@Service
public class CompraServiceImplement implements CompraService {

    @Autowired private CompraRepository compraRepository;
    @Autowired private DetalleCompraRepository detalleCompraRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompraMapper compraMapper;
    @Autowired private CorrelativoService correlativoService;

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

        // Convertir DTO a Entidad base
        Compra compra = compraMapper.toEntity(dto);
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setSituacion(1);

        if (dto.fechaCompra() != null && !dto.fechaCompra().isBlank()) {
            LocalDateTime fecha = LocalDateTime.parse(dto.fechaCompra());
            if (fecha.isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de emisión no puede ser futura");
            }
            compra.setFechaCompra(fecha);
        } else {
            compra.setFechaCompra(LocalDateTime.now());
        }

        // Generar correlativo definitivo al guardar
        String codigoCorrelativo = correlativoService.incrementarYObtenerCodigo("COMPRA_NOTA", "NC001");
        compra.setNumDocumento(codigoCorrelativo);

        Compra compraGuardada = compraRepository.save(compra);

                // Procesar la lista de productos agregados
        for (CompraDTO.DetalleItem item : dto.detalles()) {
            Producto producto = productoRepository.findById(item.idProducto())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

            // 🛢️ LÓGICA DE NEGOCIO: Convertimos item.cantidad() a BigDecimal antes de sumar con .add()
            BigDecimal cantidadComprada = BigDecimal.valueOf(item.cantidad());
            producto.setStockLlenos(producto.getStockLlenos().add(cantidadComprada));

            // Actualizar precio de compra y precio de venta según la compra
            if (item.precioCostoUnitario() != null) {
                producto.setPrecioCompra(item.precioCostoUnitario());
                BigDecimal ganancia = producto.getGananciaProducto() != null ? producto.getGananciaProducto() : BigDecimal.ZERO;
                producto.setPrecioVenta(item.precioCostoUnitario().add(ganancia));
            }

            productoRepository.save(producto);

            // Guardar fila en detalle_compra
            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(compraGuardada);
            detalle.setProducto(producto);
            
            // Si en tu entidad DetalleCompra cambiaste cantidad a BigDecimal, usa:
            // detalle.setCantidad(cantidadComprada);
            // Si aún es Integer (temporalmente), usa la línea de abajo:
            detalle.setCantidad(item.cantidad()); 
            
            detalle.setPrecioCostoUnitario(item.precioCostoUnitario());

            detalleCompraRepository.save(detalle);
        }


        return compraMapper.toSimpleResponse(compraGuardada);
    }

    @Override
    @Transactional
    public CompraDTO.SimpleResponse anularCompra(Long idCompra) {
        Compra compra = compraRepository.findById(idCompra)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
        if (compra.getSituacion() != null && compra.getSituacion() == 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La compra ya está anulada");
        }

        // Obtener todos los detalles de esta compra
        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(idCompra);

        // Revertir cambios en productos
        for (DetalleCompra detalle : detalles) {
            Producto producto = detalle.getProducto();
            
            // Restar la cantidad que se agregó al stock
            BigDecimal cantidadComprada = BigDecimal.valueOf(detalle.getCantidad());
            producto.setStockLlenos(producto.getStockLlenos().subtract(cantidadComprada));

            // Verificar si hay otras compras activas (no anuladas) de este producto
            long otrasCompras = detalleCompraRepository.countByProductoIdAndCompraIdNotAndCompraSituacionNot(
                    producto.getId(), idCompra, 2);

            // Si no hay otras compras, resetear precios
            if (otrasCompras == 0) {
                producto.setPrecioCompra(BigDecimal.ZERO);
                // Precio venta = ganancia (sin precio de compra, solo la ganancia)
                BigDecimal ganancia = producto.getGananciaProducto() != null ? producto.getGananciaProducto() : BigDecimal.ZERO;
                producto.setPrecioVenta(ganancia);
            }

            productoRepository.save(producto);
            
            // Eliminar el detalle de esta compra
            detalleCompraRepository.delete(detalle);
        }

        // Marcar la compra como anulada
        compra.setSituacion(2);
        Compra compraActualizada = compraRepository.save(compra);
        return compraMapper.toSimpleResponse(compraActualizada);
    }

    @Override
    public List<DetalleCompra> listDetallesByCompraId(Long idCompra) {
        return detalleCompraRepository.findByCompraId(idCompra);//aqui tbm me sale error en .findByCompraId
    }
}