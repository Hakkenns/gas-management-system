package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.gas.sistema_gas.dto.CompraDTO;
import com.gas.sistema_gas.Model.DetalleCompra;
import com.gas.sistema_gas.service.*;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/compras")
public class CompraController {

    @Autowired private CompraService compraService;
    @Autowired private OpcionService opcionService;
    @Autowired private ProveedorService proveedorService;
    @Autowired private ProductoService productoService;

    @GetMapping
    public String compras(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("compras", compraService.listAll());
        model.addAttribute("proveedores", proveedorService.listAll()); // Para poblar el select de proveedores
        model.addAttribute("productos", productoService.listAll());     // Para poblar el select de productos
        model.addAttribute("contenido", "views/compras");
        return "components/layout";
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
    public ResponseEntity<List<Map<String, Object>>> getDetalleByCompra(@PathVariable Long id) {
        List<DetalleCompra> detalles = compraService.listDetallesByCompraId(id);
        List<Map<String, Object>> res = detalles.stream().map(d -> {
            var producto = d.getProducto();
            var capacidad = producto.getCapacidad();
            var unidad = producto.getUnidadMedida();
            String unidadLabel = "";
            if ("KG".equals(unidad)) unidadLabel = " kg";
            else if ("L".equals(unidad)) unidadLabel = " L";
            else if ("M".equals(unidad)) unidadLabel = " m";
            String nombreCompleto = producto.getNombre();
            if (capacidad != null) {
                nombreCompleto = nombreCompleto + " - " + capacidad + unidadLabel;
            }
            
            var cantidadBD = java.math.BigDecimal.valueOf(d.getCantidad());
            var precio = d.getPrecioCostoUnitario();
            
            if ("M".equals(unidad) && capacidad != null && capacidad.compareTo(java.math.BigDecimal.ZERO) > 0) {
                cantidadBD = cantidadBD.divide(capacidad, 2, java.math.RoundingMode.HALF_UP);
                precio = precio.multiply(capacidad);
            }
            
            Map<String, Object> m = new HashMap<>();
            m.put("producto", nombreCompleto);
            m.put("cantidad", cantidadBD);
            m.put("precio", precio);
            m.put("unidad", unidad);
            m.put("capacidad", capacidad);
            return m;
        }).toList();
        return ResponseEntity.ok(res);
    }
}