package com.gas.sistema_gas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gas.sistema_gas.service.OpcionService;

@Controller
public class DashboardController {

    @Autowired
    private OpcionService opcionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("menu", opcionService.listAll());

        model.addAttribute("contenido", "views/dashboard");

        return "components/layout";
    }

}