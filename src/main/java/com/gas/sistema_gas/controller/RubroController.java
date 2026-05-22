package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.gas.sistema_gas.dto.RubroDTO;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.RubroService;

import java.util.Map;

@Controller
@RequestMapping("/rubros")
public class RubroController {

    @Autowired
    private RubroService rubroService;

    @Autowired
    private OpcionService opcionService;

    @GetMapping
    public String rubros(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("rubros", rubroService.listAll());
        model.addAttribute("contenido", "views/rubros");
        return "components/layout";
    }

    @GetMapping("/tabla")
    public String tablaRubros(Model model) {
        model.addAttribute("rubros", rubroService.listAll());
        return "views/rubros :: tablaRubros";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public RubroDTO.SimpleResponse getById(@PathVariable Long id) {
        return rubroService.findById(id);
    }

    @PostMapping
    @ResponseBody
    public Map<String, Object> crear(@Valid RubroDTO.Create rubroDto, BindingResult result) {
        if (result.hasErrors()) {
            return Map.of("status", "ERROR", "message", "Campos obligatorios vacíos.");
        }
        rubroService.create(rubroDto);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/editar")
    @ResponseBody
    public Map<String, Object> editar(@PathVariable Long id, @Valid RubroDTO.Update rubroDto, BindingResult result) {
        if (result.hasErrors()) {
            return Map.of("status", "ERROR", "message", "Datos inválidos para actualizar.");
        }
        rubroService.update(id, rubroDto);
        return Map.of("status", "OK");
    }

    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminar(@PathVariable Long id) {
        rubroService.delete(id);
        return Map.of("status", "OK");
    }

    // NUEVA RUTA ASÍNCRONA AGREGADA
    @PostMapping("/{id}/estado")
    @ResponseBody
    public Map<String, Object> cambiarEstado(@PathVariable Long id, @RequestParam Integer estado) {
        try {
            rubroService.setState(id, estado);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo cambiar el estado.");
        }
    }
}