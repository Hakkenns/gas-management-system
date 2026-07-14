package com.gas.sistema_gas.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.service.OpcionService;
import com.gas.sistema_gas.service.PedidoService;

@Controller
public class DashboardController {

    @Autowired
    private OpcionService opcionService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * GET: Ruta principal redirige al dashboard
     */
    @GetMapping("/")
    public String home(HttpServletRequest request, HttpServletResponse response, Model model, HttpSession session) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        if (perfilId != null && perfilId.equals(4L)) {
            return "redirect:/motorizado/asignados";
        }

        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("ventasHoyCount", pedidoService.countSalesToday());
        model.addAttribute("usuariosActivosCount", usuarioRepository.countByEstado(1));
        model.addAttribute("contenido", "views/dashboard");
        return "components/layout";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {

        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");
        model.addAttribute("menu", opcionService.listByPerfilId(perfilId));
        model.addAttribute("ventasHoyCount", pedidoService.countSalesToday());
        model.addAttribute("usuariosActivosCount", usuarioRepository.countByEstado(1));
        model.addAttribute("contenido", "views/dashboard");

        return "components/layout";
    }

}