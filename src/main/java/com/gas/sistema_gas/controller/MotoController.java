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

import com.gas.sistema_gas.dto.MotoDTO;
import com.gas.sistema_gas.service.MotoService;
import com.gas.sistema_gas.service.OpcionService;

@Controller
@RequestMapping("/motos")
public class MotoController {

    @Autowired
    private MotoService motoService;

    @Autowired
    private OpcionService opcionService; // Para mantener el menú dinámico del layout

    // ─── GET: carga la vista HTML de gestión de motos ──────────────────
    @GetMapping
    public String motos(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("motos", motoService.listMoto());
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/moto"); // Nombre de tu archivo HTML dentro de templates/views/
        return "components/layout";
    }

    // ─── GET por ID: retorna JSON para el JS del modal editar ──────────
    @GetMapping("/{id}")
    @ResponseBody
    public MotoDTO.SimpleResponse getById(@PathVariable Long id) {
        return motoService.findById(id);
    }

    // ─── GET TABLA: fragmento Thymeleaf para refrescar la grilla ───────
    @GetMapping("/tabla")
    public String tablaMotos(Model model) {
        model.addAttribute("motos", motoService.listMoto());
        return "views/moto :: tablaMotos"; // Cambia al fragmento de tu tabla
    }

    // ─── POST: crear nueva moto mediante AJAX ──────────────────────────
    @PostMapping
    @ResponseBody
    public Map<String, Object> crear(
            @ModelAttribute @Valid MotoDTO.Create create,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        motoService.createMoto(create);
        return Map.of("status", "OK");
    }

    // ─── POST: editar moto (simula PUT desde formulario HTML) ──────────
    @PostMapping("/{id}/editar")
    @ResponseBody
    public Map<String, Object> editar(
            @PathVariable Long id,
            @ModelAttribute @Valid MotoDTO.Update update,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        motoService.updateMoto(id, update);
        return Map.of("status", "OK");
    }

    // ─── POST: eliminar moto (eliminación lógica) ──────────────────────
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminar(@PathVariable Long id) {
        motoService.deleteMoto(id);
        return Map.of("status", "OK");
    }

    // ─── MANEJO DE EXCEPCIONES LOCALES ─────────────────────────────────
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
            message = "Datos de la moto inválidos";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "ERROR", "message", message));
    }
}
