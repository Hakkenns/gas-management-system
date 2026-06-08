package com.gas.sistema_gas.controller;

import java.util.Map;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.dto.AsignacionMotoDTO;
import com.gas.sistema_gas.service.AsignacionMotoService;
import com.gas.sistema_gas.service.EmpleadoService;
import com.gas.sistema_gas.service.MotoService;
import com.gas.sistema_gas.service.OpcionService;

@Controller
@RequestMapping("/asignacion_motos") // Escuchando con guion bajo
public class AsignacionMotoController {

    @Autowired
    private AsignacionMotoService asignacionMotoService;

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private MotoService motoService;

    @Autowired
    private OpcionService opcionService;

    @GetMapping
    public String index(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");

        model.addAttribute("asignaciones", asignacionMotoService.listAll());

        // CRÍTICO: Asegúrate de usar .listDisponibles() aquí para la carga inicial de la página
        model.addAttribute("empleados", empleadoService.listDisponibles());
        model.addAttribute("motos", motoService.listDisponibles());

        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/asignacion_moto");
        return "components/layout";
    }

    @GetMapping("/tabla")
    public String tablaAsignaciones(Model model) {
        model.addAttribute("asignaciones", asignacionMotoService.listAll());
        return "views/asignacion_moto :: tablaAsignaciones";
    }

    // Recibe directamente el POST de la raíz (/asignacion_motos)
    @PostMapping
    @ResponseBody
    public Map<String, Object> crear(
            @ModelAttribute @Valid AsignacionMotoDTO.Create create,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        asignacionMotoService.asignar(create);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/finalizar")
    @ResponseBody
    public Map<String, Object> finalizar(@PathVariable Long id) {
        asignacionMotoService.finalizar(id);
        return Map.of("status", "OK");
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("status", "ERROR", "message", ex.getReason()));
    }

    @ExceptionHandler({IllegalArgumentException.class, org.springframework.validation.BindException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Datos de asignación inválidos o conflicto de negocio";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "ERROR", "message", message));
    }
}