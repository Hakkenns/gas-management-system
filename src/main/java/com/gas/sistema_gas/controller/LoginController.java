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

@Controller
public class LoginController {

    @Autowired
    private AuthService authService;

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
