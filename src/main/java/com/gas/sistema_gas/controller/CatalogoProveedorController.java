package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.gas.sistema_gas.dto.CatalogoProveedorDTO;
import com.gas.sistema_gas.service.CatalogoProveedorService;

@Controller
@RequestMapping("/catalogo-proveedores")
@RequiredArgsConstructor // Inyección moderna por constructor libre de alertas amarillas
public class CatalogoProveedorController {

    private final CatalogoProveedorService catalogoProveedorService;

    // 🟢 OPTIMIZADO PARA TU JS (FormData): Asociar producto (Checkbox Marcado)
    @PostMapping(value = "/asociar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> asociarProducto(@Valid CatalogoProveedorDTO.Create createDto, 
                                               org.springframework.validation.BindingResult result) {
        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }

        try {
            catalogoProveedorService.asociarProducto(createDto);
            return Map.of("status", "OK", "message", "Producto asignado al catálogo con éxito.");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return Map.of("status", "ERROR", "message", e.getReason());
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo asociar el producto.");
        }
    }

    // 🟢 OPTIMIZADO PARA TU JS (FormData / Query): Desasociar producto (Checkbox Desmarcado)
    @PostMapping("/desasociar")
    @ResponseBody
    public Map<String, Object> desasociarProducto(@RequestParam Long idProveedor, @RequestParam Long idProducto) {
        try {
            catalogoProveedorService.desasociarProducto(idProveedor, idProducto);
            return Map.of("status", "OK", "message", "Producto retirado del catálogo.");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return Map.of("status", "ERROR", "message", e.getReason());
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo retirar el producto.");
        }
    }

    // 🔍 RUTA CRÍTICA PARA LA LUPA DE COMPRAS: Retorna los productos del proveedor en tiempo real
    @GetMapping("/proveedor/{idProveedor}/productos")
    @ResponseBody
    public ResponseEntity<List<CatalogoProveedorDTO.ProductoCompraResponse>> listarProductosPorProveedor(@PathVariable Long idProveedor) {
        List<CatalogoProveedorDTO.ProductoCompraResponse> productosFiltrados = catalogoProveedorService.listarProductosPorProveedor(idProveedor);
        return ResponseEntity.ok(productosFiltrados);
    }
        // =========================================================================
    // CORRECCIÓN: Agrega la anotación GetMapping explícita para que el JS la encuentre
    // =========================================================================
    @GetMapping("/listarTodo")
    @ResponseBody
    public List<CatalogoProveedorDTO.SimpleResponse> listarTodo() {
        return catalogoProveedorService.listarTodo();
    }

    @GetMapping("/buscar")
    @ResponseBody
    public List<CatalogoProveedorDTO.ProveedorCatalogoResponse> buscarProveedores(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Integer limite) {
        return catalogoProveedorService.buscarProveedores(texto, limite);
    }
    // =========================================================================

}
