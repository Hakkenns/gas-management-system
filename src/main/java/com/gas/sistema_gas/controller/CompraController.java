package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.gas.sistema_gas.dto.CompraDTO;
import com.gas.sistema_gas.service.*;
import jakarta.servlet.http.HttpSession;

import java.util.Map;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;

@Controller
@RequestMapping("/compras")
public class CompraController {

    @Autowired private CompraService compraService;
    @Autowired private OpcionService opcionService;
    @Autowired private ProveedorService proveedorService;
    @Autowired private ProductoService productoService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private InventarioLoteRepository inventarioLoteRepository;

    private void cargarModeloCompras(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("compras", compraService.listAll());
        model.addAttribute("proveedores", proveedorService.listAll());
        model.addAttribute("productos", productoService.listAll());
        model.addAttribute("categorias", categoriaService.listAll().stream()
                .filter(cat -> cat.estado() != null && cat.estado() == 1)
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping
    public String compras(Model model, HttpSession session) {
        cargarModeloCompras(model, session);
        model.addAttribute("contenido", "views/compras");
        return "components/layout";
    }

    @GetMapping("/fragment")
    public String fragmentoCompras(Model model, HttpSession session) {
        cargarModeloCompras(model, session);
        return "views/compras :: content";
    }

    @GetMapping("/tabla")
    public String tablaCompras(Model model) {
        model.addAttribute("compras", compraService.listAll());
        return "views/compras :: tablaCompras";
    }

    @PostMapping
    @ResponseBody
    public Map<String, Object> guardarCompra(@Valid @RequestBody CompraDTO.Create compraDto, BindingResult result, HttpSession session) {
        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }
        try {
            Long idUsuarioLogueado = (Long) session.getAttribute("usuarioId");
            if (idUsuarioLogueado == null) {
                return Map.of("status", "ERROR", "message", "No se ha identificado al usuario logueado");
            }
            compraService.create(compraDto, idUsuarioLogueado);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    @PostMapping("/{id}/anular")
    @ResponseBody
    public Map<String, Object> anularCompra(@PathVariable Long id) {
        try {
            compraService.anularCompra(id);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<CompraDTO.DetailResponse> getDetalleByCompra(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.getDetalleByCompraId(id));
    }

    @GetMapping("/ultimo-costo")
    @ResponseBody
    public Map<String, Object> getUltimoCosto(@RequestParam Long idProducto, @RequestParam Long idProveedor) {
        var lotes = inventarioLoteRepository.findUltimoPrecioCosto(idProducto, idProveedor);
        if (lotes != null && !lotes.isEmpty()) {
            var precio = lotes.get(0).getPrecioCompra();
            return Map.of("precioCosto", precio);
        }
        return Map.of("precioCosto", 0.00);
    }
}
