package com.gas.sistema_gas.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gas.sistema_gas.interceptor.SecurityInterceptor;

/**
 * Configuración de Spring MVC
 * Registra interceptores de seguridad
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/login",
                    "/assets/**",
                    "/img/**",
                    "/error",
                    "/h2-console/**"
                );
    }
}
