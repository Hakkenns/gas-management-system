package com.gas.sistema_gas.controller;

import com.gas.sistema_gas.service.CorrelativoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CorrelativoController {

    private final CorrelativoService correlativoService;

    public CorrelativoController(CorrelativoService correlativoService) {
        this.correlativoService = correlativoService;
    }

    @GetMapping("/api/correlativos/next")
    public ResponseEntity<?> preview(@RequestParam String tipo, @RequestParam String serie) {
        try {
            String codigo = correlativoService.previsualizarCodigoSiguiente(tipo, serie);
            return ResponseEntity.ok().body(java.util.Map.of("codigo", codigo));
        } catch (Exception ex) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/api/correlativos/next")
    public ResponseEntity<?> confirm(@RequestParam String tipo, @RequestParam String serie) {
        try {
            String codigo = correlativoService.incrementarYObtenerCodigo(tipo, serie);
            return ResponseEntity.ok().body(java.util.Map.of("codigo", codigo));
        } catch (Exception ex) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", ex.getMessage()));
        }
    }
}
