package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.CatalogoProveedorMapper;
import com.gas.sistema_gas.Model.CatalogoProveedor;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Repository.CatalogoProveedorRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.ProveedorRepository;
import com.gas.sistema_gas.dto.CatalogoProveedorDTO;
import com.gas.sistema_gas.service.CatalogoProveedorService;
import lombok.RequiredArgsConstructor;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogoProveedorServiceImplement implements CatalogoProveedorService {

    private final CatalogoProveedorRepository catalogoProveedorRepository;
    private final CatalogoProveedorMapper catalogoProveedorMapper;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    @Override
    @Transactional
    public List<CatalogoProveedorDTO.SimpleResponse> listarTodo() {
        
        return catalogoProveedorRepository.findAll().stream()
                .map(catalogoProveedorMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CatalogoProveedorDTO.SimpleResponse asociarProducto(CatalogoProveedorDTO.Create createDto) {
        // VALIDACIÓN DE SEGURIDAD: Evita duplicados idénticos en la base de datos
        if (catalogoProveedorRepository.existsByProveedorIdAndProductoId(createDto.idProveedor(), createDto.idProducto())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este producto ya se encuentra asignado a este proveedor");
        }

        // Cargamos las entidades referenciadas y las asignamos explícitamente
        Proveedor proveedor = proveedorRepository.findById(createDto.idProveedor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proveedor no válido"));

        Producto producto = productoRepository.findById(createDto.idProducto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto no válido"));

        CatalogoProveedor catalogo = new CatalogoProveedor();
        catalogo.setProveedor(proveedor);
        catalogo.setProducto(producto);

        CatalogoProveedor guardado = catalogoProveedorRepository.save(catalogo);

        return catalogoProveedorMapper.toSimpleResponse(guardado);
    }

    @Override
    @Transactional
    public void desasociarProducto(Long idProveedor, Long idProducto) {
        // VALIDACIÓN PREVIA: Si intentan borrar algo que no existe, disparamos error controlado
        if (!catalogoProveedorRepository.existsByProveedorIdAndProductoId(idProveedor, idProducto)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La asociación no existe en el catálogo");
        }
        
        // Borrado transaccional
        catalogoProveedorRepository.deleteByProveedorIdAndProductoId(idProveedor, idProducto);
    }

    @Override
    @Transactional
    public List<Producto> listarProductosPorProveedor(Long idProveedor) {
        // 🔍 EL BLINDAJE DE LA LUPA DE COMPRAS
        // Llama a la consulta JPQL personalizada para limpiar la interfaz de usuario en compras
        return catalogoProveedorRepository.findProductosByProveedorId(idProveedor);
    }

    @Override
    @Transactional
    public CatalogoProveedorDTO.SimpleResponse buscarPorId(Long id) {
        // Búsqueda segura con manejo de Optional y excepción limpia si el ID no existe
        return catalogoProveedorRepository.findById(id)
                .map(catalogoProveedorMapper::toSimpleResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El registro de catálogo no existe"));
    }
}
