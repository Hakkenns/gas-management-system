package com.gas.sistema_gas.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.dto.OpcionDTO;
import com.gas.sistema_gas.dto.PerfilDTO;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PerfilService;

@Controller
@RequestMapping("/perfiles")
public class PerfilController {

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private OpcionService opcionService;

    @GetMapping
    public String perfiles(Model model) {
        model.addAttribute("menu", opcionService.listAll());
        model.addAttribute("perfiles", perfilService.listNotDeleted());
        model.addAttribute("contenido", "views/perfiles");
        return "components/layout";
    }

    @GetMapping("/tabla")
    public String tablaPerfiles(Model model) {
        model.addAttribute("perfiles", perfilService.listNotDeleted());
        return "views/perfiles :: tablaPerfiles";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public PerfilDTO.Response getById(@PathVariable Long id) {
        return perfilService.findById(id);
    }

    @GetMapping("/{id}/permisos")
    @ResponseBody
    public Map<String, Object> getPermisos(@PathVariable Long id) {
        PerfilDTO.Response perfil = perfilService.findById(id);
        List<OpcionDTO.SimpleResponse> opciones = opcionService.listAll();
        List<Long> permisosActuales = perfil.opciones() == null
                ? List.of()
                : perfil.opciones().stream().map(OpcionDTO.SimpleResponse::id).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("idPerfil", perfil.id());
        response.put("nombre", perfil.nombrePerfil());
        response.put("opciones", opciones);
        response.put("permisosActuales", permisosActuales);
        return response;
    }

    @PostMapping
    @ResponseBody
    public Map<String, Object> crear(@Valid PerfilDTO.Create create,
            BindingResult result) {

        if (result.hasErrors()) {
            throw new IllegalArgumentException("Datos de perfil inválidos");
        }

        perfilService.create(create);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/editar")
    @ResponseBody
    public Map<String, Object> editar(@PathVariable Long id,
            @Valid PerfilDTO.Create update,
            BindingResult result) {

        if (result.hasErrors()) {
            throw new IllegalArgumentException("Datos de perfil inválidos");
        }

        perfilService.update(id, update);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/estado")
    @ResponseBody
    public Map<String, Object> cambiarEstado(@PathVariable Long id,
            @RequestParam Integer estado) {
        perfilService.setState(id, estado);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminar(@PathVariable Long id) {
        perfilService.delete(id);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/permisos")
    @ResponseBody
    public Map<String, Object> guardarPermisos(@PathVariable Long id,
            @RequestParam(required = false, name = "idOpciones") List<Long> idOpciones) {
        perfilService.assignOptions(id, idOpciones);
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
            message = "Datos de perfil inválidos";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "ERROR", "message", message));
    }
}
