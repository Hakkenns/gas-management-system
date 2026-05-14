package com.gas.sistema_gas.service.Implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.ProductoMapper;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Repository.CategoriaRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.ProveedorRepository;
import com.gas.sistema_gas.dto.ProductoDTO;
import com.gas.sistema_gas.service.ProductoService;

import jakarta.transaction.Transactional;

@Service
public class ProductoServiceImplement implements ProductoService {

        @Autowired
        private ProductoMapper productoMapper;;
        @Autowired
        private ProductoRepository productoRepository;
        @Autowired
        private CategoriaRepository categoriaRepository;
        @Autowired
        private ProveedorRepository proveedorRepository;

        @Override
        @Transactional
        public List<ProductoDTO.SimpleResponse> listAll() {
                return productoRepository.findAll().stream()
                                .filter(p -> p.getEstado() == 1)
                                .map(productoMapper::toSimpleResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse createProduct(ProductoDTO.Create createDto) {

                // Convertir Dto a entidad
                Producto producto = productoMapper.toEntity(createDto);

                // Buscamos y asignamos categoria
                Categoria categoria = categoriaRepository.findById(createDto.idCategoria())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "La categoría no existe"));

                // Si la categoria existe la asignamos al producto
                producto.setCategoria(categoria);

                // Buscamos y asignamos proveedor
                Proveedor proveedor = proveedorRepository.findById(createDto.idProveedor())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El perfil no existe"));

                // Si el proveedor existe entonces lo asignamos al producto
                producto.setProveedor(proveedor);

                // Al final guardamos y retornamos una respuesta simple SimpleResponse
                return productoMapper.toSimpleResponse(productoRepository.save(producto));
        }

        public ProductoDTO.SimpleResponse updateProduct(Long id, ProductoDTO.Update updateDto) {

                // Verificamos que el producto exista
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));

                // Actualizamos los campos de Producto
                producto.setNombre(updateDto.nombre());
                producto.setDescripcion(updateDto.descripcion());
                producto.setPrecioCompra(updateDto.precioCompra());
                producto.setPrecioVenta(updateDto.precioVenta());
                producto.setStockLlenos(updateDto.stockLlenos());
                producto.setStockMinimo(updateDto.stockMinimo());
                producto.setStockVacios(updateDto.stockVacios());
                producto.setRequiereEnvase(updateDto.requiereEnvase());

                // Modificamos las relaciones si se cambian
                Categoria categoria = categoriaRepository.findById(updateDto.idCategoria())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "La categoria no existe"));
                producto.setCategoria(categoria);

                Proveedor proveedor = proveedorRepository.findById(updateDto.idProveedor())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El proveedor no existe"));
                producto.setProveedor(proveedor);

                // retornamos una simple respuesta y guardamos los cambios
                return productoMapper.toSimpleResponse(productoRepository.save(producto));
        }

        @Override
        @Transactional
        public void deleteProduct(Long id) {
                // Verificamos que el producto exista
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));

                // Eliminación Lógica (cambiar estado)
                producto.setEstado(0);

                // gurdamos los cambios
                productoRepository.save(producto);
        }

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse findById(Long id) {
                return productoRepository.findById(id)
                                .map(productoMapper::toSimpleResponse)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));
        }

        //SimpleResponse findById(Long id)
        @Override
    @Transactional
    public List<ProductoDTO.SimpleResponse> listLowStock(){
        return productoRepository.findProductosSinStock().stream()
                .map(productoMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }
}