package com.gas.sistema_gas.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;
import com.gas.sistema_gas.service.PedidoPagosService;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.service.PedidoService;

@Controller
@RequestMapping("/motorizado")
public class MotorizadoController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoPagosService pedidoPagosService;

    @GetMapping
    public String root(HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }
        return "redirect:/motorizado/asignados";
    }

    @GetMapping("/asignados")
    public String asignados(Model model, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        var ordenesAsignadas = usuarioOpt
                .map(usuario -> {
                    var empleado = usuario.getEmpleado();
                    if (empleado == null || empleado.getId() == null) {
                        return java.util.List.<com.gas.sistema_gas.dto.PedidoDTO.SimpleResponse>of();
                    }
                    return pedidoService.listByTipoVentaAndEmpleadoId("DOMICILIO", empleado.getId());
                })
                .orElseGet(java.util.List::of);

        model.addAttribute("ordenesAsignadas", ordenesAsignadas);
        return "views/viewsMotorizado/asignados";
    }

    @GetMapping("/historial")
    public String historial(HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }
        return "views/viewsMotorizado/historial";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }
        return "views/viewsMotorizado/perfil";
    }

    @PostMapping("/pedido/estado")
    public Object actualizarEstadoPedido(@RequestParam("id") Long id,
                                         @RequestParam("estado") String estado,
                                         HttpSession session,
                                         @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(401).body(java.util.Map.of("success", false, "message", "No autenticado"));
            }
            return "redirect:/login";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null || usuarioOpt.get().getEmpleado().getId() == null) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(403).body(java.util.Map.of("success", false, "message", "Empleado no válido"));
            }
            return "redirect:/motorizado/asignados";
        }

        Long empleadoId = usuarioOpt.get().getEmpleado().getId();
        if (!pedidoService.existsByIdAndEmpleadoId(id, empleadoId)) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(403).body(java.util.Map.of("success", false, "message", "Pedido no asignado a este motorizado"));
            }
            return "redirect:/motorizado/asignados";
        }

        PedidoDTO.SimpleResponse updated = pedidoService.updateEstadoPedido(id, estado);
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            String estadoResp = updated != null ? (updated.estadoPedido() == null ? estado : updated.estadoPedido()) : estado;
            return ResponseEntity.ok(java.util.Map.of("success", true, "estado", estadoResp));
        }

        return "redirect:/motorizado/detalle?id=" + id;
    }

    @GetMapping("/detalle")
    public String detalle(@RequestParam(value = "id", required = false) Long id, Model model, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        if (id == null) {
            return "redirect:/motorizado/asignados";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null || usuarioOpt.get().getEmpleado().getId() == null) {
            return "redirect:/motorizado/asignados";
        }

        try {
            var pedido = pedidoService.findByIdAndEmpleadoId(id, usuarioOpt.get().getEmpleado().getId());
            model.addAttribute("pedido", pedido);
        } catch (Exception ex) {
            return "redirect:/motorizado/asignados";
        }

        return "views/viewsMotorizado/detalle";
    }

    @PostMapping("/pedido/pagar-yape")
    public Object pagarYape(@RequestParam("idPedido") Long idPedido,
                            @RequestParam(value = "idMetodo", required = false) Long idMetodo,
                            @RequestParam("montoRecibido") java.math.BigDecimal montoRecibido,
                            @RequestParam("numOperacion") String numOperacion,
                            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia,
                            HttpSession session,
                            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        boolean requiereOperacion = idMetodo != null && (idMetodo == 2L || idMetodo == 3L);
        if (requiereOperacion && (numOperacion == null || numOperacion.isBlank())) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("success", false, "message", "Número de operación requerido para Yape/Plin"));
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número de operación requerido para Yape/Plin");
        }

        PedidoPagoYapeDTO dto = new PedidoPagoYapeDTO(idPedido, idMetodo, montoRecibido, numOperacion);
        var pago = pedidoPagosService.registrarPagoYape(dto, evidencia);

        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "idPago", pago.getId()));
        }
        return "redirect:/motorizado/asignados";
    }
}
