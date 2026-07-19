package com.gas.sistema_gas.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gas.sistema_gas.Model.Evidencia;
import com.gas.sistema_gas.Repository.EvidenciaRepository;

@RestController
@RequestMapping("/api/evidencias")
public class EvidenciaController {

    @Autowired
    private EvidenciaRepository evidenciaRepository;

    @GetMapping("/{idPedido}")
    public ResponseEntity<Map<String, Object>> obtenerEvidenciasPorPedido(@PathVariable Long idPedido) {
        try {
            List<Evidencia> evidencias = evidenciaRepository.findByPedidoPago_Id(idPedido);

            List<Map<String, Object>> pago = evidencias.stream()
                    .filter(e -> "PAGO".equalsIgnoreCase(e.getTipoEvidencia()))
                    .map(this::toResponse)
                    .toList();

            List<Map<String, Object>> vuelto = evidencias.stream()
                    .filter(e -> "VUELTO".equalsIgnoreCase(e.getTipoEvidencia()))
                    .map(this::toResponse)
                    .toList();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("pedidoId", idPedido);
            response.put("pago", pago);
            response.put("vuelto", vuelto);

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("pedidoId", idPedido);
            response.put("pago", List.of());
            response.put("vuelto", List.of());
            response.put("error", "No se pudieron cargar las evidencias");
            return ResponseEntity.ok(response);
        }
    }

    private Map<String, Object> toResponse(Evidencia evidencia) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", evidencia.getId());
        item.put("tipoEvidencia", evidencia.getTipoEvidencia());
        item.put("urlImagen", evidencia.getUrlImagen());
        item.put("numOperacion", evidencia.getPedidoPago() != null ? evidencia.getPedidoPago().getNumOperacion() : null);
        item.put("metodoPago", evidencia.getPedidoPago() != null && evidencia.getPedidoPago().getMetodoPago() != null
                ? evidencia.getPedidoPago().getMetodoPago().getNombre()
                : null);
        return item;
    }
}
