package com.gas.sistema_gas.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.service.ClienteService;
import com.gas.sistema_gas.service.EmpleadoService;
import com.gas.sistema_gas.service.MetodoPagoService;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PedidoService;
import com.gas.sistema_gas.service.ProductoService;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private OpcionService opcionService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private com.gas.sistema_gas.service.CategoriaService categoriaService;

    @Autowired
    private MetodoPagoService metodoPagoService;

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private PedidoPagoRepository pedidoPagoRepository;

    @GetMapping
    public String ventas(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("ventas", pedidoService.listAll());
        model.addAttribute("productos", productoService.listAll());
        model.addAttribute("categorias", categoriaService.listAll());
        model.addAttribute("metodosPago", metodoPagoService.listActive());
        model.addAttribute("motorizados", empleadoService.listDisponibles());
        model.addAttribute("contenido", "views/ventas");
        return "components/layout";
    }

    @PostMapping
    @ResponseBody
    public Map<String, Object> registrarVenta(@Valid @RequestBody PedidoDTO.Create pedidoDto,
                                               BindingResult result,
                                               HttpSession session) {
        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }

        Long idUsuarioLogueado = (Long) session.getAttribute("usuarioId");
        if (idUsuarioLogueado == null) {
            return Map.of("status", "ERROR", "message", "No se ha identificado al usuario logueado");
        }

        try {
            pedidoService.createOrder(pedidoDto, idUsuarioLogueado);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    @GetMapping("/cliente")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarClientePorDni(@RequestParam String dni) {
        var cliente = clienteService.findByDni(dni);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("id", cliente.id());
        respuesta.put("nombre", cliente.nombre());
        respuesta.put("dni", cliente.dni());
        respuesta.put("telefono", cliente.telefono());
        respuesta.put("direccion", cliente.direccion());
        respuesta.put("referencia", cliente.referencia());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalleVenta(@PathVariable Long id) {
        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_Id(id);
        List<Map<String, Object>> detalleItems = detalles.stream().map(det -> {
            Map<String, Object> item = new HashMap<>();
            item.put("producto", det.getProducto().getNombre());
            item.put("cantidad", det.getCantidad());
            item.put("precioUnitario", det.getPrecioUnitario());
            item.put("subtotal", det.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(det.getCantidad())));
            return item;
        }).toList();

        List<PedidoPago> pagos = pedidoPagoRepository.findByPedido_Id(id);
        List<Map<String, Object>> pagoItems = pagos.stream().map(pago -> {
            Map<String, Object> item = new HashMap<>();
            item.put("metodo", pago.getMetodoPago().getNombre());
            item.put("monto", pago.getMonto());
            item.put("numOperacion", pago.getNumOperacion());
            return item;
        }).toList();

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("detalles", detalleItems);
        resultado.put("pagos", pagoItems);
        return ResponseEntity.ok(resultado);
    }
}
