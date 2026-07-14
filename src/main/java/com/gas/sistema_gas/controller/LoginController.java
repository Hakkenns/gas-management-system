package com.gas.sistema_gas.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    public String showLoginForm(Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("usuarioLogueado") != null) {
            session.invalidate();
            Cookie cookie = new Cookie("JSESSIONID", "");
            cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
            cookie.setMaxAge(0);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
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
            HttpServletRequest requestHttp) {
        
        try {
            // Autenticar usuario
            LoginDTO.Response usuario = authService.authenticate(request);
            
            // Invalidar sesión anterior si existía, para evitar mezcla de usuarios
            HttpSession previousSession = requestHttp.getSession(false);
            if (previousSession != null) {
                previousSession.invalidate();
            }
            HttpSession session = requestHttp.getSession(true);

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
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("usuarioLogueado");
            session.removeAttribute("usuarioPerfilId");
            session.removeAttribute("usuarioPerfil");
            session.invalidate();
        }

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        response.addCookie(cookie);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

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

        // Motorizado ve su pantalla asignada
        if (perfilId.equals(4L)) {
            return java.util.Map.of("path", "/motorizado/asignados");
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
