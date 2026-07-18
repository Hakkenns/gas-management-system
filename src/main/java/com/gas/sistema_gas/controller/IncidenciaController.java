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
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public IncidenciaController(IncidenciaRepository incidenciaRepository,
                                RespuestaIncidenciaRepository respuestaIncidenciaRepository,
                                EmpleadoRepository empleadoRepository,
                                PedidoRepository pedidoRepository,
                                MotoRepository motoRepository,
                                UsuarioRepository usuarioRepository,
                                AsignacionMotoRepository asignacionMotoRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.incidenciaRepository = incidenciaRepository;
        this.respuestaIncidenciaRepository = respuestaIncidenciaRepository;
        this.empleadoRepository = empleadoRepository;
        this.pedidoRepository = pedidoRepository;
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionMotoRepository = asignacionMotoRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // POST /api/incidencias - Repartidor registra una incidencia
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
        Long idPedido = body.get("idPedido") != null ? ((Number) body.get("idPedido")).longValue() : null;

        if (tipoIncidencia == null || tipoIncidencia.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "El tipo de incidencia es obligatorio"));
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setEmpleado(empleado);
        
        // Determinar el tipo general (CRITICA o INCIDENCIA)
        String tipoGeneral = "INCIDENCIA";
        String tipoLabel = "Incidencia";
        if ("AVERIA_VEHICULO".equalsIgnoreCase(tipoIncidencia) || "ACCIDENTE".equalsIgnoreCase(tipoIncidencia)) {
            tipoGeneral = "CRITICA";
            tipoLabel = "Alerta Crítica";
        }
        incidencia.setTipo(tipoGeneral);
        incidencia.setTipoIncidencia(tipoIncidencia.toUpperCase());
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

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", inc.getId());
                map.put("tipoIncidencia", inc.getTipoIncidencia());
                map.put("tipoLabel", inc.getTipoLabel() != null ? inc.getTipoLabel() : "");
                map.put("estado", inc.getEstado());
                map.put("empleadoNombre", nombreEmpleado);
                map.put("empleadoTelefono", telefonoEmpleado);
                map.put("idPedido", idPedido);
                map.put("codigoPedido", codigoPedido);
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
}