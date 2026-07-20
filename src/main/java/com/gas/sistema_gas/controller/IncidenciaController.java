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
import com.gas.sistema_gas.Repository.RespuestaIncidenciaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Producto;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    private final IncidenciaRepository incidenciaRepository;
    private final RespuestaIncidenciaRepository respuestaIncidenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PedidoRepository pedidoRepository;
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionMotoRepository asignacionMotoRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DetallePedidoRepository detallePedidoRepository;
    private final InventarioLoteRepository inventarioLoteRepository;
    private final ProductoRepository productoRepository;

    public IncidenciaController(IncidenciaRepository incidenciaRepository,
                                RespuestaIncidenciaRepository respuestaIncidenciaRepository,
                                EmpleadoRepository empleadoRepository,
                                PedidoRepository pedidoRepository,
                                MotoRepository motoRepository,
                                UsuarioRepository usuarioRepository,
                                AsignacionMotoRepository asignacionMotoRepository,
                                SimpMessagingTemplate messagingTemplate,
                                DetallePedidoRepository detallePedidoRepository,
                                InventarioLoteRepository inventarioLoteRepository,
                                ProductoRepository productoRepository) {
        this.incidenciaRepository = incidenciaRepository;
        this.respuestaIncidenciaRepository = respuestaIncidenciaRepository;
        this.empleadoRepository = empleadoRepository;
        this.pedidoRepository = pedidoRepository;
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionMotoRepository = asignacionMotoRepository;
        this.messagingTemplate = messagingTemplate;
        this.detallePedidoRepository = detallePedidoRepository;
        this.inventarioLoteRepository = inventarioLoteRepository;
        this.productoRepository = productoRepository;
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
        var pedidoOpt = pedidoRepository.findById(idPedido);
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
        Map<String, Object> notificacion = Map.of(
                "tipoIncidencia", tipoUpper,
                "idPedido", pedido.getId(),
                "codigoPedido", codigoPedido,
                "nombreMotorizado", nombreMotorizado,
                "nombreCliente", nombreCliente,
                "celularCliente", celularCliente,
                "urlEvidencia", urlEvidencia,
                "idIncidencia", incidencia.getId(),
                "createdAt", incidencia.getCreatedAt() != null ? incidencia.getCreatedAt().toString() : LocalDateTime.now().toString()
        );

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

    // POST /api/incidencias/confirmar-rechazo - Admin confirma rechazo (ya se ejecutó en reportar, esto es solo para trigger adicional)
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

        var pedidoOpt = pedidoRepository.findById(idPedido);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Pedido pedido = pedidoOpt.get();
        Empleado empleadoAnterior = pedido.getEmpleado(); // Guardar antes de desasignar
        String codigoPedido = pedido.getCodigo();
        
        pedido.setEstadoPedido("RECHAZADO");
        pedido.setEstadoPago("CANCELADO");
        pedido.setEmpleado(null);
        pedidoRepository.save(pedido);

        // Retornar stock
        retornarStockPedido(pedido);

        // Marcar todas las incidencias de este pedido como CONFIRMADO
        List<Incidencia> incidencias = incidenciaRepository.findByPedidoId(idPedido);
        for (Incidencia inc : incidencias) {
            inc.setEstado("CONFIRMADO");
            inc.setUpdatedAt(LocalDateTime.now());
            incidenciaRepository.save(inc);
        }

        // Actualizar contador
        long contadorPendientes = incidenciaRepository.countByEstado("PENDIENTE");
        messagingTemplate.convertAndSend("/topic/admin/incidencias", contadorPendientes);

        // Notificar al motorizado en tiempo real que su rechazo fue aceptado
        if (empleadoAnterior != null) {
            String mensajeNotificacion = "El administrador ha revisado y aceptado el rechazo del pedido " + (codigoPedido != null ? codigoPedido : "N/A");
            
            // Guardar respuesta en BD para que aparezca en la bandeja de soporte del repartidor
            RespuestaIncidencia respuesta = new RespuestaIncidencia();
            respuesta.setMotoReemplazo("");
            respuesta.setCreatedAt(LocalDateTime.now());
            respuesta.setUpdatedAt(LocalDateTime.now());
            // Asociar a la primera incidencia del pedido
            if (!incidencias.isEmpty()) {
                respuesta.setIncidencia(incidencias.get(0));
            }
            respuestaIncidenciaRepository.save(respuesta);
            
            messagingTemplate.convertAndSend("/topic/repartidor/respuestas/" + empleadoAnterior.getId(),
                Map.of(
                    "mensaje", mensajeNotificacion,
                    "fecha", LocalDateTime.now().toString(),
                    "tipoIncidencia", "RECHAZO_POST_LLEGADA",
                    "motoReemplazo", ""
                )
            );
            // También enviar al canal de pedidos para que desaparezca de su lista
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

        var incidenciaOpt = incidenciaRepository.findById(id);
        if (incidenciaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Incidencia no encontrada"));
        }

        Incidencia incidencia = incidenciaOpt.get();
        incidencia.setEstado("ATENDIDO");
        incidencia.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidencia);

        // CLIENTE_AUSENTE: retornar stock automáticamente al confirmar
        if (incidencia.getPedido() != null && "CLIENTE_AUSENTE".equalsIgnoreCase(incidencia.getTipoIncidencia())) {
            Pedido pedido = incidencia.getPedido();
            // Liberar stock_reservado
            retornarStockPedido(pedido);
        }

        // Guardar respuesta en BD para que aparezca en la bandeja del repartidor
        RespuestaIncidencia respuesta = new RespuestaIncidencia();
        respuesta.setIncidencia(incidencia);
        respuesta.setMotoReemplazo("");
        respuesta.setCreatedAt(LocalDateTime.now());
        respuesta.setUpdatedAt(LocalDateTime.now());
        respuestaIncidenciaRepository.save(respuesta);

        // Enviar notificación WebSocket al repartidor
        if (incidencia.getEmpleado() != null) {
            String codigoPedido = incidencia.getPedido() != null ? incidencia.getPedido().getCodigo() : "N/A";
            String mensajeNotificacion = "El administrador ha revisado y aceptado el rechazo del pedido " + codigoPedido;
            messagingTemplate.convertAndSend("/topic/repartidor/respuestas/" + incidencia.getEmpleado().getId(),
                Map.of(
                    "mensaje", mensajeNotificacion,
                    "fecha", LocalDateTime.now().toString(),
                    "tipoIncidencia", incidencia.getTipoIncidencia(),
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

        var incidenciaOpt = incidenciaRepository.findById(id);
        if (incidenciaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Incidencia no encontrada"));
        }

        Incidencia incidencia = incidenciaOpt.get();
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
        Pedido pedido = incidencia.getPedido();

        if (pedido == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "La incidencia no tiene un pedido asociado"));
        }

        // Reasignar el pedido al nuevo repartidor
        pedido.setEmpleado(nuevoRepartidor);
        pedido.setEstadoPedido("PENDIENTE");
        pedidoRepository.save(pedido);

        // Marcar incidencia como atendida
        incidencia.setEstado("ATENDIDO");
        incidencia.setUpdatedAt(LocalDateTime.now());
        incidenciaRepository.save(incidencia);

        // Guardar respuesta de la incidencia
        RespuestaIncidencia respuesta = new RespuestaIncidencia();
        respuesta.setIncidencia(incidencia);
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

    private void retornarStockPedido(Pedido pedido) {
        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_Id(pedido.getId());
        for (DetallePedido detalle : detalles) {
            Producto producto = detalle.getProducto();
            BigDecimal cantidadADevolver = BigDecimal.valueOf(detalle.getCantidad());

            // Devolver stock al último lote de este producto
            List<com.gas.sistema_gas.Model.InventarioLote> lotes = inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(producto.getId());
            if (!lotes.isEmpty()) {
                com.gas.sistema_gas.Model.InventarioLote ultimoLote = lotes.get(0);
                ultimoLote.setCantidadActual(ultimoLote.getCantidadActual().add(cantidadADevolver));
                inventarioLoteRepository.save(ultimoLote);
            }

            // Sincronizar el campo estático stock_llenos
            BigDecimal stockDisponible = inventarioLoteRepository.findByProductoIdOrderByCreatedAtDesc(producto.getId())
                    .stream()
                    .map(l -> l.getCantidadActual() != null ? l.getCantidadActual() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            producto.setStockLlenos(stockDisponible);
            productoRepository.save(producto);
        }
    }
}