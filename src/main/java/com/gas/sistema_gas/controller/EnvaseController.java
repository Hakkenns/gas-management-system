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

import com.gas.sistema_gas.dto.EnvioEnvaseDTO;
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
}