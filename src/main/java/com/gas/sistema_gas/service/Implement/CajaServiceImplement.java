package com.gas.sistema_gas.service.Implement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Model.Caja;
import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.EstadoSesionCaja;
import com.gas.sistema_gas.Model.MovimientoCaja;
import com.gas.sistema_gas.Model.OrigenMovimiento;
import com.gas.sistema_gas.Model.SentidoMovimiento;
import com.gas.sistema_gas.Model.SesionCaja;
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
}
