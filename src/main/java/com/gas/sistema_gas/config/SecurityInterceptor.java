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
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        // Desactivar cache en todas las rutas protegidas para evitar que el navegador
        // muestre vistas antiguas al usar atrás/recargar
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // Protección adicional: evitar que perfiles distintos accedan a rutas de motorizado/ventas
        Long perfilId = session.getAttribute("usuarioPerfilId") instanceof Long ? (Long) session.getAttribute("usuarioPerfilId") : null;

        // Si la ruta es /motorizado/*, sólo permitir perfil motorizado (4)
        if (requestURI != null && requestURI.startsWith("/motorizado")) {
            if (perfilId == null || perfilId != 4L) {
                response.sendRedirect("/");
                return false;
            }
        }

        // Si la ruta es /ventas/* y el usuario es motorizado, redirigir a su panel
        if (requestURI != null && requestURI.startsWith("/ventas")) {
            if (perfilId != null && perfilId == 4L) {
                response.sendRedirect("/motorizado/asignados");
                return false;
            }
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
