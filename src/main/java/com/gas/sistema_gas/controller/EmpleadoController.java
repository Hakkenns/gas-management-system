package com.gas.sistema_gas.controller;

import java.util.Map;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.dto.EmpleadoDTO;
import com.gas.sistema_gas.service.EmpleadoService;
import com.gas.sistema_gas.service.OpcionService;

@Controller
@RequestMapping("/empleados")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private OpcionService opcionService; // Para mantener el menú dinámico del layout

    // GET: carga la vista HTML de gestión de empleados
    @GetMapping
    public String empleados(Model model, jakarta.servlet.http.HttpSession session) {
        cargarModeloEmpleados(model, session);
        model.addAttribute("contenido", "views/empleado");
        return "components/layout";
    }

    @GetMapping("/fragment")
    public String fragmentoEmpleados(Model model, jakarta.servlet.http.HttpSession session) {
        cargarModeloEmpleados(model, session);
        return "views/empleado :: content";
    }

    private void cargarModeloEmpleados(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("empleados", empleadoService.listAll());
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
    }

    // GET por ID: retorna JSON para el JS del modal editar
    @GetMapping("/{id}")
    @ResponseBody
    public EmpleadoDTO.SimpleResponse getById(@PathVariable Long id) {
        return empleadoService.findById(id);
    }

    // GET TABLA: fragmento Thymeleaf para refrescar la grilla
    @GetMapping("/tabla")
    public String tablaEmpleados(Model model) {
        model.addAttribute("empleados", empleadoService.listAll());
        return "views/empleado :: tablaEmpleados"; // Fragmento th:fragment="tablaEmpleados"
    }

    // POST: crear nuevo empleado mediante AJAX
    @PostMapping
    @ResponseBody
    public Map<String, Object> crear(
            @ModelAttribute @Valid EmpleadoDTO.Create create,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        empleadoService.createEmployee(create);
        return Map.of("status", "OK");
    }

    // POST: editar empleado (simula PUT desde formulario HTML)
    @PostMapping("/{id}/editar")
    @ResponseBody
    public Map<String, Object> editar(
            @PathVariable Long id,
            @ModelAttribute @Valid EmpleadoDTO.Update update,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        empleadoService.updateEmployee(id, update);
        return Map.of("status", "OK");
    }

    // POST: eliminar empleado (eliminación lógica)
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminar(@PathVariable Long id) {
        empleadoService.deleteEmployee(id);
        return Map.of("status", "OK");
    }

    //  MANEJO DE EXCEPCIONES LOCALES
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
            message = "Datos del empleado inválidos";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "ERROR", "message", message));
    }

    @GetMapping("/api/consultar-dni/{dni}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consultarDniExterna(@PathVariable String  dni) {
        String url = "https://miapi.cloud/v1/dni/" + dni;
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjo2MDYsImV4cCI6MTc2NDk1MjQyMH0.vY1nXLT70L1E73d0A1cTspzZoLwppLNaF-pIbqu8EpI";

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Ejecuta la petición GET hacia el proveedor externo
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", (Object) false, "message", "Error al conectar con el servicio de identificación."));
        }
    }

}
