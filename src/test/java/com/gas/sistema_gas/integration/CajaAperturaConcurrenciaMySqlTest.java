package com.gas.sistema_gas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.SistemaGasApplication;
import com.gas.sistema_gas.Model.Caja;
import com.gas.sistema_gas.Model.CanalFondos;
import com.gas.sistema_gas.Model.EstadoSesionCaja;
import com.gas.sistema_gas.Model.MovimientoCaja;
import com.gas.sistema_gas.Model.OrigenMovimiento;
import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.Model.SentidoMovimiento;
import com.gas.sistema_gas.Model.SesionCaja;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.CajaRepository;
import com.gas.sistema_gas.Repository.MovimientoCajaRepository;
import com.gas.sistema_gas.Repository.PerfilRepository;
import com.gas.sistema_gas.Repository.SesionCajaRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.CajaDTO;
import com.gas.sistema_gas.service.CajaService;
import com.gas.sistema_gas.service.Implement.CajaServiceImplement;

@SpringBootTest(classes = SistemaGasApplication.class)
@ContextConfiguration(initializers = InventarioConcurrenciaMySqlTest.TestDbInitializer.class)
class CajaAperturaConcurrenciaMySqlTest {

    private static final String DB_TARGET = "sistema_gas_concurrency_test";

    @Autowired
    private CajaService cajaService;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private SesionCajaRepository sesionCajaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long usuarioId;

    @BeforeEach
    void prepararBaseDePrueba() {
        assertEquals(DB_TARGET, jdbcTemplate.queryForObject("SELECT DATABASE()", String.class),
                "La apertura concurrente debe ejecutarse solo en la BD de integración");
        aplicarEsquemaCajaEnBaseDePrueba();
        jdbcTemplate.execute("DELETE FROM movimiento_caja");
        jdbcTemplate.execute("DELETE FROM sesion_caja");

        Perfil perfil = new Perfil();
        perfil.setNombrePerfil("Perfil Caja Test");
        perfil.setDescripcion("Perfil exclusivo para prueba de Caja");
        perfil.setEstado(1);
        perfil = perfilRepository.saveAndFlush(perfil);

        Usuario usuario = new Usuario();
        String sufijo = Long.toUnsignedString(System.nanoTime());
        usuario.setUserName("caja.concurrente." + sufijo);
        usuario.setPassword("hash-prueba");
        usuario.setCorreo("caja.concurrente." + sufijo + "@test.com");
        usuario.setEstado(1);
        usuario.setPerfil(perfil);
        usuarioId = usuarioRepository.saveAndFlush(usuario).getId();
    }

    private void aplicarEsquemaCajaEnBaseDePrueba() {
        try {
            ClassPathResource recurso = new ClassPathResource(
                    "sql/actualizaciones/2026-08-13_caja_sesion_apertura.sql");
            String sql = new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            for (String sentencia : sql.split(";")) {
                if (!sentencia.isBlank()) {
                    jdbcTemplate.execute(sentencia);
                }
            }
        } catch (Exception ex) {
            throw new AssertionError("No se pudo aplicar el SQL versionado de Caja en la BD de integración", ex);
        }
    }

