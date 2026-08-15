package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Caja;
import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.EstadoSesionCaja;
import com.gas.sistema_gas.Model.MovimientoCaja;
import com.gas.sistema_gas.Model.OrigenMovimiento;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Model.SentidoMovimiento;
import com.gas.sistema_gas.Model.SesionCaja;
import com.gas.sistema_gas.Model.TipoFinancieroMetodoPago;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.CajaRepository;
import com.gas.sistema_gas.Repository.MovimientoCajaRepository;
import com.gas.sistema_gas.Repository.SesionCajaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.CajaDTO;
import com.gas.sistema_gas.service.CajaService;

@Service
public class CajaServiceImplement implements CajaService {

    public static final String CODIGO_CAJA_PRINCIPAL = "CAJA_PRINCIPAL";

    private final CajaRepository cajaRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final UsuarioRepository usuarioRepository;

    public CajaServiceImplement(CajaRepository cajaRepository,
            SesionCajaRepository sesionCajaRepository,
            MovimientoCajaRepository movimientoCajaRepository,
            UsuarioRepository usuarioRepository) {
        this.cajaRepository = cajaRepository;
        this.sesionCajaRepository = sesionCajaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public CajaDTO.AperturaResponse abrirCaja(Long usuarioId, BigDecimal montoInicial, String observaciones) {
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto inicial debe ser mayor o igual a cero");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Caja caja = cajaRepository.findByCodigoForUpdate(CODIGO_CAJA_PRINCIPAL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La Caja principal no está configurada"));

        if (!Boolean.TRUE.equals(caja.getActiva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Caja principal está inactiva");
        }

        if (sesionCajaRepository.findByCajaAndEstadoForUpdate(caja, EstadoSesionCaja.ABIERTA).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Caja principal ya tiene una sesión abierta");
        }

        LocalDateTime ahora = LocalDateTime.now();
        SesionCaja sesion = new SesionCaja();
        sesion.setCaja(caja);
        sesion.setFechaHoraApertura(ahora);
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        sesion.setUsuarioApertura(usuario);
        sesion.setObservaciones(observaciones);
        SesionCaja sesionGuardada = sesionCajaRepository.save(sesion);

        MovimientoCaja apertura = new MovimientoCaja();
        apertura.setCaja(caja);
        apertura.setSesionCaja(sesionGuardada);
        apertura.setFechaHora(ahora);
        apertura.setSentido(SentidoMovimiento.INGRESO);
        apertura.setOrigen(OrigenMovimiento.APERTURA);
        apertura.setCanalFondos(CanalFondos.CAJA_FISICA);
        apertura.setMonto(montoInicial);
        apertura.setUsuarioResponsable(usuario);
        apertura.setDescripcion("Apertura de Caja principal");
        MovimientoCaja aperturaGuardada = movimientoCajaRepository.save(apertura);

        return new CajaDTO.AperturaResponse(
                sesionGuardada.getId(),
                aperturaGuardada.getId(),
                caja.getCodigo(),
                sesionGuardada.getFechaHoraApertura(),
                montoInicial,
                sesionGuardada.getEstado().name());
    }

