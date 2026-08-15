package com.gas.sistema_gas.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gas.sistema_gas.dto.ConfirmarEntregaMixtaDTO;
import com.gas.sistema_gas.dto.PagoRegistroDTO;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;
import com.gas.sistema_gas.service.PedidoPagosService;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.EmpleadoDTO;
import com.gas.sistema_gas.service.EmpleadoService;
import com.gas.sistema_gas.service.PedidoService;
import com.gas.sistema_gas.service.UsuarioService;
import com.gas.sistema_gas.service.MetodoPagoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/motorizado")
public class MotorizadoController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoPagosService pedidoPagosService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private MetodoPagoService metodoPagoService;

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

        // Obtener el ID del empleado (motorizado) para WebSocket
        Long empleadoId = usuarioOpt
                .map(usuario -> {
                    var empleado = usuario.getEmpleado();
                    return empleado != null ? empleado.getId() : null;
                })
                .orElse(null);

        model.addAttribute("ordenesAsignadas", ordenesAsignadas);
        model.addAttribute("empleadoId", empleadoId);
        return "repartidor/asignados";
    }

    @GetMapping("/historial")
    public String historial(Model model, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        var pedidosEntregados = usuarioOpt
                .map(usuario -> {
                    var empleado = usuario.getEmpleado();
                    if (empleado == null || empleado.getId() == null) {
                        return java.util.List.<PedidoDTO.SimpleResponse>of();
                    }
                    return pedidoService.listEntregadosByEmpleadoId(empleado.getId());
                })
                .orElseGet(java.util.List::of);

        Long empleadoId = usuarioOpt
                .map(usuario -> {
                    var empleado = usuario.getEmpleado();
                    return empleado != null ? empleado.getId() : null;
                })
                .orElse(null);

        model.addAttribute("pedidosEntregados", pedidosEntregados);
        model.addAttribute("empleadoId", empleadoId);
        return "repartidor/historial";
    }

    @GetMapping("/perfil")
    public String perfil(Model model, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            var usuarioDTO = usuarioService.obtenerPerfilMotorizado(usuarioId);
            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("empleado", usuarioDTO);

            Long empleadoId = null;
            if (usuarioDTO != null) {
                try {
                    empleadoId = usuarioRepository.findById(usuarioId)
                            .flatMap(u -> java.util.Optional.ofNullable(u.getEmpleado()))
                            .map(e -> e.getId())
                            .orElse(null);
                } catch (Exception ignored) {
                    empleadoId = null;
                }
            }

            if (empleadoId != null) {
                var hoyInicio = java.time.LocalDate.now().atStartOfDay();
                var hoyFin = hoyInicio.plusDays(1);
                Long pedidosHoy = pedidoRepository.countPedidosHoyPorEmpleado(empleadoId, hoyInicio, hoyFin);
                Long entregasTotales = pedidoRepository.countEntregasPorEmpleadoYEstado(empleadoId, "ENTREGADO");

                model.addAttribute("pedidosHoy", pedidosHoy != null ? pedidosHoy : 0L);
                model.addAttribute("entregasTotales", entregasTotales != null ? entregasTotales : 0L);
            } else {
                model.addAttribute("pedidosHoy", 0L);
                model.addAttribute("entregasTotales", 0L);
            }
        } catch (Exception ex) {
            model.addAttribute("usuario", null);
            model.addAttribute("empleado", null);
            model.addAttribute("pedidosHoy", 0L);
            model.addAttribute("entregasTotales", 0L);
        }

        return "repartidor/perfil";
    }

    @GetMapping("/perfil/editar")
    public String perfilEditar(Model model, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null) {
            return "redirect:/login";
        }

        var usuario = usuarioOpt.get();
        var empleado = usuario.getEmpleado();

        var empleadoForm = new Empleado();
        empleadoForm.setId(empleado.getId());
        empleadoForm.setNombre(empleado.getNombre());
        empleadoForm.setDni(empleado.getDni());
        empleadoForm.setTelefono(empleado.getTelefono());
        empleadoForm.setCorreo(usuario.getCorreo());

        model.addAttribute("empleado", empleadoForm);
        return "views/viewsMotorizado/perfil-editar";
    }

    @PostMapping("/perfil/actualizar")
    @ResponseBody
    public ResponseEntity<?> actualizarPerfil(@Valid @ModelAttribute("empleado") Empleado empleado,
                                               BindingResult bindingResult,
                                               HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("success", false, "message", "No autenticado"));
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("success", false, "message", "No autenticado"));
        }

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new LinkedHashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "errors", errors));
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null || usuarioOpt.get().getEmpleado().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("success", false, "message", "Empleado no válido"));
        }

        Long empleadoId = usuarioOpt.get().getEmpleado().getId();
        var empleadoActual = empleadoRepository.findById(empleadoId).orElse(null);
        if (empleadoActual == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("success", false, "message", "Empleado no encontrado"));
        }

        empleadoActual.setNombre(empleado.getNombre() != null ? empleado.getNombre() : empleadoActual.getNombre());
        empleadoActual.setTelefono(empleado.getTelefono() != null ? empleado.getTelefono() : empleadoActual.getTelefono());

        empleadoService.updatePerfil(
                empleadoId,
                empleadoActual.getNombre(),
                empleadoActual.getTelefono(),
                null,
                empleado.getCorreo() != null ? empleado.getCorreo() : usuarioOpt.get().getCorreo()
        );

        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Cambios guardados correctamente"));
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

        if ("ENTREGADO".equalsIgnoreCase(estado)) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", "La entrega debe confirmarse mediante el flujo de pago"));
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La entrega debe confirmarse mediante el flujo de pago");
        }

        PedidoDTO.SimpleResponse updated = pedidoService.updateEstadoPedido(id, estado);
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            String estadoResp = updated != null ? (updated.estadoPedido() == null ? estado : updated.estadoPedido()) : estado;
            return ResponseEntity.ok(java.util.Map.of("success", true, "estado", estadoResp));
        }

        return "redirect:/motorizado/detalle?id=" + id;
    }

    @PostMapping("/pedido/desasignar")
    @ResponseBody
    public ResponseEntity<?> desasignarPedido(@RequestParam("id") Long id,
                                              HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("success", false, "message", "No autenticado"));
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("success", false, "message", "No autenticado"));
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null || usuarioOpt.get().getEmpleado().getId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("success", false, "message", "Empleado no válido"));
        }

        Long empleadoId = usuarioOpt.get().getEmpleado().getId();
        if (!pedidoService.existsByIdAndEmpleadoId(id, empleadoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("success", false, "message", "Pedido no asignado a este motorizado"));
        }

        try {
            pedidoService.desasignarPedido(id);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Pedido desasignado correctamente"
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(java.util.Map.of("success", false, "message", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("success", false, "message", "Error al desasignar pedido: " + e.getMessage()));
        }
    }

    @GetMapping({"/detalle", "/detalle/{id}"})
    public String detalle(@PathVariable(value = "id", required = false) Long id,
                          @RequestParam(value = "id", required = false) Long queryId,
                          Model model, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        Long pedidoId = id != null ? id : queryId;
        if (pedidoId == null) {
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

        Long empleadoId = usuarioOpt.get().getEmpleado().getId();
        try {
            var pedido = pedidoService.findByIdAndEmpleadoId(pedidoId, empleadoId);
            model.addAttribute("pedido", pedido);
            model.addAttribute("canjesPendientes", pedidoService.getEditData(pedidoId).detalles().stream()
                    .filter(detalle -> detalle.cantidadCanje() != null && detalle.cantidadCanje() > 0)
                    .toList());
        } catch (Exception ex) {
            return "redirect:/motorizado/asignados";
        }

        model.addAttribute("empleadoId", empleadoId);
        model.addAttribute("metodosPago", metodoPagoService.listActive());
        return "repartidor/detalle";
    }

    @GetMapping({"/venta-detalle", "/venta-detalle/{id}"})
    public String ventaDetalle(@PathVariable(value = "id", required = false) Long id,
                               @RequestParam(value = "id", required = false) Long queryId,
                               Model model, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        Long pedidoId = id != null ? id : queryId;
        if (pedidoId == null) {
            return "redirect:/motorizado/historial";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null || usuarioOpt.get().getEmpleado().getId() == null) {
            return "redirect:/motorizado/historial";
        }

        try {
            var pedido = pedidoService.findByIdAndEmpleadoId(pedidoId, usuarioOpt.get().getEmpleado().getId());
            model.addAttribute("pedido", pedido);
        } catch (Exception ex) {
            return "redirect:/motorizado/historial";
        }

        return "repartidor/venta-detalle";
    }

    @PostMapping("/pedido/pagar-yape")
    public Object pagarYape(@RequestParam("idPedido") Long idPedido,
                            @RequestParam(value = "idMetodo", required = false) Long idMetodo,
                            @RequestParam("montoRecibido") java.math.BigDecimal montoRecibido,
                             @RequestParam(value = "numOperacion", required = false) String numOperacion,
                            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia,
                            @RequestPart(value = "evidenciaVuelto", required = false) MultipartFile evidenciaVuelto,
                            HttpSession session,
                            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null
                || usuarioOpt.get().getEmpleado().getId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Empleado no válido"));
        }

        Long empleadoId = usuarioOpt.get().getEmpleado().getId();
        if (!pedidoService.existsByIdAndEmpleadoId(idPedido, empleadoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Pedido no asignado a este motorizado"));
        }

        PedidoPagoYapeDTO dto = new PedidoPagoYapeDTO(idPedido, idMetodo, montoRecibido, numOperacion);
        var pago = pedidoPagosService.confirmarEntregaPagoUnico(dto, evidencia, evidenciaVuelto, usuarioId);

        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "idPago", pago.getId()));
        }
        return "redirect:/motorizado/asignados";
    }

    @PostMapping("/pedido/confirmar-entrega")
    @ResponseBody
    public ResponseEntity<?> confirmarEntrega(
            @RequestParam("idPedido") Long idPedido,
            @RequestParam("pagosJson") String pagosJson,
            @RequestPart(value = "evidencias", required = false) List<MultipartFile> evidencias,
            @RequestPart(value = "evidenciaVuelto", required = false) MultipartFile evidenciaVuelto,
            HttpSession session) {

        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null || usuarioOpt.get().getEmpleado().getId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Empleado no válido"));
        }

        Long empleadoId = usuarioOpt.get().getEmpleado().getId();
        if (!pedidoService.existsByIdAndEmpleadoId(idPedido, empleadoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Pedido no asignado a este motorizado"));
        }

        try {
            // Parsear JSON de pagos
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<PagoRegistroDTO> pagos = mapper.readValue(pagosJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, PagoRegistroDTO.class));

            ConfirmarEntregaMixtaDTO dto = new ConfirmarEntregaMixtaDTO(idPedido, pagos);
            List<PedidoPago> pagosGuardados = pedidoPagosService
                    .confirmarEntregaConPagos(dto, evidencias, evidenciaVuelto, usuarioId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pago registrado correctamente",
                    "totalPagos", pagosGuardados.size()
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("success", false,
                            "message", e.getReason() != null ? e.getReason() : "Error de negocio"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al procesar el pago: " + e.getMessage()));
        }
    }

}