    private <T> ResultadoConcurrente<T> resolver(Future<T> future) {
        try {
            return new ResultadoConcurrente(future.get(20, TimeUnit.SECONDS), null);
        } catch (TimeoutException e) {
            throw new AssertionError("Timeout esperando la apertura concurrente", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            assertSinErrorDeLocking(causa);
            return new ResultadoConcurrente(null, causa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupción esperando la apertura concurrente", e);
        }
    }

    private void assertSinErrorDeLocking(Throwable error) {
        String mensajes = mensajes(error).toLowerCase(Locale.ROOT);
        assertFalse(mensajes.contains("deadlock"), "No debe ocurrir deadlock: " + mensajes);
        assertFalse(mensajes.contains("lock wait timeout"), "No debe ocurrir lock timeout: " + mensajes);
        assertFalse(mensajes.contains("could not obtain lock"), "No debe fallar por locking: " + mensajes);
    }

    private String mensajes(Throwable error) {
        StringBuilder mensajes = new StringBuilder();
        Throwable actual = error;
        while (actual != null) {
            if (actual.getMessage() != null) {
                mensajes.append(' ').append(actual.getMessage());
            }
            actual = actual.getCause();
        }
        return mensajes.toString();
    }

    @Test
    @Timeout(30)
    void dosAperturasSimultaneas_cajaPrincipal_permiteExactamenteUna() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        ResultadoConcurrente<CajaDTO.AperturaResponse> primero;
        ResultadoConcurrente<CajaDTO.AperturaResponse> segundo;
        try {
            Future<CajaDTO.AperturaResponse> futurePrimero = executor.submit(() -> {
                barrier.await();
                return cajaService.abrirCaja(usuarioId, new BigDecimal("100.00"), "Apertura concurrente");
            });
            Future<CajaDTO.AperturaResponse> futureSegundo = executor.submit(() -> {
                barrier.await();
                return cajaService.abrirCaja(usuarioId, new BigDecimal("100.00"), "Apertura concurrente");
            });

            primero = resolver(futurePrimero);
            segundo = resolver(futureSegundo);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "El executor concurrente no terminó");
        }

        long exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        long errores = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);
        assertEquals(1L, exitos, "Exactamente una apertura debe tener éxito");
        assertEquals(1L, errores, "Exactamente una apertura debe rechazarse");

        Throwable rechazo = primero.error() != null ? primero.error() : segundo.error();
        assertTrue(rechazo instanceof ResponseStatusException,
                "La apertura rechazada debe ser ResponseStatusException");
        assertEquals(HttpStatus.CONFLICT, ((ResponseStatusException) rechazo).getStatusCode());

