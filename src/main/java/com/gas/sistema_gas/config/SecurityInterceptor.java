package com.gas.sistema_gas.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Interceptor de Seguridad
 * Verifica que el usuario esté autenticado en las rutas protegidas
 */
@Component("configSecurityInterceptor")
public class SecurityInterceptor implements HandlerInterceptor {

    /**
     * Rutas que NO requieren autenticación
     */
    private static final String[] ALLOWED_PATHS = {
        "/login",
        "/assets/",
        "/api/",           // APIs públicas si las necesitas
        "/error"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        String requestURI = request.getRequestURI();
        
        // Verificar si la ruta está permitida sin autenticación
        if (isAllowedPath(requestURI)) {
            return true;
        }
        
        // Obtener la sesión sin crear una nueva
        HttpSession session = request.getSession(false);
        
        // Si no hay sesión o no hay usuario logueado, redirigir a login
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect("/login");
            return false;
        }
        
        return true;
    }

    /**
     * Verifica si la ruta está en la lista de permitidas
     */
    private boolean isAllowedPath(String requestURI) {
        for (String allowedPath : ALLOWED_PATHS) {
            if (requestURI.startsWith(allowedPath)) {
                return true;
            }
        }
        return false;
    }

}
