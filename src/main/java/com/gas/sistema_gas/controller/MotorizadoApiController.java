package com.gas.sistema_gas.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.PedidoDTO;

@RestController
public class MotorizadoApiController {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoPagoRepository pedidoPagoRepository;

    public MotorizadoApiController(UsuarioRepository usuarioRepository,
                                   PedidoRepository pedidoRepository,
                                   PedidoPagoRepository pedidoPagoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoPagoRepository = pedidoPagoRepository;
    }

    @GetMapping("/api/motorizado/historial/data")
    public ResponseEntity<?> obtenerDatosHistorial(HttpSession session,
                                                    @RequestParam(value = "metodoPago", required = false) String metodoPago) {
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }

        var usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEmpleado() == null || usuarioOpt.get().getEmpleado().getId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Empleado no válido"));
        }

        Long empleadoId = usuarioOpt.get().getEmpleado().getId();
        List<String> metodosPago = normalizarMetodosPago(metodoPago);
        List<Pedido> pedidos = pedidoRepository.findHistorialBase(empleadoId);

        if (metodosPago != null && !metodosPago.isEmpty()) {
            pedidos = pedidos.stream()
                    .filter(pedido -> {
                        List<PedidoPago> pagos = pedidoPagoRepository.findByPedido_Id(pedido.getId());
                        if (pagos.isEmpty()) {
                            return metodosPago.contains("EFECTIVO");
                        }
                        return pagos.stream().anyMatch(p -> {
                            String nombreMetodo = p.getMetodoPago() != null ? p.getMetodoPago().getNombre().toUpperCase() : "";
                            return metodosPago.contains(nombreMetodo);
                        });
                    })
                    .toList();
        }

        List<PedidoDTO.SimpleResponse> pedidosDto = pedidos.stream()
                .map(this::mapPedidoToSimpleResponse)
                .collect(Collectors.toList());

        BigDecimal totalRecaudado = pedidosDto.stream()
                .map(PedidoDTO.SimpleResponse::montoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "pedidos", pedidosDto,
                "totalPedidos", pedidosDto.size(),
                "totalRecaudado", totalRecaudado
        ));
    }

    private List<String> normalizarMetodosPago(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank() || "TODOS".equalsIgnoreCase(metodoPago.trim())) {
            return null;
        }

        return Arrays.stream(metodoPago.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .toList();
    }

    private PedidoDTO.SimpleResponse mapPedidoToSimpleResponse(Pedido pedido) {
        // Consultar los métodos de pago reales desde pedido_pagos
        List<PedidoPago> pagos = pedidoPagoRepository.findByPedido_Id(pedido.getId());
        String metodoPagoStr = null;
        if (pagos != null && !pagos.isEmpty()) {
            metodoPagoStr = pagos.stream()
                .map(p -> p.getMetodoPago() != null ? p.getMetodoPago().getNombre().toUpperCase() : "EFECTIVO")
                .distinct()
                .collect(Collectors.joining(", "));
        }

        return new PedidoDTO.SimpleResponse(
                pedido.getId(),
                pedido.getCodigo(),
                pedido.getFechaSolicitud(),
                pedido.getCliente() != null ? pedido.getCliente().getNombre() : null,
                pedido.getCliente() != null ? pedido.getCliente().getDireccion() : null,
                pedido.getEmpleado() != null ? pedido.getEmpleado().getId() : null,
                pedido.getObservaciones(),
                pedido.getEmpleado() != null ? pedido.getEmpleado().getNombre() : null,
                pedido.getEstadoPedido(),
                pedido.getEstadoPago(),
                pedido.getMontoTotal(),
                pedido.getSubtotal(),
                metodoPagoStr,
                pedido.getTipoVenta(),
                pedido.getFechaLimitePago()
        );
    }
}
