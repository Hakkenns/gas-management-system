package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor; // <-- Agregamos Lombok para limpiar el amarillo

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.gas.sistema_gas.dto.ProductoDTO;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.ProductoService;
import com.gas.sistema_gas.service.CategoriaService;
import com.gas.sistema_gas.service.ProveedorService; // <-- NUEVO: Tu servicio de proveedores
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor // <-- 1. Lombok crea el constructor automático eliminando todo el amarillo
public class ProductoController {

    // 2. Quitamos todos los @Autowired sueltos y declaramos los servicios como private final
    private final ProductoService productoService;
    private final OpcionService opcionService;
    private final CategoriaService categoriaService;
    private final ProveedorService proveedorService; // <-- NUEVA CAPA CONECTADA
    private final Validator validator;

    // Vista principal: Carga la plantilla base con el menú y los selectores
    @GetMapping
    public String productos(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");

        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("productos", productoService.listAll());
        model.addAttribute("categorias", categoriaService.listAll());
        
        // =====================================================================
        // 🌟 NUEVA LÍNEA: Enviamos los proveedores al Thymeleaf de la pantalla
        // =====================================================================
        model.addAttribute("proveedores", proveedorService.listAll()); 
        // =====================================================================
        
        model.addAttribute("contenido", "views/productos");
        return "components/layout";
    }

    // Carga asíncrona del fragmento HTML de la tabla de productos
    @GetMapping("/tabla")
    public String tablaProductos(Model model) {
        model.addAttribute("productos", productoService.listAll());
        return "views/productos :: tablaProductos";
    }

    // Obtener datos de un producto por ID para cargar el modal de edición
    @GetMapping("/{id}")
    @ResponseBody
    public ProductoDTO.SimpleResponse getById(@PathVariable Long id) {
        return productoService.findById(id);
    }

    // Guardar / Actualizar producto vía AJAX
    @PostMapping
    @ResponseBody
    public Map<String, Object> guardarProducto(@Valid ProductoDTO.Create productoDto,
            BindingResult result,
            @RequestParam(required = false) String id,
            @RequestParam(name = "archivoImagen", required = false) MultipartFile archivoImagen,
            @RequestParam(name = "imagenBase64", required = false) String imagenBase64,
            @RequestParam(name = "quitarImagen", required = false) Boolean quitarImagen) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }

        Long productoId = null;
        if (id != null && !id.isBlank()) {
            productoId = Long.valueOf(id);
        }

        try {
            if (productoId == null) {
                productoService.createProduct(productoDto, archivoImagen, imagenBase64);
            } else {
                ProductoDTO.Update updateDto = new ProductoDTO.Update(
                        productoDto.nombre(),
                        productoDto.descripcion(),
                        productoDto.capacidad(),
                        productoDto.unidadMedida(),
                        productoDto.gananciaProducto(),
                        productoDto.requiereEnvase(),
                        productoDto.idCategoria(),
                        productoDto.stockVacios(),
                        productoDto.stockMinimo(),
                        1);

                Set<ConstraintViolation<ProductoDTO.Update>> violations = validator.validate(updateDto);
                if (!violations.isEmpty()) {
                    String message = violations.iterator().next().getMessage();
                    return Map.of("status", "ERROR", "message", message);
                }

                productoService.updateProduct(productoId, updateDto, archivoImagen, imagenBase64, quitarImagen);
            }
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "Error al procesar el producto: " + e.getMessage());
        }
    }

    // Cambiar estado de producto vía AJAX (Activo / Inactivo)
    @PostMapping("/{id}/estado")
    @ResponseBody
    public Map<String, Object> cambiarEstadoProductoAjax(@PathVariable Long id, @RequestParam Integer estado) {
        try {
            productoService.setState(id, estado);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo cambiar el estado del producto.");
        }
    }

    // Eliminar producto vía AJAX (Borrado lógico)
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminarProductoAjax(@PathVariable Long id) {
        try {
            productoService.deleteProduct(id);
            return Map.of("status", "OK");
        } catch (ResponseStatusException e) {
            return Map.of("status", "ERROR", "message", e.getReason());
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo eliminar el producto.");
        }
    }
}
