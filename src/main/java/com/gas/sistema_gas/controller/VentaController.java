package com.gas.sistema_gas.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PedidoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private OpcionService opcionService;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @GetMapping
    public String ventas(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("ventas", pedidoService.listAll());
        model.addAttribute("contenido", "views/ventas");
        return "components/layout";
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> detalleVenta(@PathVariable Long id) {
        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_Id(id);
        List<Map<String, Object>> resultado = detalles.stream().map(det -> {
            Map<String, Object> item = new HashMap<>();
            item.put("producto", det.getProducto().getNombre());
            item.put("cantidad", det.getCantidad());
            item.put("precioUnitario", det.getPrecioUnitario());
            item.put("subtotal", det.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(det.getCantidad())));
            return item;
        }).toList();
        return ResponseEntity.ok(resultado);
    }
}
