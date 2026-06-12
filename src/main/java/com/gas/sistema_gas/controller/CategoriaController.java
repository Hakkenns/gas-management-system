package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;
import java.util.Map;
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

import com.gas.sistema_gas.dto.CategoriaDTO;
import com.gas.sistema_gas.service.CategoriaService;
import com.gas.sistema_gas.service.OpcionService;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private OpcionService opcionService;

    @GetMapping
    public String categorias(Model model, jakarta.servlet.http.HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("categorias", categoriaService.listAll());
        model.addAttribute("contenido", "views/categoria");
        return "components/layout";
    }

    // 🔄 NUEVA RUTA: Retorna solo el fragmento HTML de la tabla para refrescar con
    // AJAX
    @GetMapping("/tabla")
    public String tablaCategorias(Model model) {
        model.addAttribute("categorias", categoriaService.listAll());
        return "views/categoria :: tablaCategorias";
    }

    @PostMapping
    public String guardarCategoria(@Valid CategoriaDTO.Create categoriaDto,
            BindingResult result,
            @RequestParam(required = false) Long id,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("menu", opcionService.listAll());
            model.addAttribute("categorias", categoriaService.listAll());
            model.addAttribute("contenido", "views/categoria");
            return "components/layout";
        }

        if (id == null) {
            categoriaService.createCategory(categoriaDto);
        } else {
            // CORREGIDO: Ahora enviamos también el 'tipoUnidad' al actualizar
            categoriaService.updateCategory(id,
                    new CategoriaDTO.Update(categoriaDto.nombre(), categoriaDto.descripcion(),
                            categoriaDto.tipoUnidad()));
        }

        return "redirect:/categorias";
    }

    @PostMapping(value = "/ajax")
    @ResponseBody
    public Map<String, Object> guardarCategoriaAjax(@RequestParam(required = false) Long id,
            @Valid CategoriaDTO.Create categoriaDto,
            BindingResult result) {
        if (result.hasErrors()) {
            String message = result.getAllErrors().get(0).getDefaultMessage();
            return Map.of("status", "ERROR", "message", message);
        }

        if (id == null) {
            categoriaService.createCategory(categoriaDto);
        } else {
            // CORREGIDO: Ahora enviamos también el 'tipoUnidad' en la petición por AJAX
            categoriaService.updateCategory(id,
                    new CategoriaDTO.Update(categoriaDto.nombre(), categoriaDto.descripcion(),
                            categoriaDto.tipoUnidad()));
        }

        return Map.of("status", "OK");
    }

    // 🟢 OPTIMIZADO PARA AJAX: Cambiar Estado sin recargar página
    @PostMapping("/{id}/estado")
    @ResponseBody
    public Map<String, Object> cambiarEstado(@PathVariable Long id, @RequestParam Integer estado) {
        try {
            categoriaService.setState(id, estado);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo cambiar el estado.");
        }
    }

    // 🟢 OPTIMIZADO PARA AJAX: Eliminar sin recargar página
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public Map<String, Object> eliminar(@PathVariable Long id) {
        try {
            categoriaService.deleteCategory(id);
            return Map.of("status", "OK");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "No se pudo eliminar la categoría.");
        }
    }
}
