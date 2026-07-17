package com.gas.sistema_gas.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

import java.util.LinkedHashMap;

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
import com.gas.sistema_gas.dto.EmpleadoDTO;
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

    @Autowired
    private com.gas.sistema_gas.Repository.ControlEnvaseRepository controlEnvaseRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public String ventas() {
        return "redirect:/ventas/local";
    }

    @GetMapping("/local")
    public String ventasLocal(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        if (perfilId != null && perfilId == 4L) {
            return "redirect:/motorizado/asignados";
        }

        populateVentasModel(model, session, "local");
        return "components/layout";
    }

    @GetMapping("/domicilio")
    public String ventasDomicilio(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        if (perfilId != null && perfilId == 4L) {
            return "redirect:/motorizado/asignados";
        }

        populateVentasModel(model, session, "domicilio");
        return "components/layout";
    }

    private void populateVentasModel(Model model, HttpSession session, String activeTab) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        List<PedidoDTO.SimpleResponse> ventasLocalList = pedidoService.listByTipoVenta("LOCAL");
        List<PedidoDTO.SimpleResponse> ventasDomicilioList = pedidoService.listByTipoVenta("DOMICILIO");

        // Si es motorizado (perfil 4), NO mostrar la vista general y sólo ver sus domicilios asignados
        if (perfilId != null && perfilId == 4L) {
            ventasLocalList = List.of();
            if (usuarioId != null) {
                Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
                if (usuario != null && usuario.getEmpleado() != null) {
                    String nombreMoto = usuario.getEmpleado().getNombre();
                    ventasDomicilioList = ventasDomicilioList.stream()
                            .filter(v -> nombreMoto.equals(v.nombreEmpleado()))
                            .collect(Collectors.toList());
                } else {
                    ventasDomicilioList = List.of();
                }
            } else {
                ventasDomicilioList = List.of();
            }
        }

        // Obtener solo los motorizados que tienen una moto activa asignada en este momento
        List<EmpleadoDTO.SimpleResponse> motorizadosActivos = asignacionMotoRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> a.getEstado() == AsignacionMoto.EstadoAsignacion.ACTIVA)
                .map(AsignacionMoto::getEmpleado)
                .distinct()
                .map(e -> new EmpleadoDTO.SimpleResponse(e.getId(), e.getNombre(), e.getDni(), e.getTelefono(), e.getSueldoBase(), e.getEstado()))
                .collect(Collectors.toList());

        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("ventasLocal", ventasLocalList);
        model.addAttribute("ventasDomicilio", ventasDomicilioList);
        model.addAttribute("productos", productoService.listAll());
        model.addAttribute("categorias", categoriaService.listAll());
        model.addAttribute("metodosPago", metodoPagoService.listActive());
        model.addAttribute("motorizados", motorizadosActivos);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("contenido", "views/ventas");
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
            PedidoDTO.SimpleResponse response = pedidoService.createOrder(pedidoDto, idUsuarioLogueado);

            // Enviar notificación en tiempo real al motorizado si el pedido es a domicilio y tiene empleado asignado
            if (response != null 
                && "DOMICILIO".equalsIgnoreCase(response.tipoVenta()) 
                && response.empleadoId() != null) {
                messagingTemplate.convertAndSend("/topic/pedidos/" + response.empleadoId(), response);
            }

            return Map.of("status", "OK", "pedido", response);
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
        List<com.gas.sistema_gas.Model.ControlEnvase> prestamos = controlEnvaseRepository.findByPedido_Id(id);
        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_Id(id);
        List<Map<String, Object>> detalleItems = detalles.stream().map(det -> {
            Map<String, Object> item = new HashMap<>();
            item.put("producto", det.getProducto().getNombre());
            item.put("cantidad", det.getCantidad());
            item.put("precioUnitario", det.getPrecioUnitario());
            item.put("subtotal", det.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(det.getCantidad())));
            
            Integer cantPrestada = prestamos.stream()
                .filter(p -> p.getProducto().getId().equals(det.getProducto().getId()))
                .map(com.gas.sistema_gas.Model.ControlEnvase::getCantidadPrestada)
                .findFirst()
                .orElse(0);
            item.put("cantidadPrestada", cantPrestada);
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

    @GetMapping("/estado-actual")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> estadoActualPedidos(HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        if (perfilId == null) {
            return ResponseEntity.status(403).build();
        }

        List<PedidoDTO.SimpleResponse> pedidos = pedidoService.listByTipoVenta("DOMICILIO");
        List<Map<String, Object>> respuesta = pedidos.stream().map(pedido -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idPedido", pedido.idPedido());
            item.put("estadoPedido", pedido.estadoPedido());
            item.put("estadoPago", pedido.estadoPago());
            item.put("metodoPago", pedido.metodoPago());
            return item;
        }).toList();

        return ResponseEntity.ok(respuesta);
    }
}
