package com.gas.sistema_gas.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Web MVC
 * Registra los interceptores de seguridad
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    @Qualifier("configSecurityInterceptor")
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")  // Aplicar a todas las rutas
                .excludePathPatterns(    // Excepto estas
                    "/login",
                    "/assets/**",
                    "/img/**",
                    "/error",
                    "/h2-console/**"     // Si usas H2 console
                );
    }

}
