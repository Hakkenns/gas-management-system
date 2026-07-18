package com.gas.sistema_gas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefijo para los canales a los que se suscriben los clientes
        config.enableSimpleBroker("/topic");
        // Prefijo para los mensajes enviados desde el cliente al servidor (si se necesita)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint que los clientes usarán para conectarse via WebSocket con SockJS fallback
        registry.addEndpoint("/ws-repartidor")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // Endpoint adicional para administradores (notificaciones de incidencias)
        registry.addEndpoint("/ws-admin")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}