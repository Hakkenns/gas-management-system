package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Map;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private OpcionService opcionService;

    @Autowired
    private CategoriaService categoriaService; // Para llenar el select de categorías

    // Vista principal: Carga la plantilla base con el menú y los selectores
    @GetMapping
    public String productos(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("productos", productoService.listAll());
        
        // Alimentamos los combos selectores del modal
        model.addAttribute("categorias", categoriaService.listAll()); 
        model.addAttribute("contenido", "views/productos");
        return "components/layout";
    }

    // Carga asíncrona del fragmento HTML de la tabla de productos
    @GetMapping("/tabla")
    public String tablaProductos(Model model) {
        model.addAttribute("productos", productoService.listAll());
        return "views/productos :: tablaProductos";
    }

    //Obtener datos de un producto por ID para cargar el modal de edición
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
        
        // Validaciones del DTO anotadas con @NotBlank, @NotNull, etc.
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
                productoService.updateProduct(productoId, new ProductoDTO.Update(
                        productoDto.nombre(),
                        productoDto.descripcion(),
                        productoDto.gananciaProducto(),
                        productoDto.requiereEnvase(),
                        productoDto.idCategoria(),
                        productoDto.stockVacios(),
                        productoDto.stockMinimo(),
                        1 // Estado por defecto activo al editar
                ), archivoImagen, imagenBase64, quitarImagen);
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
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo eliminar el producto.");
        }
    }
}