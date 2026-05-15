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
            categoriaService.updateCategory(id,
                    new CategoriaDTO.Update(categoriaDto.nombre(), categoriaDto.descripcion()));
        }

        return "redirect:/categorias";
    }

    @GetMapping("/estado/{id}/{estado}")
    public String cambiarEstado(@PathVariable Long id,
            @PathVariable Integer estado) {
        categoriaService.setState(id, estado);
        return "redirect:/categorias";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        categoriaService.deleteCategory(id);
        return "redirect:/categorias";
    }
}
