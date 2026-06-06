package com.gas.sistema_gas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;

import com.gas.sistema_gas.service.OpcionService;

@Controller
public class DashboardController {

    @Autowired
    private OpcionService opcionService;

    /**
     * GET: Ruta principal redirige al dashboard
     */
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("contenido", "views/dashboard");
        return "components/layout";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {

        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));

        model.addAttribute("contenido", "views/dashboard");

        return "components/layout";
    }

}