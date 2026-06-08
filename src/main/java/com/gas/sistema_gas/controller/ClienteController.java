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

import com.gas.sistema_gas.dto.ClienteDTO;
import com.gas.sistema_gas.service.ClienteService;
import com.gas.sistema_gas.service.OpcionService;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private OpcionService opcionService; // Mantiene el menú dinámico del layout

    // GET: carga la vista HTML de gestión de clientes incorporada en el Layout general
    @GetMapping
    public String clientes(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("clientes", clienteService.listAll());
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/cliente"); // Buscará: templates/views/cliente.html
        return "components/layout";
    }

    // GET por ID: retorna JSON para poblar los campos del modal de edición en Javascript
    @GetMapping("/{id}")
    @ResponseBody
    public ClienteDTO.SimpleResponse getById(@PathVariable Long id) {
        return clienteService.findById(id);
    }

    // GET TABLA: fragmento exclusivo de Thymeleaf para refrescar asíncronamente la grilla por AJAX
    @GetMapping("/tabla")
    public String tablaClientes(Model model) {
        model.addAttribute("clientes", clienteService.listAll());
        return "views/cliente :: tablaClientes"; // th:fragment="tablaClientes" en tu HTML
    }

    // POST: crea un nuevo cliente mediante peticiones AJAX
    @PostMapping
    @ResponseBody
    public Map<String, Object> crear(
            @ModelAttribute @Valid ClienteDTO.Create create,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        clienteService.createClient(create);
        return Map.of("status", "OK");
    }

    // POST: edita un cliente existente (Simula el método PUT desde el formulario HTML)
    @PostMapping("/{id}/editar")
    @ResponseBody
    public Map<String, Object> editar(
            @PathVariable Long id,
            @ModelAttribute @Valid ClienteDTO.Update update,
            BindingResult result) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(message);
        }

        clienteService.updateClient(id, update);
        return Map.of("status", "OK");
    }

    // POST: deshabilita un cliente (eliminación lógica cambiando el estado)
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminar(@PathVariable Long id) {
        clienteService.deleteClient(id);
        return Map.of("status", "OK");
    }

    // API EXTERNA: Consulta de DNI en tiempo real para autocompletar nombres de clientes
    @GetMapping("/api/consultar-dni/{dni}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consultarDniExterna(@PathVariable String dni) {
        String url = "https://miapi.cloud/v1/dni/" + dni;
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjo2MDYsImV4cCI6MTc2NDk1MjQyMH0.vY1nXLT70L1E73d0A1cTspzZoLwppLNaF-pIbqu8EpI";

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Consumo asíncrono del servicio en la nube
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", (Object) false, "message", "Error al conectar con el servicio de identificación de clientes."));
        }
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
            message = "Datos del cliente inválidos o mal estructurados";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "ERROR", "message", message));
    }
}
