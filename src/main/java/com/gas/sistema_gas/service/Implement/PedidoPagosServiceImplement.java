package com.gas.sistema_gas.service.Implement;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Evidencia;
import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Repository.EvidenciaRepository;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.dto.ConfirmarEntregaMixtaDTO;
import com.gas.sistema_gas.dto.PagoRegistroDTO;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;
import com.gas.sistema_gas.service.PedidoPagosService;

import jakarta.transaction.Transactional;

@Service
public class PedidoPagosServiceImplement implements PedidoPagosService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private PedidoPagoRepository pedidoPagoRepository;

    @Autowired
    private EvidenciaRepository evidenciaRepository;

    @Value("${app.evidencias.dir:src/main/resources/static/imagenes-sistema}")
    private String evidenciasDir;

    @Override
    @Transactional
    public PedidoPago registrarPagoYape(PedidoPagoYapeDTO dto, MultipartFile evidencia) {
        return registrarPagoYape(dto, evidencia, null);
    }

    @Override
    @Transactional
    public PedidoPago registrarPagoYape(PedidoPagoYapeDTO dto, MultipartFile evidencia, MultipartFile evidenciaVuelto) {
        if (dto == null || dto.idPedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido inválido");
        }

        // Obtener pedido con lock para evitar condiciones de carrera
        Pedido pedido = pedidoRepository.findByIdForUpdate(dto.idPedido)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        // Log para depuración de llamadas concurrentes
        System.out.println("[PedidoPagosService] registrarPagoYape called for pedido=" + dto.idPedido + " montoRecibido=" + dto.montoRecibido + " op=" + dto.numOperacion);

        // Evitar registros duplicados: si el pedido ya está entregado, devolver el pago ya existente
        if (pedido.getEstadoPedido() != null && pedido.getEstadoPedido().equalsIgnoreCase("ENTREGADO")) {
            java.util.List<PedidoPago> pagosExistentes = pedidoPagoRepository.findByPedido(pedido);
            if (!pagosExistentes.isEmpty()) {
                return pagosExistentes.get(0);
            }
            // si por alguna razón no hay pagos pero estado es ENTREGADO, continuar para crear uno
        }

        MetodoPago metodo;
        if (dto.idMetodo != null) {
            metodo = metodoPagoRepository.findById(dto.idMetodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago no encontrado"));
        } else {
            metodo = metodoPagoRepository.findByNombre("Yape")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago Yape no configurado"));
        }

        BigDecimal montoTotal = pedido.getMontoTotal() != null ? pedido.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal montoRecibido = dto.montoRecibido != null ? dto.montoRecibido : BigDecimal.ZERO;
        BigDecimal vuelto = montoRecibido.subtract(montoTotal);
        if (vuelto.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto recibido es menor al total a cobrar");
        }

        // 1) Crear registro de pago
        PedidoPago pago = new PedidoPago();
        pago.setPedido(pedido);
        pago.setMetodoPago(metodo);
        pago.setMonto(montoTotal);
        pago.setNumOperacion(dto.numOperacion);
        pago.setVuelto(vuelto);

        PedidoPago pagoGuardado = pedidoPagoRepository.save(pago);

        // 2) Guardar evidencia del PAGO si viene archivo
        if (evidencia != null && !evidencia.isEmpty()) {
            String url = almacenarImagen(evidencia);
            Evidencia ev = new Evidencia();
            ev.setPedidoPago(pagoGuardado);
            ev.setUrlImagen(url);
            ev.setTipoEvidencia("PAGO");
            evidenciaRepository.save(ev);
        }

        // 3) Guardar evidencia de VUELTO si se proporcionó
        if (evidenciaVuelto != null && !evidenciaVuelto.isEmpty()) {
            String url = almacenarImagen(evidenciaVuelto);
            Evidencia ev = new Evidencia();
            ev.setPedidoPago(pagoGuardado);
            ev.setUrlImagen(url);
            ev.setTipoEvidencia("VUELTO");
            evidenciaRepository.save(ev);
        }

        // 4) Marcar pedido como ENTREGADO y recalcular el estado de pago
        //    según la suma REAL de pedido_pagos vs montoTotal (independiente del estadoPedido)
        pedido.setEstadoPedido("ENTREGADO");
        if (pedido.getFechaEntrega() == null) {
            pedido.setFechaEntrega(java.time.LocalDateTime.now());
        }
        recalcularEstadoPago(pedido);
        // Persistir explícitamente los cambios en la tabla pedidos
        pedidoRepository.save(pedido);

        return pagoGuardado;
    }

    @Override
    @Transactional
    public List<PedidoPago> registrarPagosMultiples(ConfirmarEntregaMixtaDTO dto, List<MultipartFile> evidencias) {
        return registrarPagosMultiples(dto, evidencias, null);
    }

    @Override
    @Transactional
    public List<PedidoPago> registrarPagosMultiples(ConfirmarEntregaMixtaDTO dto, List<MultipartFile> evidencias, MultipartFile evidenciaVuelto) {
        if (dto == null || dto.idPedido == null || dto.pagos == null || dto.pagos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de pago inválidos");
        }

        Pedido pedido = pedidoRepository.findByIdForUpdate(dto.idPedido)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        BigDecimal montoTotal = pedido.getMontoTotal();
        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido debe tener un monto total mayor a cero");
        }

        // 1) Validar y resolver TODOS los pagos ANTES de tocar la persistencia.
        //    No se usa continue silencioso: cualquier pago inválido aborta la operación.
        record PagoResuelto(PagoRegistroDTO dto, MetodoPago metodo) {}
        List<PagoResuelto> pagosResueltos = new ArrayList<>();
        for (PagoRegistroDTO pagoDto : dto.pagos) {
            if (pagoDto.idMetodo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago no válido");
            }
            if (pagoDto.monto == null || pagoDto.monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
            }
            MetodoPago metodo = metodoPagoRepository.findById(pagoDto.idMetodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pago no encontrado"));
            pagosResueltos.add(new PagoResuelto(pagoDto, metodo));
        }

        if (pagosResueltos.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago mixto requiere al menos dos métodos de pago");
        }

        // 2) El pago mixto exige al menos dos métodos de pago DIFERENTES.
        long metodosDistintos = pagosResueltos.stream()
                .map(p -> p.metodo.getId())
                .distinct()
                .count();
        if (metodosDistintos < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago mixto requiere al menos dos métodos de pago diferentes");
        }

        // 3) El campo monto del DTO representa el dinero recibido por ese método.
        BigDecimal totalRecibido = pagosResueltos.stream()
                .map(p -> p.dto.monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRecibido.compareTo(montoTotal) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El total pagado no cubre el monto del pedido");
        }

        // 4) El vuelto se calcula EXCLUSIVAMENTE en backend. pagoDto.vuelto se ignora.
        BigDecimal vueltoTotal = totalRecibido.subtract(montoTotal);

        PagoResuelto pagoEfectivoConVuelto = null;
        if (vueltoTotal.compareTo(BigDecimal.ZERO) > 0) {
            pagoEfectivoConVuelto = pagosResueltos.stream()
                    .filter(p -> "Efectivo".equalsIgnoreCase(p.metodo.getNombre()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No se puede entregar vuelto sin un pago en efectivo"));
            if (pagoEfectivoConVuelto.dto.monto.compareTo(vueltoTotal) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El efectivo recibido no cubre el vuelto a entregar");
            }
            // El monto aplicado al efectivo debe ser estrictamente mayor que cero
            BigDecimal montoAplicadoEfectivo = pagoEfectivoConVuelto.dto.monto.subtract(vueltoTotal);
            if (montoAplicadoEfectivo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada método debe aplicar un monto mayor a cero al pedido");
            }
        }

        // 4) Validaciones completas: recién ahora se puede reemplazar el historial de pagos.
        List<PedidoPago> pagosAnteriores = pedidoPagoRepository.findByPedido(pedido);
        if (!pagosAnteriores.isEmpty()) {
            pedidoPagoRepository.deleteAll(pagosAnteriores);
            pedidoPagoRepository.flush();
        }

        List<PedidoPago> pagosGuardados = new ArrayList<>();
        int evidenciaIndex = 0;

        for (PagoResuelto pagoResuelto : pagosResueltos) {
            boolean esEfectivoConVuelto = pagoResuelto == pagoEfectivoConVuelto;

            PedidoPago pago = new PedidoPago();
            pago.setPedido(pedido);
            pago.setMetodoPago(pagoResuelto.metodo);
            pago.setNumOperacion(pagoResuelto.dto.numOperacion);
            if (esEfectivoConVuelto) {
                // El vuelto se descuenta únicamente del primer pago en Efectivo
                pago.setMonto(pagoResuelto.dto.monto.subtract(vueltoTotal));
                pago.setVuelto(vueltoTotal);
            } else {
                pago.setMonto(pagoResuelto.dto.monto);
                pago.setVuelto(BigDecimal.ZERO);
            }

            PedidoPago pagoGuardado = pedidoPagoRepository.save(pago);

            // La evidencia del vuelto se asocia al pago en Efectivo que entrega el vuelto
            if (esEfectivoConVuelto && evidenciaVuelto != null && !evidenciaVuelto.isEmpty()) {
                String url = almacenarImagen(evidenciaVuelto);
                Evidencia ev = new Evidencia();
                ev.setPedidoPago(pagoGuardado);
                ev.setUrlImagen(url);
                ev.setTipoEvidencia("VUELTO");
                evidenciaRepository.save(ev);
            }

            // Guardar evidencia si hay archivo disponible para este pago
            if (evidencias != null && evidenciaIndex < evidencias.size()) {
                MultipartFile evidencia = evidencias.get(evidenciaIndex);
                if (evidencia != null && !evidencia.isEmpty()) {
                    String url = almacenarImagen(evidencia);
                    Evidencia ev = new Evidencia();
                    ev.setPedidoPago(pagoGuardado);
                    ev.setUrlImagen(url);
                    ev.setTipoEvidencia("PAGO");
                    evidenciaRepository.save(ev);
                }
            }
            evidenciaIndex++;

            pagosGuardados.add(pagoGuardado);
        }

        // Salvaguarda: nunca marcar ENTREGADO sin pagos persistidos
        if (pagosGuardados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo registrar ningún pago");
        }

        // Marcar pedido como ENTREGADO y recalcular el estado de pago
        // según la suma REAL de pedido_pagos vs montoTotal (independiente del estadoPedido)
        pedido.setEstadoPedido("ENTREGADO");
        if (pedido.getFechaEntrega() == null) {
            pedido.setFechaEntrega(java.time.LocalDateTime.now());
        }
        recalcularEstadoPago(pedido);
        // Persistir explícitamente los cambios en la tabla pedidos (estado_pedido, fecha_entrega, estado_pago)
        pedidoRepository.save(pedido);

        return pagosGuardados;
    }

    @Override
    public List<PedidoPago> findByPedidoId(Long idPedido) {
        return pedidoPagoRepository.findByPedido_Id(idPedido);
    }

    /**
     * Recalcula el estadoPago del pedido a partir de la suma REAL de pedido_pagos
     * comparada con el montoTotal. Un pedido puede estar ENTREGADO y seguir
     * PENDIENTE, CREDITO o con pago parcial.
     */
    private void recalcularEstadoPago(Pedido pedido) {
        BigDecimal montoTotal = pedido.getMontoTotal() != null ? pedido.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal totalPagado = pedidoPagoRepository.findByPedido(pedido).stream()
                .map(pago -> pago.getMonto() != null ? pago.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagado.compareTo(montoTotal) >= 0 && montoTotal.compareTo(BigDecimal.ZERO) > 0) {
            pedido.setEstadoPago("PAGADO");
        } else if (totalPagado.compareTo(BigDecimal.ZERO) > 0 || pedido.getFechaLimitePago() != null) {
            pedido.setEstadoPago("CREDITO");
        } else {
            pedido.setEstadoPago("PENDIENTE");
        }
    }

    private String almacenarImagen(MultipartFile archivoImagen) {
        if (archivoImagen == null || archivoImagen.isEmpty()) {
            return null;
        }

        if (archivoImagen.getSize() > 15 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen no puede superar los 15 MB");
        }

        String contentType = archivoImagen.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser una imagen válida");
        }

        String originalFilename = org.springframework.util.StringUtils.cleanPath(archivoImagen.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path uploadPath = Paths.get(evidenciasDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            Path target = uploadPath.resolve(filename);
            Files.copy(archivoImagen.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/imagenes-sistema/" + filename;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar la imagen", ex);
        }
    }
}
