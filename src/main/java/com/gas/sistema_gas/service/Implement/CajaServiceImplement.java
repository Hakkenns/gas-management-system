package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Caja;
import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.Empleado;
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
import com.gas.sistema_gas.Repository.EmpleadoRepository;
import com.gas.sistema_gas.Repository.MovimientoCajaRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.SesionCajaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.CajaDTO;
import com.gas.sistema_gas.service.CajaService;

@Service
public class CajaServiceImplement implements CajaService {

    public static final String CODIGO_CAJA_PRINCIPAL = "CAJA_PRINCIPAL";
    private static final String UK_MOVIMIENTO_CAJA_PEDIDO_PAGO = "uk_movimiento_caja_pedido_pago";

    private final CajaRepository cajaRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final PedidoPagoRepository pedidoPagoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;

    public CajaServiceImplement(CajaRepository cajaRepository,
            SesionCajaRepository sesionCajaRepository,
            MovimientoCajaRepository movimientoCajaRepository,
            PedidoPagoRepository pedidoPagoRepository,
            EmpleadoRepository empleadoRepository,
            UsuarioRepository usuarioRepository) {
        this.cajaRepository = cajaRepository;
        this.sesionCajaRepository = sesionCajaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.pedidoPagoRepository = pedidoPagoRepository;
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public CajaDTO.AperturaResponse abrirCaja(Long usuarioId, BigDecimal montoInicial, String observaciones) {
        BigDecimal montoInicialNormalizado = normalizarMontoInicial(montoInicial);

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
        apertura.setMonto(montoInicialNormalizado);
        apertura.setUsuarioResponsable(usuario);
        apertura.setDescripcion("Apertura de Caja principal");
        MovimientoCaja aperturaGuardada = movimientoCajaRepository.save(apertura);

        return new CajaDTO.AperturaResponse(
                sesionGuardada.getId(),
                aperturaGuardada.getId(),
                caja.getCodigo(),
                sesionGuardada.getFechaHoraApertura(),
                montoInicialNormalizado,
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
    public CajaDTO.LiquidacionResponse liquidarCustodiaMotorizado(CajaDTO.LiquidacionRequest request,
            Long usuarioCajaId) {
        if (request == null || request.empleadoCustodioId() == null || usuarioCajaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El empleado custodio y el usuario de Caja son obligatorios");
        }
        BigDecimal montoLiquidado = normalizarMontoLiquidacion(request.monto());

        Empleado empleadoCustodio = empleadoRepository.findByIdForUpdate(request.empleadoCustodioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

        Caja caja = cajaRepository.findByCodigoForUpdate(CODIGO_CAJA_PRINCIPAL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La Caja principal no esta configurada"));
        if (!Boolean.TRUE.equals(caja.getActiva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Caja principal esta inactiva");
        }

        SesionCaja sesion = sesionCajaRepository.findByCajaAndEstadoForUpdate(caja, EstadoSesionCaja.ABIERTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "La Caja principal no tiene una sesion abierta"));

        BigDecimal saldoAnterior = normalizarMonto(movimientoCajaRepository.calcularSaldoCustodiaPorEmpleado(
                empleadoCustodio,
                CanalFondos.CUSTODIA_MOTORIZADO,
                SentidoMovimiento.INGRESO,
                SentidoMovimiento.EGRESO));
        if (saldoAnterior.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El motorizado no tiene efectivo pendiente de liquidar");
        }
        if (montoLiquidado.compareTo(saldoAnterior) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El monto de liquidación supera el efectivo pendiente del motorizado");
        }

        Usuario usuarioCaja = usuarioRepository.findById(usuarioCajaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        LocalDateTime ahora = LocalDateTime.now();
        String referencia = "LIQ-" + UUID.randomUUID();

        MovimientoCaja egresoCustodia = new MovimientoCaja();
        egresoCustodia.setCaja(caja);
        egresoCustodia.setSesionCaja(null);
        egresoCustodia.setFechaHora(ahora);
        egresoCustodia.setSentido(SentidoMovimiento.EGRESO);
        egresoCustodia.setOrigen(OrigenMovimiento.LIQUIDACION_MOTORIZADO);
        egresoCustodia.setCanalFondos(CanalFondos.CUSTODIA_MOTORIZADO);
        egresoCustodia.setMonto(montoLiquidado);
        egresoCustodia.setUsuarioResponsable(usuarioCaja);
        egresoCustodia.setEmpleadoCustodio(empleadoCustodio);
        egresoCustodia.setReferencia(referencia);
        egresoCustodia.setDescripcion(descripcionLiquidacion("Egreso de custodia de motorizado", request.observacion()));
        MovimientoCaja egresoGuardado = movimientoCajaRepository.saveAndFlush(egresoCustodia);

        MovimientoCaja ingresoCaja = new MovimientoCaja();
        ingresoCaja.setCaja(caja);
        ingresoCaja.setSesionCaja(sesion);
        ingresoCaja.setFechaHora(ahora);
        ingresoCaja.setSentido(SentidoMovimiento.INGRESO);
        ingresoCaja.setOrigen(OrigenMovimiento.LIQUIDACION_MOTORIZADO);
        ingresoCaja.setCanalFondos(CanalFondos.CAJA_FISICA);
        ingresoCaja.setMonto(montoLiquidado);
        ingresoCaja.setUsuarioResponsable(usuarioCaja);
        ingresoCaja.setMovimientoOriginal(egresoGuardado);
        ingresoCaja.setReferencia(referencia);
        ingresoCaja.setDescripcion(descripcionLiquidacion("Ingreso a Caja por liquidación de motorizado",
                request.observacion()));
        MovimientoCaja ingresoGuardado = movimientoCajaRepository.saveAndFlush(ingresoCaja);

        return new CajaDTO.LiquidacionResponse(
                egresoGuardado.getId(),
                ingresoGuardado.getId(),
                sesion.getId(),
                empleadoCustodio.getId(),
                montoLiquidado,
                saldoAnterior,
                saldoAnterior.subtract(montoLiquidado).setScale(2, RoundingMode.UNNECESSARY),
                ahora,
                referencia);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaDTO.CustodiaPendienteResponse> listarCustodiasPendientes() {
        return movimientoCajaRepository.listarCustodiasPendientes(
                CanalFondos.CUSTODIA_MOTORIZADO,
                SentidoMovimiento.INGRESO,
                SentidoMovimiento.EGRESO);
    }

    @Override
    @Transactional(readOnly = true)
    public CajaDTO.EstadoResponse obtenerEstadoCaja() {
        Caja caja = cajaRepository.findByCodigo(CODIGO_CAJA_PRINCIPAL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La Caja principal no esta configurada"));
        SesionCaja sesionAbierta = sesionCajaRepository.findByCajaAndEstado(caja, EstadoSesionCaja.ABIERTA)
                .orElse(null);
        BigDecimal efectivoEsperado = sesionAbierta != null
                ? normalizarMonto(movimientoCajaRepository.calcularSaldoPorSesionYCanal(
                        sesionAbierta,
                        CanalFondos.CAJA_FISICA,
                        SentidoMovimiento.INGRESO,
                        SentidoMovimiento.EGRESO))
                : null;

        return new CajaDTO.EstadoResponse(
                caja.getCodigo(),
                caja.getActiva(),
                sesionAbierta != null,
                sesionAbierta != null ? sesionAbierta.getId() : null,
                sesionAbierta != null ? sesionAbierta.getFechaHoraApertura() : null,
                efectivoEsperado);
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

    @Override
    @Transactional
    public void registrarIngresosVentaDomicilio(List<PedidoPago> pagos, Long usuarioResponsableId) {
        if (pagos == null || pagos.isEmpty() || usuarioResponsableId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los pagos y el usuario responsable son obligatorios");
        }

        List<Long> idsPagos = new ArrayList<>();
        Set<Long> idsUnicos = new HashSet<>();
        for (PedidoPago pago : pagos) {
            if (pago == null || pago.getId() == null || !idsUnicos.add(pago.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Los pagos de venta domicilio deben tener IDs únicos");
            }
            idsPagos.add(pago.getId());
        }
        idsPagos.sort(Long::compareTo);

        Caja caja = cajaRepository.findByCodigo(CODIGO_CAJA_PRINCIPAL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La Caja principal no esta configurada"));
        if (!Boolean.TRUE.equals(caja.getActiva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Caja principal esta inactiva");
        }

        List<PedidoPago> pagosBloqueados = pedidoPagoRepository.findAllByIdInForUpdate(idsPagos);
        if (pagosBloqueados.size() != idsPagos.size()
                || pagosBloqueados.stream().map(PedidoPago::getId).anyMatch(id -> !idsUnicos.contains(id))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Uno o más pagos de venta domicilio no existen");
        }

        Long idPedido = null;
        for (PedidoPago pago : pagosBloqueados) {
            validarPagoVentaDomicilio(pago);
            if (idPedido == null) {
                idPedido = pago.getPedido().getId();
            } else if (!idPedido.equals(pago.getPedido().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Los pagos de venta domicilio deben pertenecer al mismo pedido");
            }
        }

        Empleado empleadoCustodioBloqueado = null;
        for (PedidoPago pago : pagosBloqueados) {
            if (pago.getMetodoPago().getTipoFinanciero() == TipoFinancieroMetodoPago.EFECTIVO) {
                empleadoCustodioBloqueado = empleadoRepository.findByIdForUpdate(pago.getPedido().getEmpleado().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
                break;
            }
        }

        Usuario usuarioResponsable = usuarioRepository.findById(usuarioResponsableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        for (PedidoPago pago : pagosBloqueados) {
            if (movimientoCajaRepository.findByPedidoPago(pago).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El pago de la venta domicilio ya tiene un movimiento de Caja");
            }
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (PedidoPago pago : pagosBloqueados) {
            Pedido pedido = pago.getPedido();
            TipoFinancieroMetodoPago tipoFinanciero = pago.getMetodoPago().getTipoFinanciero();
            MovimientoCaja movimiento = new MovimientoCaja();
            movimiento.setCaja(caja);
            movimiento.setSesionCaja(null);
            movimiento.setFechaHora(ahora);
            movimiento.setSentido(SentidoMovimiento.INGRESO);
            movimiento.setOrigen(OrigenMovimiento.VENTA);
            movimiento.setMonto(pago.getMonto());
            movimiento.setMetodoPago(pago.getMetodoPago());
            movimiento.setPedidoPago(pago);
            movimiento.setUsuarioResponsable(usuarioResponsable);
            movimiento.setReferencia(pedido.getCodigo());

            if (tipoFinanciero == TipoFinancieroMetodoPago.EFECTIVO) {
                movimiento.setCanalFondos(CanalFondos.CUSTODIA_MOTORIZADO);
                movimiento.setEmpleadoCustodio(empleadoCustodioBloqueado);
                movimiento.setDescripcion("Ingreso por venta domicilio en custodia " + pedido.getCodigo());
            } else if (tipoFinanciero == TipoFinancieroMetodoPago.DIGITAL) {
                movimiento.setCanalFondos(CanalFondos.DIGITAL_NEGOCIO);
                movimiento.setEmpleadoCustodio(null);
                movimiento.setDescripcion("Ingreso digital por venta domicilio " + pedido.getCodigo());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El pago de venta domicilio no tiene un tipo financiero válido");
            }
            try {
                movimientoCajaRepository.saveAndFlush(movimiento);
            } catch (DataIntegrityViolationException exception) {
                if (esViolacionMovimientoCajaPedidoPagoUnico(exception)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "El pago de la venta domicilio ya tiene un movimiento de Caja");
                }
                throw exception;
            }
        }
    }

    private boolean esViolacionMovimientoCajaPedidoPagoUnico(DataIntegrityViolationException exception) {
        Throwable actual = exception;
        while (actual != null) {
            if (actual instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && UK_MOVIMIENTO_CAJA_PEDIDO_PAGO.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            if (actual.getMessage() != null
                    && actual.getMessage().contains(UK_MOVIMIENTO_CAJA_PEDIDO_PAGO)) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
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

    private void validarPagoVentaDomicilio(PedidoPago pago) {
        if (pago == null || pago.getId() == null || pago.getPedido() == null || pago.getPedido().getId() == null
                || pago.getMetodoPago() == null || pago.getMetodoPago().getTipoFinanciero() == null
                || pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0
                || pago.getPedido().getEmpleado() == null || pago.getPedido().getEmpleado().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El pago de venta domicilio no tiene datos financieros válidos");
        }
        if (!"DOMICILIO".equalsIgnoreCase(pago.getPedido().getTipoVenta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo los pagos de ventas a domicilio pueden ingresar a custodia");
        }
        TipoFinancieroMetodoPago tipoFinanciero = pago.getMetodoPago().getTipoFinanciero();
        if (tipoFinanciero != TipoFinancieroMetodoPago.EFECTIVO
                && tipoFinanciero != TipoFinancieroMetodoPago.DIGITAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El pago de venta domicilio no tiene un tipo financiero válido");
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

    private BigDecimal normalizarMontoInicial(BigDecimal montoInicial) {
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto inicial debe ser mayor o igual a cero");
        }
        if (montoInicial.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto inicial debe tener como maximo 2 decimales");
        }

        try {
            return montoInicial.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto inicial debe tener como maximo 2 decimales");
        }
    }

    private BigDecimal normalizarMontoLiquidacion(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto de liquidación debe ser mayor a cero");
        }
        if (monto.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto de liquidación debe tener como maximo 2 decimales");
        }

        try {
            return monto.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto de liquidación debe tener como maximo 2 decimales");
        }
    }

    private BigDecimal normalizarMonto(BigDecimal monto) {
        BigDecimal montoSeguro = monto != null ? monto : BigDecimal.ZERO;
        return montoSeguro.setScale(2, RoundingMode.UNNECESSARY);
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private String descripcionLiquidacion(String descripcionBase, String observacion) {
        if (!tieneTexto(observacion)) {
            return descripcionBase;
        }
        return descripcionBase + ": " + observacion.trim();
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
