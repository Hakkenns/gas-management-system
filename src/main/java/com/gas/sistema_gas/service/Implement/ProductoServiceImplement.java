package com.gas.sistema_gas.service.Implement;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.Base64;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.ProductoMapper;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.CategoriaRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.ProductoDTO;
import com.gas.sistema_gas.service.ProductoService;

import jakarta.transaction.Transactional;

@Service
public class ProductoServiceImplement implements ProductoService {

        @Autowired
        private ProductoMapper productoMapper;
        @Autowired
        private ProductoRepository productoRepository;
        @Autowired
        private CategoriaRepository categoriaRepository;
        @Autowired
        private InventarioLoteRepository inventarioLoteRepository;

        @Override
        @Transactional
        public List<ProductoDTO.SimpleResponse> listAll() {
                return productoRepository.findAll().stream()
                                .filter(p -> p.getEstado() != 2)
                                .map(p -> {
                                        // Calcular stockLlenos en caliente sumando cantidadActual de los lotes
                                        java.math.BigDecimal suma = inventarioLoteRepository
                                                        .sumCantidadActualByProductoId(p.getId());

                                        boolean tieneHistorialLotes = inventarioLoteRepository
                                                        .existsByProducto_Id(p.getId());

                                        // Obtener lote PEPS (más antiguo con cantidadActual > 0) SIN bloqueo
                                        java.math.BigDecimal precioCompraPeps = java.math.BigDecimal.ZERO;
                                        java.math.BigDecimal precioVentaPeps = java.math.BigDecimal.ZERO;
                                        java.util.List<com.gas.sistema_gas.Model.InventarioLote> peps = inventarioLoteRepository
                                                        .findLotesDisponiblesPEPSLectura(p.getId());
                                        if (peps != null && !peps.isEmpty()) {
                                                com.gas.sistema_gas.Model.InventarioLote lote = peps.get(0);
                                                precioCompraPeps = lote.getPrecioCompra() != null ? lote.getPrecioCompra()
                                                                : java.math.BigDecimal.ZERO;
                                                precioVentaPeps = lote.getPrecioVenta() != null ? lote.getPrecioVenta()
                                                                : java.math.BigDecimal.ZERO;
                                        } else {
                                                java.util.List<com.gas.sistema_gas.Model.InventarioLote> historial = inventarioLoteRepository
                                                                .findHistorialCompletoByProductoIdOrderByCreatedAtDesc(p.getId());
                                                if (historial != null && !historial.isEmpty()) {
                                                        com.gas.sistema_gas.Model.InventarioLote lote = historial.get(0);
                                                        precioCompraPeps = lote.getPrecioCompra() != null ? lote.getPrecioCompra()
                                                                        : java.math.BigDecimal.ZERO;
                                                        precioVentaPeps = lote.getPrecioVenta() != null ? lote.getPrecioVenta()
                                                                        : java.math.BigDecimal.ZERO;
                                                }
                                        }

                                        // Mapear a SimpleResponse y sobreescribir precioCompra, precioVenta, stockLlenos
                                        ProductoDTO.SimpleResponse base = productoMapper.toSimpleResponse(p);
                                        
                                        // Calcular ganancia dinámica basada en el lote PEPS activo o en el último lote histórico
                                        java.math.BigDecimal gananciaDinamica;
                                        if ((peps != null && !peps.isEmpty()) || precioVentaPeps.compareTo(java.math.BigDecimal.ZERO) != 0 || precioCompraPeps.compareTo(java.math.BigDecimal.ZERO) != 0) {
                                                gananciaDinamica = precioVentaPeps.subtract(precioCompraPeps);
                                        } else {
                                                gananciaDinamica = base.gananciaProducto();
                                        }
                                        
                                        return new ProductoDTO.SimpleResponse(
                                                        base.id(),
                                                        base.nombre(),
                                                        base.descripcion(),
                                                        base.urlImagen(),
                                                        base.idCategoria(),
                                                        base.nombreCategoria(),
                                                        base.capacidad(),
                                                        base.unidadMedida(),
                                                        precioCompraPeps,
                                                        gananciaDinamica,
                                                        precioVentaPeps,
                                                        base.requiereEnvase(),
                                                        suma,
                                                        base.stockVacios(),
                                                        base.stockMinimo(),
                                                        base.stockReservado(),
                                                        base.stockDisponible(),
                                                        tieneHistorialLotes,
                                                        base.estado());
                                })
                                .collect(Collectors.toList());
        }