        Caja caja = cajaRepository.findByCodigo(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL).orElseThrow();
        assertEquals(1L, sesionCajaRepository.countByCajaAndEstado(caja, EstadoSesionCaja.ABIERTA),
                "Debe existir exactamente una sesión ABIERTA");
        assertEquals(1L, movimientoCajaRepository.count(), "Debe existir exactamente un movimiento APERTURA");
        assertNotNull(primero.respuesta() != null ? primero.respuesta() : segundo.respuesta(),
                "La apertura exitosa debe devolver respuesta");
    }

    @Test
    @Timeout(30)
    void cierreCaja_calculaSoloEfectivoFisicoYGuardaSnapshotsReales() {
        CajaDTO.AperturaResponse apertura = cajaService.abrirCaja(usuarioId, new BigDecimal("100.00"), "Fondo inicial");
        SesionCaja sesion = sesionCajaRepository.findById(apertura.idSesionCaja()).orElseThrow();
        Caja caja = cajaRepository.findByCodigo(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL).orElseThrow();
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();

        guardarMovimiento(caja, sesion, usuario, SentidoMovimiento.INGRESO, OrigenMovimiento.VENTA,
                CanalFondos.CAJA_FISICA, "50.00");
        guardarMovimiento(caja, sesion, usuario, SentidoMovimiento.EGRESO, OrigenMovimiento.AJUSTE,
                CanalFondos.CAJA_FISICA, "20.00");
        guardarMovimiento(caja, sesion, usuario, SentidoMovimiento.INGRESO, OrigenMovimiento.VENTA,
                CanalFondos.DIGITAL_NEGOCIO, "30.00");
        guardarMovimiento(caja, sesion, usuario, SentidoMovimiento.INGRESO, OrigenMovimiento.VENTA,
                CanalFondos.CUSTODIA_MOTORIZADO, "20.00");

        CajaDTO.CierreResponse cierre = cajaService.cerrarCaja(usuarioId, new BigDecimal("130.00"), null);

        SesionCaja sesionCerrada = sesionCajaRepository.findById(apertura.idSesionCaja()).orElseThrow();
        assertEquals("CERRADA", cierre.estado());
        assertEquals(EstadoSesionCaja.CERRADA, sesionCerrada.getEstado());
        assertEquals(0, sesionCerrada.getMontoEsperadoCierre().compareTo(new BigDecimal("130.00")));
        assertEquals(0, sesionCerrada.getMontoDeclaradoCierre().compareTo(new BigDecimal("130.00")));
        assertEquals(0, sesionCerrada.getDiferenciaCierre().compareTo(BigDecimal.ZERO));
        assertNotNull(sesionCerrada.getUsuarioCierre());
        assertNotNull(sesionCerrada.getFechaHoraCierre());
    }

    @Test
    @Timeout(30)
    void dosCierresSimultaneos_cajaPrincipal_permiteExactamenteUno() throws Exception {
        CajaDTO.AperturaResponse apertura = cajaService.abrirCaja(usuarioId, new BigDecimal("100.00"), "Fondo inicial");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        ResultadoConcurrente<CajaDTO.CierreResponse> primero;
        ResultadoConcurrente<CajaDTO.CierreResponse> segundo;
        try {
            Future<CajaDTO.CierreResponse> futurePrimero = executor.submit(() -> {
                barrier.await();
                return cajaService.cerrarCaja(usuarioId, new BigDecimal("100.00"), null);
            });
            Future<CajaDTO.CierreResponse> futureSegundo = executor.submit(() -> {
                barrier.await();
                return cajaService.cerrarCaja(usuarioId, new BigDecimal("100.00"), null);
            });

            primero = resolver(futurePrimero);
            segundo = resolver(futureSegundo);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "El executor concurrente no termino");
        }

        long exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        long errores = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);
        assertEquals(1L, exitos, "Exactamente un cierre debe tener exito");
        assertEquals(1L, errores, "Exactamente un cierre debe rechazarse");

        Throwable rechazo = primero.error() != null ? primero.error() : segundo.error();
        assertTrue(rechazo instanceof ResponseStatusException,
                "El cierre rechazado debe ser ResponseStatusException");
        assertEquals(HttpStatus.CONFLICT, ((ResponseStatusException) rechazo).getStatusCode());

        Caja caja = cajaRepository.findByCodigo(CajaServiceImplement.CODIGO_CAJA_PRINCIPAL).orElseThrow();
        SesionCaja sesionCerrada = sesionCajaRepository.findById(apertura.idSesionCaja()).orElseThrow();
        assertEquals(0L, sesionCajaRepository.countByCajaAndEstado(caja, EstadoSesionCaja.ABIERTA));
        assertEquals(1L, sesionCajaRepository.countByCajaAndEstado(caja, EstadoSesionCaja.CERRADA),
                "Debe existir exactamente una sesión CERRADA");
        assertEquals(EstadoSesionCaja.CERRADA, sesionCerrada.getEstado());
        assertEquals(0, sesionCerrada.getMontoEsperadoCierre().compareTo(new BigDecimal("100.00")));
        assertEquals(0, sesionCerrada.getMontoDeclaradoCierre().compareTo(new BigDecimal("100.00")));
        assertEquals(0, sesionCerrada.getDiferenciaCierre().compareTo(BigDecimal.ZERO));
        assertNotNull(sesionCerrada.getFechaHoraCierre());
        assertNotNull(sesionCerrada.getUsuarioCierre());
    }

    private void guardarMovimiento(Caja caja, SesionCaja sesion, Usuario usuario, SentidoMovimiento sentido,
            OrigenMovimiento origen, CanalFondos canalFondos, String monto) {
        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setCaja(caja);
        movimiento.setSesionCaja(sesion);
        movimiento.setFechaHora(java.time.LocalDateTime.now());
        movimiento.setSentido(sentido);
        movimiento.setOrigen(origen);
        movimiento.setCanalFondos(canalFondos);
        movimiento.setMonto(new BigDecimal(monto));
        movimiento.setUsuarioResponsable(usuario);
        movimiento.setDescripcion("Movimiento de prueba para cierre de Caja");
        movimientoCajaRepository.saveAndFlush(movimiento);
    }

    private record ResultadoConcurrente<T>(T respuesta, Throwable error) {
        boolean exitoso() {
            return respuesta != null;
        }
    }
}