    @Override
    @Transactional
    public CajaDTO.CierreResponse cerrarCaja(Long usuarioId, BigDecimal montoDeclarado, String observaciones) {
        BigDecimal montoDeclaradoNormalizado = normalizarMontoDeclarado(montoDeclarado);

        Caja caja = cajaRepository.findByCodigoForUpdate(CODIGO_CAJA_PRINCIPAL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La Caja principal no esta configurada"));

        if (!Boolean.TRUE.equals(caja.getActiva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Caja principal esta inactiva");
        }

        SesionCaja sesion = sesionCajaRepository.findByCajaAndEstadoForUpdate(caja, EstadoSesionCaja.ABIERTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "La Caja principal no tiene una sesion abierta"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        BigDecimal efectivoEsperado = normalizarMonto(movimientoCajaRepository.calcularSaldoPorSesionYCanal(
                sesion,
                CanalFondos.CAJA_FISICA,
                SentidoMovimiento.INGRESO,
                SentidoMovimiento.EGRESO));
        BigDecimal diferencia = montoDeclaradoNormalizado.subtract(efectivoEsperado)
                .setScale(2, RoundingMode.UNNECESSARY);

        if (diferencia.compareTo(BigDecimal.ZERO) != 0 && !tieneTexto(observaciones)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La observacion de cierre es obligatoria cuando existe diferencia");
        }

        LocalDateTime ahora = LocalDateTime.now();
        sesion.setFechaHoraCierre(ahora);
        sesion.setUsuarioCierre(usuario);
        sesion.setMontoEsperadoCierre(efectivoEsperado);
        sesion.setMontoDeclaradoCierre(montoDeclaradoNormalizado);
        sesion.setDiferenciaCierre(diferencia);
        sesion.setEstado(EstadoSesionCaja.CERRADA);
        sesion.setObservaciones(anexarObservacionCierre(sesion.getObservaciones(), observaciones));
        SesionCaja sesionGuardada = sesionCajaRepository.save(sesion);

        return new CajaDTO.CierreResponse(
                sesionGuardada.getId(),
                caja.getCodigo(),
                sesionGuardada.getFechaHoraCierre(),
                sesionGuardada.getMontoEsperadoCierre(),
                sesionGuardada.getMontoDeclaradoCierre(),
                sesionGuardada.getDiferenciaCierre(),
                sesionGuardada.getEstado().name());
    }

    @Override
    @Transactional
    public void registrarIngresosVentaLocal(List<PedidoPago> pagos, Long usuarioId) {
        if (pagos == null || pagos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe registrar al menos un pago de venta local");
        }

        // El orden Caja -> SesionCaja debe coincidir con abrir/cerrar Caja para evitar deadlocks.
        Caja caja = cajaRepository.findByCodigoForUpdate(CODIGO_CAJA_PRINCIPAL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La Caja principal no esta configurada"));
        if (!Boolean.TRUE.equals(caja.getActiva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Caja principal esta inactiva");
        }

        SesionCaja sesion = sesionCajaRepository.findByCajaAndEstadoForUpdate(caja, EstadoSesionCaja.ABIERTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "La Caja principal no tiene una sesion abierta"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Validar por completo antes de escribir: una lista con un pago duplicado no deja movimientos parciales.
        for (PedidoPago pago : pagos) {
            validarPagoVentaLocal(pago);
            if (movimientoCajaRepository.findByPedidoPagoForUpdate(pago).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El pago de la venta local ya tiene un movimiento de Caja");
            }
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (PedidoPago pago : pagos) {
            Pedido pedido = pago.getPedido();
            MovimientoCaja movimiento = new MovimientoCaja();
            movimiento.setCaja(caja);
            movimiento.setSesionCaja(sesion);
            movimiento.setFechaHora(ahora);
            movimiento.setSentido(SentidoMovimiento.INGRESO);
            movimiento.setOrigen(OrigenMovimiento.VENTA);
            movimiento.setCanalFondos(pago.getMetodoPago().getTipoFinanciero() == TipoFinancieroMetodoPago.EFECTIVO
                    ? CanalFondos.CAJA_FISICA
                    : CanalFondos.DIGITAL_NEGOCIO);
            movimiento.setMonto(pago.getMonto());
            movimiento.setMetodoPago(pago.getMetodoPago());
            movimiento.setPedidoPago(pago);
            movimiento.setUsuarioResponsable(usuario);
            movimiento.setReferencia(pedido.getCodigo());
            movimiento.setDescripcion("Ingreso por venta local " + pedido.getCodigo());
            movimientoCajaRepository.save(movimiento);
        }
    }

    private void validarPagoVentaLocal(PedidoPago pago) {
        if (pago == null || pago.getId() == null || pago.getPedido() == null
                || pago.getMetodoPago() == null || pago.getMetodoPago().getTipoFinanciero() == null
                || pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El pago de venta local no tiene datos financieros validos");
        }
        if (!"LOCAL".equalsIgnoreCase(pago.getPedido().getTipoVenta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo los pagos de ventas locales pueden ingresar a Caja");
        }
    }

    private BigDecimal normalizarMontoDeclarado(BigDecimal montoDeclarado) {
        if (montoDeclarado == null || montoDeclarado.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto declarado debe ser mayor o igual a cero");
        }

        try {
            return montoDeclarado.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto declarado debe tener como maximo 2 decimales");
        }
    }

    private BigDecimal normalizarMonto(BigDecimal monto) {
        BigDecimal montoSeguro = monto != null ? monto : BigDecimal.ZERO;
        return montoSeguro.setScale(2, RoundingMode.UNNECESSARY);
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private String anexarObservacionCierre(String observacionExistente, String observacionCierre) {
        if (!tieneTexto(observacionCierre)) {
            return observacionExistente;
        }

        String cierreFormateado = "Cierre: " + observacionCierre.trim();
        if (!tieneTexto(observacionExistente)) {
            return cierreFormateado;
        }
        return observacionExistente + "\n" + cierreFormateado;
    }
}
