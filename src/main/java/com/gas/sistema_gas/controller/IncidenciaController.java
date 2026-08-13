package com.gas.sistema_gas.controller;

import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.Incidencia;
import com.gas.sistema_gas.Model.Moto;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.RespuestaIncidencia;
import com.gas.sistema_gas.Repository.AsignacionMotoRepository;
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Repository.IncidenciaRepository;
import com.gas.sistema_gas.Repository.MotoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.RespuestaIncidenciaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.service.InventarioLoteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

    private static final String MENSAJE_PAGO_REQUIERE_REEMBOLSO =
            "El pedido tiene pagos registrados y requiere un reembolso antes de poder cancelarse";

    private final IncidenciaRepository incidenciaRepository;
    private final RespuestaIncidenciaRepository respuestaIncidenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoPagoRepository pedidoPagoRepository;
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionMotoRepository asignacionMotoRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DetallePedidoRepository detallePedidoRepository;
    private final InventarioLoteRepository inventarioLoteRepository;
    private final ProductoRepository productoRepository;
    private final InventarioLoteService inventarioLoteService;

    public IncidenciaController(IncidenciaRepository incidenciaRepository,
                                RespuestaIncidenciaRepository respuestaIncidenciaRepository,
                                EmpleadoRepository empleadoRepository,
                                PedidoRepository pedidoRepository,
                                PedidoPagoRepository pedidoPagoRepository,
                                MotoRepository motoRepository,
                                UsuarioRepository usuarioRepository,
                                AsignacionMotoRepository asignacionMotoRepository,
                                SimpMessagingTemplate messagingTemplate,
                                DetallePedidoRepository detallePedidoRepository,
                                InventarioLoteRepository inventarioLoteRepository,
                                ProductoRepository productoRepository,
                                InventarioLoteService inventarioLoteService) {
        this.incidenciaRepository = incidenciaRepository;
        this.respuestaIncidenciaRepository = respuestaIncidenciaRepository;
        this.empleadoRepository = empleadoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoPagoRepository = pedidoPagoRepository;
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionMotoRepository = asignacionMotoRepository;
        this.messagingTemplate = messagingTemplate;
        this.detallePedidoRepository = detallePedidoRepository;
        this.inventarioLoteRepository = inventarioLoteRepository;
        this.productoRepository = productoRepository;
        this.inventarioLoteService = inventarioLoteService;
    }

    // POST /api/incidencias/reportar - Repartidor reporta incidencia con foto (RECHAZO_POST_LLEGADA o CLIENTE_AUSENTE)
    @PostMapping("/reportar")
    @Transactional
    public ResponseEntity<?> reportarIncidencia(
            @RequestParam("idPedido") Long idPedido,
            @RequestParam("tipoIncidencia") String tipoIncidencia,
            @RequestParam("evidencia") MultipartFile evidencia,
            HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Empleado no válido"));
        }

        Empleado empleado = usuarioOpt.get().getEmpleado();

        // Validar tipo de incidencia permitido
        String tipoUpper = tipoIncidencia.toUpperCase();
        if (!"RECHAZO_POST_LLEGADA".equals(tipoUpper) && !"CLIENTE_AUSENTE".equals(tipoUpper)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tipo de incidencia no válido"));
        }

        // Validar que se haya subido una foto
        if (evidencia == null || evidencia.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "La foto de evidencia es obligatoria"));
        }

        // Validar que el pedido exista y esté asignado al empleado
        var pedidoOpt = pedidoRepository.findByIdForUpdate(idPedido);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Pedido no encontrado"));
        }

        Pedido pedido = pedidoOpt.get();
        if (pedido.getEmpleado() == null || !pedido.getEmpleado().getId().equals(empleado.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Pedido no asignado a este motorizado"));
        }

        // Validar que el pedido esté en estado EN_DOMICILIO
        if (!"EN_DOMICILIO".equalsIgnoreCase(pedido.getEstadoPedido())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "El pedido debe estar en estado EN_DOMICILIO para reportar incidencia"));
        }

        // Guardar la foto de evidencia
        String urlEvidencia = guardarEvidencia(evidencia, idPedido);

        // Crear la incidencia
        Incidencia incidencia = new Incidencia();
        incidencia.setEmpleado(empleado);
        incidencia.setPedido(pedido);
        incidencia.setUrlEvidencia(urlEvidencia);
        incidencia.setEstado("PENDIENTE");

        if ("RECHAZO_POST_LLEGADA".equals(tipoUpper)) {
            incidencia.setTipo("WARNING");
            incidencia.setTipoIncidencia("RECHAZO_POST_LLEGADA");
            incidencia.setTipoLabel("Rechazo de Pedido");
        } else {
            incidencia.setTipo("INFO");
            incidencia.setTipoIncidencia("CLIENTE_AUSENTE");
            incidencia.setTipoLabel("Cliente Ausente");
        }

        incidencia.setCreatedAt(LocalDateTime.now());
        incidencia.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidencia);

        // Actualizar estado del pedido según tipo
        if ("RECHAZO_POST_LLEGADA".equals(tipoUpper)) {
            // El pedido NO desaparece de la lista del motorizado - se queda visible con "EN_REVISION"
            pedido.setEstadoPedido("EN_REVISION");
            // mantenemos el empleado asignado
            pedidoRepository.save(pedido);
            // NO retornamos stock aún - eso lo hará el admin al confirmar
        } else {
            // CLIENTE_AUSENTE: el pedido NO desaparece, solo cambia badge
            pedido.setEstadoPedido("CLIENTE_AUSENTE");
            // estado_pago se mantiene en PENDIENTE
            pedidoRepository.save(pedido);
        }

        // Obtener datos completos para la notificación
        String nombreCliente = pedido.getCliente() != null ? pedido.getCliente().getNombre() : "Desconocido";
        String celularCliente = pedido.getCliente() != null ? pedido.getCliente().getTelefono() : "";
        String codigoPedido = pedido.getCodigo();
        String nombreMotorizado = empleado.getNombre();

        // Enviar notificación por WebSocket a los administradores con datos completos
        Map<String, Object> notificacion = new java.util.HashMap<>();
        notificacion.put("tipoIncidencia", tipoUpper);
        notificacion.put("idPedido", pedido.getId());
        notificacion.put("codigoPedido", codigoPedido);
        notificacion.put("nombreMotorizado", nombreMotorizado);
        notificacion.put("nombreCliente", nombreCliente);
        notificacion.put("celularCliente", celularCliente);
        notificacion.put("urlEvidencia", urlEvidencia);
        notificacion.put("idIncidencia", incidencia.getId());
        notificacion.put("createdAt", incidencia.getCreatedAt() != null ? incidencia.getCreatedAt().toString() : LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/admin/incidencias/detalle", notificacion);

        // También actualizar el contador
        long contadorPendientes = incidenciaRepository.countByEstado("PENDIENTE");
        messagingTemplate.convertAndSend("/topic/admin/incidencias", contadorPendientes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Incidencia reportada correctamente",
                "idIncidencia", incidencia.getId()
        ));
    }

    // POST /api/incidencias/confirmar-rechazo - Admin confirma rechazo
    @PostMapping("/confirmar-rechazo")
    @Transactional
    public ResponseEntity<?> confirmarRechazo(@RequestBody Map<String, Object> body, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        Long idPedido = null;
        if (body.get("idPedido") != null) {
            Object idPedidoObj = body.get("idPedido");
            if (idPedidoObj instanceof Number) {
                idPedido = ((Number) idPedidoObj).longValue();
            } else if (idPedidoObj instanceof String) {
                try {
                    idPedido = Long.parseLong((String) idPedidoObj);
                } catch (NumberFormatException e) {
                    idPedido = null;
                }
            }
        }

        if (idPedido == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "ID de pedido requerido"));
        }

        var pedidoOpt = pedidoRepository.findByIdForUpdate(idPedido);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Pedido pedido = pedidoOpt.get();
        String estadoActual = pedido.getEstadoPedido() != null ? pedido.getEstadoPedido().trim().toUpperCase() : "";

        // Idempotencia: si ya está RECHAZADO, no procesar
        if ("RECHAZADO".equals(estadoActual)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "El rechazo ya fue confirmado"));
        }

        // Validar estado previo
        if (!"EN_REVISION".equals(estadoActual)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "El pedido debe estar en EN_REVISION para confirmar el rechazo"));
        }

        // Buscar incidencias del pedido con bloqueo pesimista
        List<Incidencia> incidencias = incidenciaRepository.findByPedidoIdForUpdate(idPedido);

        // Localizar la incidencia pendiente exacta de tipo RECHAZO_POST_LLEGADA
        Incidencia incidenciaRechazo = incidencias.stream()
                .filter(inc -> "RECHAZO_POST_LLEGADA".equalsIgnoreCase(inc.getTipoIncidencia())
                        && "PENDIENTE".equalsIgnoreCase(inc.getEstado()))
                .findFirst()
                .orElse(null);

        if (incidenciaRechazo == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "No existe un rechazo pendiente para confirmar"));
        }

        if (pedidoPagoRepository.existsByPedido_IdAndMontoGreaterThan(idPedido, BigDecimal.ZERO)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", MENSAJE_PAGO_REQUIERE_REEMBOLSO));
        }

        Empleado empleadoAnterior = pedido.getEmpleado();
        String codigoPedido = pedido.getCodigo();

        // Devolver stock antes de cambiar estados
        inventarioLoteService.devolverStockDePedido(pedido.getId());

        // Cambiar estados del pedido
        pedido.setEstadoPedido("RECHAZADO");
        pedido.setEstadoPago("CANCELADO");
        pedido.setEmpleado(null);
        pedidoRepository.save(pedido);

        // Marcar la incidencia de rechazo como CONFIRMADO
        incidenciaRechazo.setEstado("CONFIRMADO");
        incidenciaRechazo.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidenciaRechazo);

        // Actualizar contador
        long contadorPendientes = incidenciaRepository.countByEstado("PENDIENTE");
        messagingTemplate.convertAndSend("/topic/admin/incidencias", contadorPendientes);

        // Notificar al motorizado
        if (empleadoAnterior != null) {
            String mensajeNotificacion = "El administrador ha revisado y aceptado el rechazo del pedido " + (codigoPedido != null ? codigoPedido : "N/A");

            RespuestaIncidencia respuesta = new RespuestaIncidencia();
            respuesta.setMotoReemplazo("");
            respuesta.setCreatedAt(LocalDateTime.now());
            respuesta.setUpdatedAt(LocalDateTime.now());
            respuesta.setIncidencia(incidenciaRechazo);
            respuestaIncidenciaRepository.save(respuesta);

            messagingTemplate.convertAndSend("/topic/repartidor/respuestas/" + empleadoAnterior.getId(),
                Map.of(
                    "mensaje", mensajeNotificacion,
                    "fecha", LocalDateTime.now().toString(),
                    "tipoIncidencia", "RECHAZO_POST_LLEGADA",
                    "motoReemplazo", ""
                )
            );
            messagingTemplate.convertAndSend("/topic/pedidos/" + empleadoAnterior.getId(),
                Map.of(
                    "mensaje", "rechazo_confirmado",
                    "idPedido", pedido.getId(),
                    "codigo", codigoPedido
                )
            );
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rechazo confirmado. Stock retornado."
        ));
    }

    // GET /api/incidencias/evidencia/{idIncidencia} - Admin visualiza evidencia
    @GetMapping("/evidencia/{idIncidencia}")
    public ResponseEntity<?> obtenerEvidencia(@PathVariable Long idIncidencia, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var incidenciaOpt = incidenciaRepository.findById(idIncidencia);
        if (incidenciaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Incidencia incidencia = incidenciaOpt.get();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "urlEvidencia", incidencia.getUrlEvidencia() != null ? incidencia.getUrlEvidencia() : ""
        ));
    }

    // POST /api/incidencias - Repartidor registra una incidencia (legacy, no tocar)
    @PostMapping
    public ResponseEntity<?> crearIncidencia(@RequestBody Map<String, Object> body, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Empleado no válido"));
        }

        Empleado empleado = usuarioOpt.get().getEmpleado();
        String tipoIncidencia = (String) body.get("tipoIncidencia");
        
        // Manejar idPedido que puede venir como String o Number desde el JSON
        Long idPedido = null;
        if (body.get("idPedido") != null) {
            Object idPedidoObj = body.get("idPedido");
            if (idPedidoObj instanceof Number) {
                idPedido = ((Number) idPedidoObj).longValue();
            } else if (idPedidoObj instanceof String) {
                try {
                    idPedido = Long.parseLong((String) idPedidoObj);
                } catch (NumberFormatException e) {
                    idPedido = null;
                }
            }
        }

        if (tipoIncidencia == null || tipoIncidencia.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "El tipo de incidencia es obligatorio"));
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setEmpleado(empleado);
        
        // Determinar el tipo general y label según el tipo de incidencia
        String tipoGeneral = "INCIDENCIA";
        String tipoLabel = "Incidencia";
        String tipoIncidenciaUpper = tipoIncidencia.toUpperCase();
        
        if ("AVERIA_VEHICULO".equalsIgnoreCase(tipoIncidencia) || "ACCIDENTE".equalsIgnoreCase(tipoIncidencia)) {
            tipoGeneral = "CRITICA";
            tipoLabel = "Alerta Crítica";
        } else if ("RECHAZO_PEDIDO".equalsIgnoreCase(tipoIncidencia)) {
            tipoGeneral = "WARNING";
            tipoLabel = "Advertencia de Entrega";
            // Mapear el motivo al tipo_incidencia específico
            String motivo = (String) body.get("motivo");
            if (motivo != null) {
                tipoIncidenciaUpper = motivo.toUpperCase();
            }
        } else if ("INCONVENIENTE_PUERTA".equalsIgnoreCase(tipoIncidencia)) {
            tipoGeneral = "INFO";
            tipoLabel = "Inconveniente en Punto";
            // Mapear el motivo al tipo_incidencia específico
            String motivo = (String) body.get("motivo");
            if (motivo != null) {
                tipoIncidenciaUpper = motivo.toUpperCase();
            }
        }
        
        incidencia.setTipo(tipoGeneral);
        incidencia.setTipoIncidencia(tipoIncidenciaUpper);
        incidencia.setTipoLabel(tipoLabel);
        incidencia.setEstado("PENDIENTE");

        if (idPedido != null) {
            pedidoRepository.findById(idPedido).ifPresent(incidencia::setPedido);
        }

        incidencia.setCreatedAt(LocalDateTime.now());
        incidencia.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidencia);

        // Enviar notificación por WebSocket a los administradores
        long contadorPendientes = incidenciaRepository.countByEstado("PENDIENTE");
        messagingTemplate.convertAndSend("/topic/admin/incidencias", contadorPendientes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Incidencia reportada correctamente",
                "idIncidencia", incidencia.getId()
        ));
    }

    // GET /api/incidencias/pendientes - Admin consulta alertas no atendidas
    @GetMapping("/pendientes")
    public ResponseEntity<?> obtenerPendientes(HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        try {
            List<Incidencia> pendientes = incidenciaRepository.findByEstadoOrderByCreatedAtDesc("PENDIENTE");

            List<Map<String, Object>> resultado = pendientes.stream().map(inc -> {
                String nombreEmpleado = inc.getEmpleado() != null ? inc.getEmpleado().getNombre() : "Desconocido";
                String telefonoEmpleado = inc.getEmpleado() != null ? inc.getEmpleado().getTelefono() : "";
                Long idPedido = inc.getPedido() != null ? inc.getPedido().getId() : null;
                String codigoPedido = inc.getPedido() != null ? inc.getPedido().getCodigo() : null;
                String nombreCliente = inc.getPedido() != null && inc.getPedido().getCliente() != null ? inc.getPedido().getCliente().getNombre() : "";
                String celularCliente = inc.getPedido() != null && inc.getPedido().getCliente() != null ? inc.getPedido().getCliente().getTelefono() : "";

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", inc.getId());
                map.put("tipoIncidencia", inc.getTipoIncidencia());
                map.put("tipoLabel", inc.getTipoLabel() != null ? inc.getTipoLabel() : "");
                map.put("estado", inc.getEstado());
                map.put("empleadoNombre", nombreEmpleado);
                map.put("empleadoTelefono", telefonoEmpleado);
                map.put("idPedido", idPedido);
                map.put("codigoPedido", codigoPedido);
                map.put("nombreCliente", nombreCliente);
                map.put("celularCliente", celularCliente);
                map.put("urlEvidencia", inc.getUrlEvidencia() != null ? inc.getUrlEvidencia() : "");
                map.put("createdAt", inc.getCreatedAt() != null ? inc.getCreatedAt().toString() : "");
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "incidencias", resultado,
                    "total", resultado.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al obtener incidencias: " + e.getMessage()));
        }
    }

    // POST /api/incidencias/{id}/atender - Admin responde una incidencia con reasignación de moto
    @PostMapping("/{id}/atender")
    @Transactional
    public ResponseEntity<?> atenderIncidencia(@PathVariable Long id,
                                                @RequestBody Map<String, String> body,
                                                HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var incidenciaOpt = incidenciaRepository.findById(id);
        if (incidenciaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Incidencia no encontrada"));
        }

        Incidencia incidencia = incidenciaOpt.get();
        String motoReemplazo = body.get("motoReemplazo");
        Long idMotoNueva = body.get("idMotoNueva") != null ? Long.parseLong(body.get("idMotoNueva")) : null;

        if (idMotoNueva == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Debe seleccionar una moto de reemplazo"));
        }

        var motoNuevaOpt = motoRepository.findById(idMotoNueva);
        if (motoNuevaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Moto no encontrada"));
        }

        Moto motoNueva = motoNuevaOpt.get();
        Empleado empleadoAfectado = incidencia.getEmpleado();

        // 1. Finalizar asignación anterior del empleado
        var asignacionActiva = asignacionMotoRepository
                .findByEmpleadoIdAndEstado(empleadoAfectado.getId(), com.gas.sistema_gas.Model.AsignacionMoto.EstadoAsignacion.ACTIVA);
        
        if (asignacionActiva.isPresent()) {
            var asignacion = asignacionActiva.get();
            asignacion.setEstado(com.gas.sistema_gas.Model.AsignacionMoto.EstadoAsignacion.FINALIZADA);
            asignacion.setFechaDevolucion(LocalDateTime.now());
        }

        // 2. Crear nueva asignación con la moto de reemplazo
        com.gas.sistema_gas.Model.AsignacionMoto nuevaAsignacion = new com.gas.sistema_gas.Model.AsignacionMoto();
        nuevaAsignacion.setMoto(motoNueva);
        nuevaAsignacion.setEmpleado(empleadoAfectado);
        nuevaAsignacion.setEstado(com.gas.sistema_gas.Model.AsignacionMoto.EstadoAsignacion.ACTIVA);
        asignacionMotoRepository.save(nuevaAsignacion);

        // 3. Guardar respuesta de la incidencia
        RespuestaIncidencia respuesta = new RespuestaIncidencia();
        respuesta.setIncidencia(incidencia);
        respuesta.setMotoReemplazo(motoReemplazo);
        respuesta.setCreatedAt(LocalDateTime.now());
        respuesta.setUpdatedAt(LocalDateTime.now());
        respuestaIncidenciaRepository.save(respuesta);

        // 4. Marcar incidencia como atendida
        incidencia.setEstado("ATENDIDO");
        incidencia.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidencia);

        // 5. Enviar notificación por WebSocket al repartidor con mensaje generado dinámicamente
        String mensajeRepartidor = generarMensajeRespuesta(incidencia.getTipoIncidencia(), motoReemplazo);
        messagingTemplate.convertAndSend("/topic/repartidor/respuestas/" + empleadoAfectado.getId(), 
            Map.of(
                "mensaje", mensajeRepartidor,
                "fecha", LocalDateTime.now().toString(),
                "tipoIncidencia", incidencia.getTipoIncidencia(),
                "motoReemplazo", motoReemplazo != null ? motoReemplazo : ""
            )
        );

        // 6. Actualizar contador de incidencias pendientes
        long contadorPendientes = incidenciaRepository.countByEstado("PENDIENTE");
        messagingTemplate.convertAndSend("/topic/admin/incidencias", contadorPendientes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Se asignó la unidad " + motoReemplazo + " a " + empleadoAfectado.getNombre() + " y se envió la notificación de auxilio."
        ));
    }

    // Genera el mensaje de respuesta según el tipo de incidencia y la moto de reemplazo
    private String generarMensajeRespuesta(String tipoIncidencia, String motoReemplazo) {
        if (motoReemplazo == null || motoReemplazo.isBlank()) {
            motoReemplazo = "No especificada";
        }
        if ("AVERIA_VEHICULO".equalsIgnoreCase(tipoIncidencia)) {
            return "Se ha registrado un cambio de vehículo. Tu nueva unidad asignada es: " + motoReemplazo + ".";
        } else if ("ACCIDENTE".equalsIgnoreCase(tipoIncidencia)) {
            return "Se ha registrado un reporte de accidente. Unidad de auxilio " + motoReemplazo + " y asistencia médica enviadas de inmediato.";
        } else if ("RECHAZO_POST_LLEGADA".equalsIgnoreCase(tipoIncidencia)) {
            return "El administrador ha revisado y aceptado el rechazo del pedido.";
        }
        return "Notificación de soporte recibida.";
    }

    // GET /api/incidencias/repartidor/{idEmpleado}/respuestas - Repartidor consulta respuestas NO leídas
    @GetMapping("/repartidor/{idEmpleado}/respuestas")
    public ResponseEntity<?> obtenerRespuestasRepartidor(@PathVariable Long idEmpleado, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        // Solo retorna respuestas NO leídas
        List<RespuestaIncidencia> respuestas = respuestaIncidenciaRepository
                .findByIncidenciaEmpleadoIdAndLeidoFalseOrderByCreatedAtDesc(idEmpleado);

        List<Map<String, Object>> resultado = respuestas.stream().map(r -> {
            String tipoIncidencia = r.getIncidencia() != null ? r.getIncidencia().getTipoIncidencia() : "";
            String motoReemplazo = r.getMotoReemplazo() != null ? r.getMotoReemplazo() : "";
            String mensaje = generarMensajeRespuesta(tipoIncidencia, motoReemplazo);
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", r.getId());
            map.put("tipoIncidencia", tipoIncidencia);
            map.put("mensaje", mensaje);
            map.put("motoReemplazo", motoReemplazo);
            map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "respuestas", resultado,
                "total", resultado.size()
        ));
    }

    // POST /api/incidencias/repartidor/{idEmpleado}/respuestas/marcar-leidas - Marcar todas como leídas
    @PostMapping("/repartidor/{idEmpleado}/respuestas/marcar-leidas")
    @Transactional
    public ResponseEntity<?> marcarRespuestasComoLeidas(@PathVariable Long idEmpleado, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        int actualizadas = respuestaIncidenciaRepository.marcarComoLeidasPorEmpleado(idEmpleado);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", actualizadas + " respuesta(s) marcada(s) como leída(s).",
                "actualizadas", actualizadas
        ));
    }

    // GET /api/motos/disponibles - Para el modal del admin (listar motos activas)
    @GetMapping("/motos-disponibles")
    public ResponseEntity<?> obtenerMotosDisponibles(HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var motos = motoRepository.findMotosDisponibles();
        List<Map<String, Object>> resultado = motos.stream().map(m -> 
            Map.<String, Object>of(
                "id", m.getId(),
                "placa", m.getPlaca(),
                "marca", m.getMarca(),
                "modelo", m.getModelo()
            )
        ).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "motos", resultado));
    }

    // GET /api/incidencias/repartidores-disponibles - Listar repartidores con moto activa para reasignación
    @GetMapping("/repartidores-disponibles")
    public ResponseEntity<?> obtenerRepartidoresDisponibles(HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        try {
            // Obtener empleados que tienen una asignación de moto ACTIVA
            var asignacionesActivas = asignacionMotoRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(a -> a.getEstado() == com.gas.sistema_gas.Model.AsignacionMoto.EstadoAsignacion.ACTIVA)
                    .collect(Collectors.toList());

            List<Map<String, Object>> resultado = asignacionesActivas.stream().map(a -> {
                Empleado emp = a.getEmpleado();
                return Map.<String, Object>of(
                    "id", emp.getId(),
                    "nombre", emp.getNombre(),
                    "telefono", emp.getTelefono() != null ? emp.getTelefono() : ""
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "repartidores", resultado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al obtener repartidores: " + e.getMessage()));
        }
    }

    // POST /api/incidencias/{id}/marcar-revisado - Marcar incidencia CLIENTE_AUSENTE como revisada
    @PostMapping("/{id}/marcar-revisado")
    @Transactional
    public ResponseEntity<?> marcarComoRevisado(@PathVariable Long id, HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        // 1. Lectura inicial sin bloqueo de Incidencia para descubrir pedido
        var incidenciaOpt = incidenciaRepository.findById(id);
        if (incidenciaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Incidencia no encontrada"));
        }

        Incidencia incidencia = incidenciaOpt.get();

        // Validar que tenga pedido asociado
        if (incidencia.getPedido() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "La incidencia no tiene un pedido asociado"));
        }

        Long pedidoId = incidencia.getPedido().getId();

        // 2. Pedido bloqueado
        var pedidoOpt = pedidoRepository.findByIdForUpdate(pedidoId);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Pedido no encontrado"));
        }

        Pedido pedido = pedidoOpt.get();

        // 3. Incidencia bloqueada (re-leer con bloqueo)
        var incidenciaBloqueadaOpt = incidenciaRepository.findByIdForUpdate(id);
        if (incidenciaBloqueadaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Incidencia no encontrada"));
        }

        Incidencia incidenciaBloqueada = incidenciaBloqueadaOpt.get();

        // 4. Volver a validar todo
        // - asociación con el mismo Pedido
        if (incidenciaBloqueada.getPedido() == null
                || !incidenciaBloqueada.getPedido().getId().equals(pedidoId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia no está asociada al pedido bloqueado"));
        }
        // - tipo CLIENTE_AUSENTE
        if (!"CLIENTE_AUSENTE".equalsIgnoreCase(incidenciaBloqueada.getTipoIncidencia())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Esta acción solo corresponde a Cliente Ausente"));
        }
        // - Incidencia PENDIENTE
        if ("ATENDIDO".equalsIgnoreCase(incidenciaBloqueada.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia ya fue atendida"));
        }
        if (!"PENDIENTE".equalsIgnoreCase(incidenciaBloqueada.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia no está pendiente"));
        }
        // - Pedido CLIENTE_AUSENTE
        String estadoPedido = pedido.getEstadoPedido() != null ? pedido.getEstadoPedido().trim().toUpperCase() : "";
        if (!"CLIENTE_AUSENTE".equals(estadoPedido)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "El pedido no está en estado CLIENTE_AUSENTE"));
        }

        if (pedidoPagoRepository.existsByPedido_IdAndMontoGreaterThan(pedidoId, BigDecimal.ZERO)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", MENSAJE_PAGO_REQUIERE_REEMBOLSO));
        }

        // 5. devolverStockDePedido
        inventarioLoteService.devolverStockDePedido(pedido.getId());

        // 6. Escritura
        // Marcar incidencia como ATENDIDO
        incidenciaBloqueada.setEstado("ATENDIDO");
        incidenciaBloqueada.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidenciaBloqueada);

        // Guardar respuesta en BD para que aparezca en la bandeja del repartidor
        RespuestaIncidencia respuesta = new RespuestaIncidencia();
        respuesta.setIncidencia(incidenciaBloqueada);
        respuesta.setMotoReemplazo("");
        respuesta.setCreatedAt(LocalDateTime.now());
        respuesta.setUpdatedAt(LocalDateTime.now());
        respuestaIncidenciaRepository.save(respuesta);

        // Enviar notificación WebSocket al repartidor
        if (incidenciaBloqueada.getEmpleado() != null) {
            String codigoPedido = pedido.getCodigo() != null ? pedido.getCodigo() : "N/A";
            String mensajeNotificacion = "El administrador ha revisado y aceptado el rechazo del pedido " + codigoPedido;
            messagingTemplate.convertAndSend("/topic/repartidor/respuestas/" + incidenciaBloqueada.getEmpleado().getId(),
                Map.of(
                    "mensaje", mensajeNotificacion,
                    "fecha", LocalDateTime.now().toString(),
                    "tipoIncidencia", incidenciaBloqueada.getTipoIncidencia(),
                    "motoReemplazo", ""
                )
            );
        }

        // Actualizar contador
        long contadorPendientes = incidenciaRepository.countByEstado("PENDIENTE");
        messagingTemplate.convertAndSend("/topic/admin/incidencias", contadorPendientes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Incidencia marcada como revisada."
        ));
    }

    // POST /api/incidencias/{id}/reasignar-pedido - Reasignar pedido a otro repartidor
    @PostMapping("/{id}/reasignar-pedido")
    @Transactional
    public ResponseEntity<?> reasignarPedido(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body,
                                              HttpSession session) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        // 1. Lectura inicial sin bloqueo de Incidencia
        var incidenciaOpt = incidenciaRepository.findById(id);
        if (incidenciaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Incidencia no encontrada"));
        }

        Incidencia incidencia = incidenciaOpt.get();

        // 2. Validaciones iniciales
        // - tipo CLIENTE_AUSENTE
        String tipoIncidencia = incidencia.getTipoIncidencia();
        if (tipoIncidencia == null || !"CLIENTE_AUSENTE".equalsIgnoreCase(tipoIncidencia)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Solo se pueden reasignar incidencias de Cliente Ausente"));
        }
        // - Incidencia PENDIENTE
        if ("ATENDIDO".equalsIgnoreCase(incidencia.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia ya fue atendida"));
        }
        if (!"PENDIENTE".equalsIgnoreCase(incidencia.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia no está pendiente"));
        }
        // - Pedido asociado
        Pedido pedido = incidencia.getPedido();
        if (pedido == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "La incidencia no tiene un pedido asociado"));
        }

        Long pedidoId = pedido.getId();

        // 3. Pedido bloqueado
        var pedidoOpt = pedidoRepository.findByIdForUpdate(pedidoId);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Pedido no encontrado"));
        }

        pedido = pedidoOpt.get();

        // 4. Incidencia bloqueada (re-leer con bloqueo)
        var incidenciaBloqueadaOpt = incidenciaRepository.findByIdForUpdate(id);
        if (incidenciaBloqueadaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Incidencia no encontrada"));
        }

        Incidencia incidenciaBloqueada = incidenciaBloqueadaOpt.get();

        // 5. Validaciones (re-validar todo después de bloquear)
        // - asociación con el mismo Pedido
        if (incidenciaBloqueada.getPedido() == null
                || !incidenciaBloqueada.getPedido().getId().equals(pedidoId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia no está asociada al pedido bloqueado"));
        }
        // - tipo CLIENTE_AUSENTE
        if (!"CLIENTE_AUSENTE".equalsIgnoreCase(incidenciaBloqueada.getTipoIncidencia())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Solo se pueden reasignar incidencias de Cliente Ausente"));
        }
        // - Incidencia PENDIENTE
        if ("ATENDIDO".equalsIgnoreCase(incidenciaBloqueada.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia ya fue atendida"));
        }
        if (!"PENDIENTE".equalsIgnoreCase(incidenciaBloqueada.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "La incidencia no está pendiente"));
        }
        // - Pedido CLIENTE_AUSENTE
        String estadoPedido = pedido.getEstadoPedido() != null ? pedido.getEstadoPedido().trim().toUpperCase() : "";
        if (!"CLIENTE_AUSENTE".equals(estadoPedido)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "El pedido no está en estado CLIENTE_AUSENTE"));
        }

        // 6. Validar idNuevoRepartidor y existencia del empleado
        Long idNuevoRepartidor = body.get("idNuevoRepartidor") != null
                ? ((Number) body.get("idNuevoRepartidor")).longValue()
                : null;

        if (idNuevoRepartidor == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Debe seleccionar un repartidor"));
        }

        var nuevoRepartidorOpt = empleadoRepository.findById(idNuevoRepartidor);
        if (nuevoRepartidorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Repartidor no encontrado"));
        }

        Empleado nuevoRepartidor = nuevoRepartidorOpt.get();

        // 7. Cargar detalles una vez
        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_Id(pedidoId);

        // 8. Validar detalle, producto, ID y cantidad
        for (DetallePedido detalle : detalles) {
            if (detalle.getProducto() == null || detalle.getProducto().getId() == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "El detalle no tiene producto válido"));
            }
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "La cantidad del detalle no es válida"));
            }
        }

        // 9. Agrupar cantidad total por producto
        Map<Long, BigDecimal> cantidadPorProducto = new java.util.HashMap<>();
        for (DetallePedido detalle : detalles) {
            Long idProducto = detalle.getProducto().getId();
            BigDecimal cantidad = BigDecimal.valueOf(detalle.getCantidad());
            cantidadPorProducto.merge(idProducto, cantidad, BigDecimal::add);
        }

        // 10. devolverStockDePedido exactamente una vez
        inventarioLoteService.devolverStockDePedido(pedido.getId());

        // 11. Productos ordenados bloqueados
        List<Long> idsProductos = cantidadPorProducto.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        List<Producto> productos = productoRepository.findAllByIdInForUpdate(idsProductos);

        // 12. Comprobar que todos los Productos existen
        if (productos.size() != idsProductos.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente para reasignar el pedido");
        }

        // 13. Calcular stockReal - stockReservado y validar todos antes de escribir
        Map<Long, Producto> mapaProductos = productos.stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        for (Map.Entry<Long, BigDecimal> entry : cantidadPorProducto.entrySet()) {
            Long idProducto = entry.getKey();
            BigDecimal cantidadTotal = entry.getValue();

            Producto productoBloqueado = mapaProductos.get(idProducto);
            if (productoBloqueado == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para reasignar el pedido");
            }

            // Calcular stockReal - stockReservado
            BigDecimal stockReal = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
            BigDecimal stockReservado = productoBloqueado.getStockReservado() != null
                    ? productoBloqueado.getStockReservado()
                    : BigDecimal.ZERO;
            BigDecimal stockDisponible = stockReal.subtract(stockReservado);

            // Validar antes de escribir
            if (stockDisponible.compareTo(cantidadTotal) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para reasignar el pedido: " + productoBloqueado.getNombre());
            }
        }

        // 14. Reserva agrupada - reservar una sola vez por producto
        for (Map.Entry<Long, BigDecimal> entry : cantidadPorProducto.entrySet()) {
            Long idProducto = entry.getKey();
            BigDecimal cantidadTotal = entry.getValue();

            Producto productoBloqueado = mapaProductos.get(idProducto);
            BigDecimal stockReservadoActual = productoBloqueado.getStockReservado() != null
                    ? productoBloqueado.getStockReservado()
                    : BigDecimal.ZERO;
            productoBloqueado.setStockReservado(stockReservadoActual.add(cantidadTotal));
        }

        // 15. Guardar cada Producto una sola vez
        productoRepository.saveAll(productos);

        // 16. Luego actualizar Pedido, Incidencia y RespuestaIncidencia
        pedido.setEmpleado(nuevoRepartidor);
        pedido.setEstadoPedido("PENDIENTE");
        pedidoRepository.save(pedido);

        incidenciaBloqueada.setEstado("ATENDIDO");
        incidenciaBloqueada.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidenciaBloqueada);

        RespuestaIncidencia respuesta = new RespuestaIncidencia();
        respuesta.setIncidencia(incidenciaBloqueada);
        respuesta.setMotoReemplazo("Reasignado a: " + nuevoRepartidor.getNombre());
        respuesta.setCreatedAt(LocalDateTime.now());
        respuesta.setUpdatedAt(LocalDateTime.now());
        respuestaIncidenciaRepository.save(respuesta);

        // Notificar al nuevo repartidor por WebSocket
        messagingTemplate.convertAndSend("/topic/pedidos/" + nuevoRepartidor.getId(),
            Map.of(
                "mensaje", "Se te ha asignado un nuevo pedido",
                "idPedido", pedido.getId(),
                "codigo", pedido.getCodigo()
            )
        );

        // Actualizar contador de incidencias pendientes
        long contadorPendientes = incidenciaRepository.countByEstado("PENDIENTE");
        messagingTemplate.convertAndSend("/topic/admin/incidencias", contadorPendientes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pedido " + pedido.getCodigo() + " reasignado a " + nuevoRepartidor.getNombre() + " correctamente."
        ));
    }

    // ========== MÉTODOS PRIVADOS ==========

    private String guardarEvidencia(MultipartFile file, Long idPedido) {
        try {
            // Ruta única y permanente: target/classes/static/imagenes-sistema/
            String uploadDir = "target/classes/static/imagenes-sistema/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = "incidencia_" + idPedido + "_" + UUID.randomUUID().toString() + extension;

            // Guardar solo en la ruta estática
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Retornar la URL pública estandarizada
            return "/imagenes-sistema/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la evidencia: " + e.getMessage());
        }
    }

}
