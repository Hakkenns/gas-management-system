package com.gas.sistema_gas.controller;

import com.gas.sistema_gas.Repository.IncidenciaRepository;
import com.gas.sistema_gas.Repository.RespuestaIncidenciaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

    private final IncidenciaRepository incidenciaRepository;
    private final RespuestaIncidenciaRepository respuestaIncidenciaRepository;
    private final UsuarioRepository usuarioRepository;

    public GlobalModelAttributeAdvice(IncidenciaRepository incidenciaRepository,
                                     RespuestaIncidenciaRepository respuestaIncidenciaRepository,
                                     UsuarioRepository usuarioRepository) {
        this.incidenciaRepository = incidenciaRepository;
        this.respuestaIncidenciaRepository = respuestaIncidenciaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("contadorIncidencias")
    public long contadorIncidenciasPendientes(HttpSession session) {
        try {
            if (session != null && session.getAttribute("usuarioLogueado") != null) {
                Long usuarioId = (Long) session.getAttribute("usuarioId");
                if (usuarioId != null) {
                    var usuarioOpt = usuarioRepository.findById(usuarioId);
                    if (usuarioOpt.isPresent() && usuarioOpt.get().getEmpleado() != null) {
                        var empleado = usuarioOpt.get().getEmpleado();
                        // Para repartidores: contar respuesttas de incidencia no leídas
                        return respuestaIncidenciaRepository.countByIncidenciaEmpleadoId(empleado.getId());
                    }
                }
                
                // Para administradores: contar incidencias pendientes
                String perfilNombre = (String) session.getAttribute("usuarioPerfilNombre");
                if (perfilNombre != null && 
                    (perfilNombre.equalsIgnoreCase("ADMINISTRADOR") || 
                     perfilNombre.equalsIgnoreCase("ADMIN"))) {
                    return incidenciaRepository.countByEstado("PENDIENTE");
                }
            }
        } catch (Exception e) {
            // Si hay error, retornar 0 para no mostrar el badge
        }
        return 0;
    }
}
