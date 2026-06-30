package com.gas.sistema_gas.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.gas.sistema_gas.Model.AsignacionMoto;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.Repository.AsignacionMotoRepository;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AsignacionMotoRepository asignacionMotoRepository;

    @GetMapping
    public String ventas() {
        return "redirect:/ventas/local";
    }

    @GetMapping("/local")
    public String ventasLocal(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");

        List<PedidoDTO.SimpleResponse> ventasList = pedidoService.listByTipoVenta("LOCAL");

        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("ventas", ventasList);
        model.addAttribute("productos", productoService.listAll());
        model.addAttribute("categorias", categoriaService.listAll());
        model.addAttribute("metodosPago", metodoPagoService.listActive());
        model.addAttribute("contenido", "views/ventas_local");
        return "components/layout";
    }

    @GetMapping("/domicilio")
    public String ventasDomicilio(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        List<PedidoDTO.SimpleResponse> ventasList = pedidoService.listByTipoVenta("DOMICILIO");
        
        // Si es motorizado (perfil 4), solo ve las ventas que tiene asignadas
        if (perfilId != null && perfilId == 4L && usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null && usuario.getEmpleado() != null) {
                String nombreMoto = usuario.getEmpleado().getNombre();
                ventasList = ventasList.stream()
                        .filter(v -> nombreMoto.equals(v.nombreEmpleado()))
                        .collect(Collectors.toList());
            } else {
                ventasList = List.of(); // Si el motorizado no tiene empleado asociado
            }
        }

        // Obtener solo los motorizados que tienen una moto activa asignada en este momento
        List<Empleado> motorizadosActivos = asignacionMotoRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> a.getEstado() == AsignacionMoto.EstadoAsignacion.ACTIVA)
                .map(AsignacionMoto::getEmpleado)
                .distinct()
                .collect(Collectors.toList());

        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("ventas", ventasList);
        model.addAttribute("productos", productoService.listAll());
        model.addAttribute("categorias", categoriaService.listAll());
        model.addAttribute("metodosPago", metodoPagoService.listActive());
        model.addAttribute("motorizados", motorizadosActivos);
        model.addAttribute("contenido", "views/ventas_domicilio");
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

        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        if (perfilId != null && perfilId == 4L && pedidoDto.idPedido() != null) {
            PedidoDTO.SimpleResponse pedidoExistente = pedidoService.findById(pedidoDto.idPedido());
            if (!"PENDIENTE".equalsIgnoreCase(pedidoExistente.estadoPedido())) {
                return Map.of("status", "ERROR", "message", "Acceso denegado: El pedido ya está completado y no puede ser modificado.");
            }
        }

        try {
            pedidoService.createOrder(pedidoDto, idUsuarioLogueado);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<PedidoDTO.EditResponse> datosParaEditar(@PathVariable Long id) {
        PedidoDTO.EditResponse response = pedidoService.getEditData(id);
        return ResponseEntity.ok(response);
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
