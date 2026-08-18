package com.gas.sistema_gas.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.gas.sistema_gas.dto.EnvioEnvaseDTO;
import com.gas.sistema_gas.Model.Envase;
import com.gas.sistema_gas.service.EnvaseService;
import com.gas.sistema_gas.service.OpcionService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/envases")
public class EnvaseController {

    @Autowired
    private EnvaseService envaseService;

    @Autowired
    private OpcionService opcionService;

    @GetMapping
    public String envases(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/envases");
        return "components/layout";
    }

    @GetMapping("/vista")
    public String vistaEnvases(Model model, HttpSession session) {
        cargarModeloCatalogoEnvases(model, session);
        model.addAttribute("contenido", "views/envases-maestro");
        return "components/layout";
    }

    @GetMapping("/vista/fragment")
    public String fragmentoVistaEnvases(Model model, HttpSession session) {
        cargarModeloCatalogoEnvases(model, session);
        return "views/envases-maestro :: content";
    }

    @GetMapping("/maestro")
    public String maestroEnvases(Model model, HttpSession session) {
        return catalogoEnvases(model, session);
    }

    @GetMapping("/catalogo")
    public String catalogoEnvases(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/envases-maestro");
        return "components/layout";
    }

    @GetMapping("/deudores")
    public String deudoresEnvases(Model model, HttpSession session) {
        return envases(model, session);
    }

    @GetMapping("/api/deudores")
    @ResponseBody
    public ResponseEntity<List<EnvioEnvaseDTO.DeudorResponse>> listarDeudores() {
        return ResponseEntity.ok(envaseService.listarDeudoresPendientes());
    }

    @PostMapping("/api/devolucion")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarDevolucion(
            @RequestBody EnvioEnvaseDTO.DevolucionRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            envaseService.registrarDevolucion(request);
            response.put("status", "OK");
            response.put("message", "Devolución registrada correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<Envase>> listarTodos() {
        return ResponseEntity.ok(envaseService.listarTodos());
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Envase> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(envaseService.obtenerPorId(id));
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<Envase> crear(@RequestBody Envase envase) {
        return ResponseEntity.ok(envaseService.guardar(envase));
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Envase> actualizar(@PathVariable Long id, @RequestBody Envase envase) {
        envaseService.obtenerPorId(id);
        envase.setId(id);
        return ResponseEntity.ok(envaseService.guardar(envase));
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        envaseService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private void cargarModeloCatalogoEnvases(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
    }
}
