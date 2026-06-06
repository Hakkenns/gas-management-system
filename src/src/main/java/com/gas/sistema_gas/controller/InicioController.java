package com.gas.sistema_gas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gas.sistema_gas.service.OpcionService;

@Controller
public class InicioController {

    @Autowired
    private OpcionService opcionService;

    @GetMapping("/inicio")
    public String inicio(Model model, jakarta.servlet.http.HttpSession session){

        model.addAttribute("contenido", "views/dashboard");

        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));

        return "components/layout";
    }
}