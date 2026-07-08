package com.gas.sistema_gas.service.Implement;


import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.InventarioLoteMapper;
import com.gas.sistema_gas.Model.Compra;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
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
        // Estilo exacto de tus Streams de Categorías para mapear la lista completa del modal
        return inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(idProducto).stream()
                .map(this::mapearASimpleResponseConNombres)
                .collect(Collectors.toList());
    }

    /**
     * Desconta el stock de los lotes disponibles según el método PEPS (Primero en Entrar, Primero en Salir)
     * @param idProducto El ID del producto del que se desea descontar stock
     * @param cantidadAVender La cantidad a vender
     */
    @Override
    @Transactional
    public void descontarStockPorPEPS(Long idProducto, BigDecimal cantidadAVender) {
        
        List<InventarioLote> lotesDisponibles = inventarioLoteRepository.findLotesDisponiblesPEPS(idProducto);

        BigDecimal cantidadRestante = cantidadAVender;

        for (InventarioLote lote : lotesDisponibles) {
            if (cantidadRestante.compareTo(BigDecimal.ZERO) <= 0) break; // Si ya descontamos todo, terminamos

            BigDecimal stockDisponibleLote = lote.getCantidadActual();

            if (stockDisponibleLote.compareTo(cantidadRestante) >= 0) {
                // Caso A: El lote antiguo tiene suficiente stock para cubrir toda la venta
                lote.setCantidadActual(stockDisponibleLote.subtract(cantidadRestante));
                cantidadRestante = BigDecimal.ZERO;
            } else {
                // Caso B: El lote antiguo no abastece todo. Lo vaciamos a 0 y el saldo pasa al siguiente lote caro
                cantidadRestante = cantidadRestante.subtract(stockDisponibleLote);
                lote.setCantidadActual(BigDecimal.ZERO);
            }
            inventarioLoteRepository.save(lote); // Sincroniza el nuevo stock del lote en phpMyAdmin
        }

        // Validación final de seguridad: Si recorrió todo y aún falta stock, hubo una inconsistencia
        if (cantidadRestante.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock insuficiente en los lotes para cubrir la venta.");
        }
    }

    @Override
    @Transactional
    public void anularLotesCompra(Long idCompra) {
        List<InventarioLote> lotes = inventarioLoteRepository.findByCompraId(idCompra);
        inventarioLoteRepository.deleteAll(lotes);
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
                lote.getUpdatedAt()
        );
    }
}
