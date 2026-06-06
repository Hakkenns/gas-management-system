package com.gas.sistema_gas.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor de seguridad que valida que el usuario esté logueado
 * antes de acceder a rutas protegidas
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String requestURI = request.getRequestURI();

        // Rutas públicas que NO requieren autenticación
        if (isPublicRoute(requestURI)) {
            return true;
        }

        // Obtener sesión (false = no crear si no existe)
        HttpSession session = request.getSession(false);

        // Si no hay sesión o no hay usuario logueado, redirigir al login
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        return true;
    }

    /**
     * Define qué rutas son públicas (no requieren autenticación)
     */
    private boolean isPublicRoute(String requestURI) {
        // "/" es una ruta protegida si no hay usuario logueado
        // /login es pública
        return requestURI.contains("/login")
            || requestURI.contains("/logout")
            || requestURI.contains("/assets/")
            || requestURI.contains("/img/")
            || requestURI.contains("/css/")
            || requestURI.contains("/js/");
    }
}
