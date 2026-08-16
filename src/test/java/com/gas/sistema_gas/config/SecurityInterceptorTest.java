package com.gas.sistema_gas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityInterceptorTest {

    private final SecurityInterceptor interceptor = new SecurityInterceptor();

    @Test
    void apiCaja_sinSesion_responde401JsonSinRedirect() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/caja/custodias");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"message\":\"No autenticado\"}",
                response.getContentAsString(StandardCharsets.UTF_8));
        assertEquals(null, response.getRedirectedUrl());
    }

    @Test
    void rutaWeb_sinSesion_conservaRedirectAlLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/compras");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals(302, response.getStatus());
        assertEquals("/login", response.getRedirectedUrl());
    }
}
