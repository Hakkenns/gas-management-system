package com.gas.sistema_gas.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.dto.LoginDTO;
import com.gas.sistema_gas.service.AuthService;
import com.gas.sistema_gas.service.OpcionService;

@Controller
public class LoginController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OpcionService opcionService;

    /**
     * GET: Muestra el formulario de login
     */
    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        
        // Si ya hay un usuario en sesión, redirigir al dashboard
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/";
        }
        
        return "views/login";
    }

    /**
     * POST: Procesa la autenticación del usuario
     * Recibe username y password, y devuelve JSON
     */
    @PostMapping("/login")
    @ResponseBody
    public LoginDTO.Response processLogin(
            @Valid @RequestBody LoginDTO.Request request,
            HttpSession session) {
        
        try {
            // Autenticar usuario
            LoginDTO.Response usuario = authService.authenticate(request);
            
            // Guardar en sesión
            session.setAttribute("usuarioLogueado", usuario);
            session.setAttribute("usuarioId", usuario.id());
            session.setAttribute("usuarioPerfil", usuario.nombrePerfil());
            session.setAttribute("usuarioPerfilId", usuario.idPerfil());
            
            return usuario;
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error durante la autenticación"
            );
        }
    }

    /**
     * GET: Cierra la sesión del usuario
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /**
     * Devuelve la ruta de landing según el perfil del usuario en sesión.
     */
    @GetMapping("/api/landing")
    @ResponseBody
    public java.util.Map<String, String> landing(HttpSession session) {
        Long perfilId = (Long) session.getAttribute("usuarioPerfilId");

        if (perfilId == null) {
            return java.util.Map.of("path", "/");
        }

        // Admin ve dashboard
        if (perfilId.equals(1L)) {
            return java.util.Map.of("path", "/");
        }

        // Buscar la primera opción disponible para este perfil
        var opciones = opcionService.listByPerfilId(perfilId);
        if (opciones == null || opciones.isEmpty()) {
            return java.util.Map.of("path", "/");
        }

        String ruta = opciones.get(0).ruta();
        if (ruta == null || ruta.isBlank()) ruta = "/";

        return java.util.Map.of("path", ruta.startsWith("/") ? ruta : "/" + ruta);
    }

    /**
     * GET: Obtiene datos del usuario logueado (para AJAX)
     */
    @GetMapping("/api/usuario-actual")
    @ResponseBody
    public LoginDTO.Response getCurrentUser(HttpSession session) {
        LoginDTO.Response usuario = (LoginDTO.Response) session.getAttribute("usuarioLogueado");
        
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay usuario logueado");
        }
        
        return usuario;
    }
}
