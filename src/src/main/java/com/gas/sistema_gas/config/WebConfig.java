package com.gas.sistema_gas.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Web MVC
 * Registra los interceptores de seguridad y habilita recursos estáticos externos
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    @Qualifier("configSecurityInterceptor")
    private SecurityInterceptor securityInterceptor;

    @Value("${app.product-images.dir:uploads/product-images}")
    private String productoImagesDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")  // Aplicar a todas las rutas
                .excludePathPatterns(    // Excepto estas
                    "/login",
                    "/assets/**",
                    "/img/**",
                    "/images/**",
                    "/error",
                    "/h2-console/**"     // Si usas H2 console
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(productoImagesDir).toAbsolutePath().normalize();
        String resourceLocation = uploadDir.toUri().toString();

        registry.addResourceHandler("/images/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);
    }

}
