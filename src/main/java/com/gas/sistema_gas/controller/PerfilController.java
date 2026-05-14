package com.gas.sistema_gas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PerfilService;

@Controller
public class PerfilController {

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private OpcionService opcionService;

    @GetMapping("/perfiles")
    public String perfiles(Model model){

        model.addAttribute("menu",
                opcionService.listAll());

        model.addAttribute("perfiles",
                perfilService.listActive());

        model.addAttribute("contenido",
                "views/perfiles");

        return "components/layout";
    }

}