        @Value("${app.product-images.dir:src/main/resources/static/imagenes-sistema}")
        private String productoImagesDir;

        private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse createProduct(ProductoDTO.Create createDto, MultipartFile archivoImagen) {
                return createProduct(createDto, archivoImagen, null);
        }

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse createProduct(ProductoDTO.Create createDto, MultipartFile archivoImagen,
                        String imagenBase64) {

                // Convertir Dto a entidad
                Producto producto = productoMapper.toEntity(createDto);

                // Buscamos y asignamos categoria
                Categoria categoria = categoriaRepository.findById(createDto.idCategoria())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "La categoría no existe"));

                // Si la categoria existe la asignamos al producto
                producto.setCategoria(categoria);
                producto.setCapacidad(createDto.capacidad());
                producto.setUnidadMedida(createDto.unidadMedida());

                
                producto.setPrecioCompra(BigDecimal.ZERO); // Vendrá de Compras
                producto.setGananciaProducto(createDto.gananciaProducto());
                producto.setPrecioVenta(BigDecimal.ZERO); // Se calcula después
                
                // Pasamos BigDecimal.ZERO en lugar del número entero 0
                producto.setStockLlenos(BigDecimal.ZERO); 
                
                // El stock de vacíos se queda igual porque en tu entidad sigue siendo Integer
                producto.setStockVacios(createDto.stockVacios() != null ? createDto.stockVacios() : 0);
                producto.setStockMinimo(createDto.stockMinimo() != null ? createDto.stockMinimo() : BigDecimal.ZERO);
                String imagenUrl = null;
                if (imagenBase64 != null && !imagenBase64.isBlank()) {
                        imagenUrl = almacenarImagenDesdeBase64(imagenBase64);
                } else {
                        imagenUrl = almacenarImagen(archivoImagen);
                }
                if (imagenUrl != null) {
                        producto.setUrlImagen(imagenUrl);
                }

                // Al final guardamos y retornamos una respuesta simple SimpleResponse
                return productoMapper.toSimpleResponse(productoRepository.save(producto));
        }

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse updateProduct(Long id, ProductoDTO.Update updateDto,
                        MultipartFile archivoImagen) {
                return updateProduct(id, updateDto, archivoImagen, null, false);
        }

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse updateProduct(Long id, ProductoDTO.Update updateDto,
                        MultipartFile archivoImagen, String imagenBase64, Boolean quitarImagen) {

                // Verificamos que el producto exista
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));

                productoMapper.updateEntityFromDto(updateDto, producto);
                producto.setCapacidad(updateDto.capacidad());
                producto.setUnidadMedida(updateDto.unidadMedida());

                // Actualizar ganancia y recalcular precio de venta
                producto.setGananciaProducto(updateDto.gananciaProducto());
                if (producto.getPrecioCompra() != null
                                && producto.getPrecioCompra().compareTo(BigDecimal.ZERO) > 0
                                && producto.getGananciaProducto() != null) {
                        producto.setPrecioVenta(producto.getPrecioCompra().add(updateDto.gananciaProducto()));
                } else {
                        producto.setPrecioVenta(BigDecimal.ZERO);
                }

                // Modificamos las relaciones si se cambian
                Categoria categoria = categoriaRepository.findById(updateDto.idCategoria())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "La categoria no existe"));
                producto.setCategoria(categoria);

                // Actualizar stock vacíos (solo si corresponde)
                producto.setStockVacios(updateDto.stockVacios() != null ? updateDto.stockVacios() : 0);
                producto.setStockMinimo(updateDto.stockMinimo() != null ? updateDto.stockMinimo() : BigDecimal.ZERO);

                // Manejo de quitar imagen
                if (quitarImagen != null && quitarImagen.booleanValue()) {
                        if (producto.getUrlImagen() != null && producto.getUrlImagen().startsWith("/images/")) {
                                eliminarImagenAnterior(producto.getUrlImagen());
                        }
                        producto.setUrlImagen(null);
                }

                // Nuevas imágenes desde base64 o multipart
                if (imagenBase64 != null && !imagenBase64.isBlank()) {
                        if (producto.getUrlImagen() != null && producto.getUrlImagen().startsWith("/images/")) {
                                eliminarImagenAnterior(producto.getUrlImagen());
                        }
                        producto.setUrlImagen(almacenarImagenDesdeBase64(imagenBase64));
                } else if (archivoImagen != null && !archivoImagen.isEmpty()) {
                        if (producto.getUrlImagen() != null && producto.getUrlImagen().startsWith("/images/")) {
                                eliminarImagenAnterior(producto.getUrlImagen());
                        }
                        producto.setUrlImagen(almacenarImagen(archivoImagen));
                }

                // retornamos una simple respuesta y guardamos los cambios
                return productoMapper.toSimpleResponse(productoRepository.save(producto));
        }

        @Override
        @Transactional
        public void removeImage(Long id) {
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));
                if (producto.getUrlImagen() != null && producto.getUrlImagen().startsWith("/images/")) {
                        eliminarImagenAnterior(producto.getUrlImagen());
                }
                producto.setUrlImagen(null);
                productoRepository.save(producto);
        }

        private String almacenarImagen(MultipartFile archivoImagen) {
                if (archivoImagen == null || archivoImagen.isEmpty()) {
                        return null;
                }

                if (archivoImagen.getSize() > MAX_IMAGE_SIZE) {
                        throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                                        "La imagen no puede superar los 5 MB");
                }

                String contentType = archivoImagen.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "El archivo debe ser una imagen válida");
                }

                String originalFilename = StringUtils.cleanPath(archivoImagen.getOriginalFilename());
                String extension = "";
                int dotIndex = originalFilename.lastIndexOf('.');
                if (dotIndex >= 0) {
                        extension = originalFilename.substring(dotIndex);
                }

                String filename = UUID.randomUUID().toString() + extension;
                Path uploadPath = Paths.get(productoImagesDir).toAbsolutePath().normalize();
                try {
                        Files.createDirectories(uploadPath);
                        Path target = uploadPath.resolve(filename);
                        Files.copy(archivoImagen.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                        return "/images/" + filename;
                } catch (IOException ex) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "No se pudo almacenar la imagen", ex);
                }
        }

        private void eliminarImagenAnterior(String urlImagen) {
                try {
                        String filename = urlImagen.replaceFirst("^/images/", "");
                        if (filename.contains("..") || filename.contains("/")) {
                                return;
                        }
                        Path uploadPath = Paths.get(productoImagesDir).toAbsolutePath().normalize();
                        Path filePath = uploadPath.resolve(filename).normalize();
                        if (Files.exists(filePath)) {
                                Files.delete(filePath);
                        }
                } catch (IOException ignored) {
                }
        }

        private String almacenarImagenDesdeBase64(String base64Data) {
                if (base64Data == null || base64Data.isBlank())
                        return null;
                try {
                        String data = base64Data;
                        String extension = "";
                        if (data.startsWith("data:")) {
                                int comma = data.indexOf(',');
                                String meta = data.substring(5, comma);
                                String mime = meta.split(";")[0];
                                extension = mime.substring(mime.indexOf('/') + 1);
                                data = data.substring(comma + 1);
                        } else {
                                // fallback to png
                                extension = "png";
                        }

                        byte[] bytes = Base64.getDecoder().decode(data);
                        String filename = UUID.randomUUID().toString() + "." + extension;
                        Path uploadPath = Paths.get(productoImagesDir).toAbsolutePath().normalize();
                        Files.createDirectories(uploadPath);
                        Path target = uploadPath.resolve(filename);
                        Files.write(target, bytes);
                        return "/images/" + filename;
                } catch (IOException ex) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "No se pudo almacenar la imagen base64", ex);
                }
        }

        @Override
        @Transactional
        public void deleteProduct(Long id) {
                // Verificamos que el producto exista
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));

                boolean tieneHistorialLotes = inventarioLoteRepository
                                .existsByProducto_Id(id);

                if (tieneHistorialLotes) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "No se puede eliminar el producto porque cuenta con historial de movimientos en el inventario.");
                }

                // Eliminación Lógica (cambiar estado)
                producto.setEstado(2);

                // Guardamos los cambios
                productoRepository.save(producto);
        }

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse setState(Long id, Integer estado) {
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));
                producto.setEstado(estado);
                return productoMapper.toSimpleResponse(productoRepository.save(producto));
        }

        @Override
        @Transactional
        public ProductoDTO.SimpleResponse findById(Long id) {
                return productoRepository.findById(id)
                                .map(p -> {
                                        java.math.BigDecimal suma = inventarioLoteRepository
                                                        .sumCantidadActualByProductoId(p.getId());

                                        java.math.BigDecimal precioCompraPeps = java.math.BigDecimal.ZERO;
                                        java.math.BigDecimal precioVentaPeps = java.math.BigDecimal.ZERO;
                                        java.util.List<com.gas.sistema_gas.Model.InventarioLote> peps = inventarioLoteRepository
                                                        .findLotesDisponiblesPEPSLectura(p.getId());
                                        if (peps != null && !peps.isEmpty()) {
                                                com.gas.sistema_gas.Model.InventarioLote lote = peps.get(0);
                                                precioCompraPeps = lote.getPrecioCompra() != null ? lote.getPrecioCompra()
                                                                : java.math.BigDecimal.ZERO;
                                                precioVentaPeps = lote.getPrecioVenta() != null ? lote.getPrecioVenta()
                                                                : java.math.BigDecimal.ZERO;
                                        }

                                        ProductoDTO.SimpleResponse base = productoMapper.toSimpleResponse(p);
                                        boolean tieneHistorialLotes = inventarioLoteRepository
                                                        .existsByProducto_Id(p.getId());
                                        return new ProductoDTO.SimpleResponse(
                                                        base.id(),
                                                        base.nombre(),
                                                        base.descripcion(),
                                                        base.urlImagen(),
                                                        base.idCategoria(),
                                                        base.nombreCategoria(),
                                                        base.capacidad(),
                                                        base.unidadMedida(),
                                                        precioCompraPeps,
                                                        base.gananciaProducto(),
                                                        precioVentaPeps,
                                                        base.requiereEnvase(),
                                                        suma,
                                                        base.stockVacios(),
                                                        base.stockMinimo(),
                                                        base.stockReservado(),
                                                        base.stockDisponible(),
                                                        tieneHistorialLotes,
                                                        base.estado());
                                })
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El producto no existe"));
        }

        // SimpleResponse findById(Long id)
        @Override
        @Transactional
        public List<ProductoDTO.SimpleResponse> listLowStock() {
                return productoRepository.findProductosSinStock().stream()
                                .map(p -> {
                                        java.math.BigDecimal suma = inventarioLoteRepository
                                                        .sumCantidadActualByProductoId(p.getId());

                                        java.math.BigDecimal precioCompraPeps = java.math.BigDecimal.ZERO;
                                        java.math.BigDecimal precioVentaPeps = java.math.BigDecimal.ZERO;
                                        java.util.List<com.gas.sistema_gas.Model.InventarioLote> peps = inventarioLoteRepository
                                                        .findLotesDisponiblesPEPSLectura(p.getId());
                                        if (peps != null && !peps.isEmpty()) {
                                                com.gas.sistema_gas.Model.InventarioLote lote = peps.get(0);
                                                precioCompraPeps = lote.getPrecioCompra() != null ? lote.getPrecioCompra()
                                                                : java.math.BigDecimal.ZERO;
                                                precioVentaPeps = lote.getPrecioVenta() != null ? lote.getPrecioVenta()
                                                                : java.math.BigDecimal.ZERO;
                                        }

                                        ProductoDTO.SimpleResponse base = productoMapper.toSimpleResponse(p);
                                        boolean tieneHistorialLotes = inventarioLoteRepository
                                                        .existsByProducto_Id(p.getId());
                                        return new ProductoDTO.SimpleResponse(
                                                        base.id(),
                                                        base.nombre(),
                                                        base.descripcion(),
                                                        base.urlImagen(),
                                                        base.idCategoria(),
                                                        base.nombreCategoria(),
                                                        base.capacidad(),
                                                        base.unidadMedida(),
                                                        precioCompraPeps,
                                                        base.gananciaProducto(),
                                                        precioVentaPeps,
                                                        base.requiereEnvase(),
                                                        suma,
                                                        base.stockVacios(),
                                                        base.stockMinimo(),
                                                        base.stockReservado(),
                                                        base.stockDisponible(),
                                                        tieneHistorialLotes,
                                                        base.estado());
                                })
                                .collect(Collectors.toList());
        }
}