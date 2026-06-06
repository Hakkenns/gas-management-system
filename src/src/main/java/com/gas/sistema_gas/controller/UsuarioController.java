package com.gas.sistema_gas.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.dto.UsuarioDTO;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PerfilService;
import com.gas.sistema_gas.service.UsuarioService;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private PerfilService perfilService;
    @Autowired
    private OpcionService opcionService;

    // ─── GET: carga la vista ───────────────────────────────────────────
    @GetMapping
    public String usuarios(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("usuarios", usuarioService.listAll());
        model.addAttribute("perfiles", perfilService.listActive());
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/usuario");
        return "components/layout";
    }

    // ─── GET por ID: retorna JSON para el JS del modal editar ──────────
    @GetMapping("/{id}")
    @ResponseBody
    public UsuarioDTO.SimpleResponse getById(@PathVariable Long id) {
        return usuarioService.findById(id);
    }

    @GetMapping("/tabla")
    public String tablaUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listAll());
        return "views/usuario :: tablaUsuarios";
    }

    // ─── POST: crear nuevo usuario ─────────────────────────────────────
    @PostMapping
    @ResponseBody
    public Map<String, Object> crear(
            @ModelAttribute @Valid UsuarioDTO.Create create,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        usuarioService.createUser(create);
        return Map.of("status", "OK");
    }

    // ─── POST: editar usuario (simula PUT desde HTML form) ─────────────
    @PostMapping("/{id}/editar")
    @ResponseBody
    public Map<String, Object> editar(
            @PathVariable Long id,
            @ModelAttribute @Valid UsuarioDTO.Update update,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        usuarioService.updateUser(id, update);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/estado")
    @ResponseBody
    public Map<String, Object> cambiarEstado(@PathVariable Long id,
            @RequestParam Integer estado) {
        usuarioService.setState(id, estado);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminar(@PathVariable Long id) {
        usuarioService.deleteUser(id);
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
            message = "Datos de usuario inválidos";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "ERROR", "message", message));
    }
}
