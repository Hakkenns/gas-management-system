package com.gas.sistema_gas.controller;

import java.util.Map;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.dto.CajaDTO;
import com.gas.sistema_gas.service.CajaService;
import com.gas.sistema_gas.service.OpcionService;

@Controller
public class CajaController {

    private static final String RUTA_CAJA = "caja";

    private final CajaService cajaService;
    private final OpcionService opcionService;

    public CajaController(CajaService cajaService, OpcionService opcionService) {
        this.cajaService = cajaService;
        this.opcionService = opcionService;
    }

    @GetMapping("/caja")
    public String caja(Model model, HttpSession session) {
        if (!sesionAutenticada(session)) {
            return "redirect:/login";
        }
        if (!tienePermisoCaja(session)) {
            return "redirect:/";
        }

        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/caja");
        return "components/layout";
    }

    @GetMapping("/api/caja/estado")
    @ResponseBody
    public ResponseEntity<?> obtenerEstado(HttpSession session) {
        ResponseEntity<?> rechazo = rechazarApiSiNoAutorizado(session);
        if (rechazo != null) {
            return rechazo;
        }
        return ResponseEntity.ok(cajaService.obtenerEstadoCaja());
    }

    @GetMapping("/api/caja/custodias")
    @ResponseBody
    public ResponseEntity<?> listarCustodias(HttpSession session) {
        ResponseEntity<?> rechazo = rechazarApiSiNoAutorizado(session);
        if (rechazo != null) {
            return rechazo;
        }
        return ResponseEntity.ok(cajaService.listarCustodiasPendientes());
    }

    @PostMapping("/api/caja/custodias/liquidar")
    @ResponseBody
    public ResponseEntity<?> liquidarCustodia(
            @Valid @RequestBody CajaDTO.LiquidacionRequest request,
            BindingResult bindingResult,
            HttpSession session) {
        ResponseEntity<?> rechazo = rechazarApiSiNoAutorizado(session);
        if (rechazo != null) {
            return rechazo;
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        try {
            return ResponseEntity.ok(cajaService.liquidarCustodiaMotorizado(request, usuarioId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "message", e.getReason() != null ? e.getReason() : "Error de negocio"));
        }
    }

    private ResponseEntity<?> rechazarApiSiNoAutorizado(HttpSession session) {
        if (!sesionAutenticada(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado"));
        }
        if (!tienePermisoCaja(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acceso denegado"));
        }
        return null;
    }

    private boolean sesionAutenticada(HttpSession session) {
        return session != null
                && session.getAttribute("usuarioLogueado") != null
                && session.getAttribute("usuarioId") instanceof Long;
    }

    private boolean tienePermisoCaja(HttpSession session) {
        Long perfilId = session.getAttribute("usuarioPerfilId") instanceof Long
                ? (Long) session.getAttribute("usuarioPerfilId")
                : null;
        return opcionService.tieneAccesoRuta(perfilId, RUTA_CAJA);
    }
}
