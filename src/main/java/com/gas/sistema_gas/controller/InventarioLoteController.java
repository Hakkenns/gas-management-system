package com.gas.sistema_gas.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.gas.sistema_gas.dto.InventarioLoteDTO;
import com.gas.sistema_gas.service.InventarioLoteService;

@Controller
@RequestMapping("/inventario-lotes")
@RequiredArgsConstructor // Inyección por constructor automática y limpia de advertencias
public class InventarioLoteController {

    private final InventarioLoteService inventarioLoteService;

    // 🟢 ENTRADA DE DATOS DESDE COMPRAS: Registrar un nuevo lote al guardar la factura
    @PostMapping("/registrar")
    @ResponseBody
    public Map<String, Object> registrarLote(@Valid InventarioLoteDTO.Create createDto, BindingResult result) {
        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }

        try {
            inventarioLoteService.registrarLote(createDto);
            return Map.of("status", "OK", "message", "Lote de inventario registrado con éxito.");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return Map.of("status", "ERROR", "message", e.getReason());
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo registrar el lote de inventario.");
        }
    }

    // 🟢 EDICIÓN EN CALIENTE POR COMPETENCIA: Actualizar el precio de venta de un lote específico
    @PostMapping("/{id}/actualizar-precio")
    @ResponseBody
    public Map<String, Object> actualizarPrecioVenta(@PathVariable Long id, @Valid InventarioLoteDTO.Update updateDto, BindingResult result) {
        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }

        try {
            inventarioLoteService.actualizarPrecioVenta(id, updateDto);
            return Map.of("status", "OK", "message", "Precio de venta del lote actualizado correctamente.");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return Map.of("status", "ERROR", "message", e.getReason());
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo actualizar el precio del lote.");
        }
    }

    // 🔍 ALIMENTACIÓN DEL NUEVO BOTÓN: Trae el desglose de todos los lotes de un producto en formato JSON
    @GetMapping("/producto/{idProducto}")
    @ResponseBody
    public ResponseEntity<List<InventarioLoteDTO.SimpleResponse>> listarLotesPorProducto(@PathVariable Long idProducto) {
        List<InventarioLoteDTO.SimpleResponse> lotes = inventarioLoteService.listarLotesPorProducto(idProducto);
        return ResponseEntity.ok(lotes);
    }
}
