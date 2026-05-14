package com.gas.sistema_gas.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gas.sistema_gas.dto.UsuarioDTO;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PerfilService;
import com.gas.sistema_gas.service.UsuarioService;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private PerfilService perfilService;
    @Autowired
    private OpcionService opcionService;

    // ─── GET: carga la vista ───────────────────────────────────────────
    @GetMapping
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listAll());
        model.addAttribute("perfiles", perfilService.listActive());
        model.addAttribute("menu",     opcionService.listAll());
        model.addAttribute("contenido", "views/usuario");
        return "components/layout";
    }

    // ─── GET por ID: retorna JSON para el JS del modal editar ──────────
    @GetMapping("/{id}")
    @ResponseBody
    public UsuarioDTO.SimpleResponse getById(@PathVariable Long id) {
        return usuarioService.findById(id);
    }

    // ─── POST: crear nuevo usuario ─────────────────────────────────────
    @PostMapping
    public String crear(
            @ModelAttribute @Valid UsuarioDTO.Create create,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("usuarios",  usuarioService.listAll());
            model.addAttribute("perfiles",  perfilService.listActive());
            model.addAttribute("menu",      opcionService.listAll());
            model.addAttribute("contenido", "views/usuario");
            return "components/layout";
        }

        try {
            usuarioService.createUser(create);
            return "redirect:/usuarios";
        } catch (Exception e) {
            model.addAttribute("error",     e.getMessage());
            model.addAttribute("usuarios",  usuarioService.listAll());
            model.addAttribute("perfiles",  perfilService.listActive());
            model.addAttribute("menu",      opcionService.listAll());
            model.addAttribute("contenido", "views/usuario");
            return "components/layout";
        }
    }

    // ─── POST: editar usuario (simula PUT desde HTML form) ─────────────
    @PostMapping("/{id}/editar")
    public String editar( @PathVariable Long id, @ModelAttribute @Valid UsuarioDTO.Update update, BindingResult result, Model model) {

        if (result.hasErrors()) {
            model.addAttribute("usuarios",  usuarioService.listAll());
            model.addAttribute("perfiles",  perfilService.listActive());
            model.addAttribute("menu",      opcionService.listAll());
            model.addAttribute("contenido", "views/usuario");
            return "components/layout";
        }

        try {
            usuarioService.updateUser(id, update);
            return "redirect:/usuarios";
        } catch (Exception e) {
            model.addAttribute("error",     e.getMessage());
            model.addAttribute("usuarios",  usuarioService.listAll());
            model.addAttribute("perfiles",  perfilService.listActive());
            model.addAttribute("menu",      opcionService.listAll());
            model.addAttribute("contenido", "views/usuario");
            return "components/layout";
        }
    }
}