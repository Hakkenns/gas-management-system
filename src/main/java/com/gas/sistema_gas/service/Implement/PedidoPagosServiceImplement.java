package com.gas.sistema_gas.service.Implement;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @Value("${app.product-images.dir:uploads/product-images}")
    private String productoImagesDir;

    @Override
    @Transactional
    public PedidoPago registrarPagoYape(PedidoPagoYapeDTO dto, MultipartFile evidencia) {
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

        // 2) Guardar evidencia si viene archivo
        if (evidencia != null && !evidencia.isEmpty()) {
            String url = almacenarImagen(evidencia);
            Evidencia ev = new Evidencia();
            ev.setPedidoPago(pagoGuardado);
            ev.setUrlImagen(url);
            ev.setTipoEvidencia("PAGO");
            evidenciaRepository.save(ev);
        }

        // 3) Marcar pedido como ENTREGADO y PAGADO, además registrar método de pago en el pedido
        pedido.setEstadoPedido("ENTREGADO");
        pedido.setNumOperacion(dto.numOperacion);
        if (pedido.getFechaEntrega() == null) {
            pedido.setFechaEntrega(java.time.LocalDateTime.now());
        }

        // Actualización explícita de la tabla pedidos para garantizar que se guarde id_metodo y estado_pago
        pedidoRepository.updateEstadoPagoAndMetodoById(pedido.getId(), "PAGADO", metodo);

        // Mantener el estado del entity manager consistente
        pedido.setEstadoPago("PAGADO");
        pedido.setMetodoPago(metodo);

        return pagoGuardado;
    }

    private String almacenarImagen(MultipartFile archivoImagen) {
        if (archivoImagen == null || archivoImagen.isEmpty()) {
            return null;
        }

        if (archivoImagen.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen no puede superar los 5 MB");
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
        Path uploadPath = Paths.get(productoImagesDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            Path target = uploadPath.resolve(filename);
            Files.copy(archivoImagen.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/images/" + filename;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar la imagen", ex);
        }
    }
}
