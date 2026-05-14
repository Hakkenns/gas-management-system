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

    @GetMapping("/")
    public String inicio(Model model){

        model.addAttribute("contenido", "views/dashboard");

        model.addAttribute("menu",
                opcionService.listAll());

        return "components/layout";
    }
}