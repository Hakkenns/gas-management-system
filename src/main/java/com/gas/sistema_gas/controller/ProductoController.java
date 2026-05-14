package com.gas.sistema_gas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gas.sistema_gas.service.PerfilService;
@RequestMapping("/usuarios")
@Controller
public class ProductoController {

    @Autowired
    private PerfilService perfilService;
    
}