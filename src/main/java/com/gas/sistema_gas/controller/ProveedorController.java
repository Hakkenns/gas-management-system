package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gas.sistema_gas.dto.ProveedorDTO;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.ProveedorService;
import com.gas.sistema_gas.service.RubroService;
import java.util.Map;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private OpcionService opcionService;

    @Autowired
    private RubroService rubroService;

    @GetMapping
    public String proveedores(Model model, jakarta.servlet.http.HttpSession session) {
        cargarModeloProveedores(model, session);
        model.addAttribute("contenido", "views/proveedores");
        return "components/layout";
    }

    @GetMapping("/fragment")
    public String fragmentoProveedores(Model model, jakarta.servlet.http.HttpSession session) {
        cargarModeloProveedores(model, session);
        return "views/proveedores :: content";
    }

    private void cargarModeloProveedores(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("proveedores", proveedorService.listAll());
        model.addAttribute("rubros", rubroService.listAll());
    }

    //Carga asíncrona del fragmento HTML de la tabla
    @GetMapping("/tabla")
    public String tablaProveedores(Model model) {
        model.addAttribute("proveedores", proveedorService.listAll());
        return "views/proveedores :: tablaProveedores";
    }

    // Obtener datos de un proveedor para cargar el modal de edición
    @GetMapping("/{id}")
    @ResponseBody
    public ProveedorDTO.SimpleResponse getById(@PathVariable Long id) {
        return proveedorService.findById(id);
    }

    // Guardar / Actualizar proveedor vía AJAX
    @PostMapping
    @ResponseBody
    public Map<String, Object> guardarProveedor(@Valid ProveedorDTO.Create proveedorDto,
            BindingResult result,
            @RequestParam(required = false) String id) {

        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }

        Long proveedorId = null;
        if (id != null && !id.isBlank()) {
            proveedorId = Long.valueOf(id);
        }

        try {
            if (proveedorId == null) {
                proveedorService.create(proveedorDto);
            } else {
                proveedorService.update(proveedorId, new ProveedorDTO.Update(
                        proveedorDto.nombre(),
                        proveedorDto.idRubro(),
                        proveedorDto.telefono(),
                        proveedorDto.correo(),
                        proveedorDto.ruc()));
            }
            return Map.of("status", "OK");
        } catch (Exception e) {
            // Captura el mensaje de error del Service (ej: "El RUC ya está registrado")
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    //Cambiar estado vía AJAX (Activo/Inactivo)
    @PostMapping("/{id}/estado")
    @ResponseBody
    public Map<String, Object> cambiarEstadoAjax(@PathVariable Long id, @RequestParam Integer estado) {
        try {
            proveedorService.setState(id, estado);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo cambiar el estado.");
        }
    }

    //Eliminar proveedor vía AJAX (Borrado lógico)
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminarProveedorAjax(@PathVariable Long id) {
        try {
            proveedorService.delete(id);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo eliminar el proveedor.");
        }
    }
}
