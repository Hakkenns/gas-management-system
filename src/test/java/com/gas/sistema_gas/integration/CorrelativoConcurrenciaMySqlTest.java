package com.gas.sistema_gas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.gas.sistema_gas.SistemaGasApplication;
import com.gas.sistema_gas.Model.Correlativo;
import com.gas.sistema_gas.Repository.CorrelativoRepository;
import com.gas.sistema_gas.service.CorrelativoService;

@SpringBootTest(classes = SistemaGasApplication.class)
@ContextConfiguration(initializers = InventarioConcurrenciaMySqlTest.TestDbInitializer.class)
class CorrelativoConcurrenciaMySqlTest {

    private static final String TIPO = "VENTA_NOTA";
    private static final String SERIE = "NV001";

    @Autowired
    private CorrelativoService correlativoService;

    @Autowired
    private CorrelativoRepository correlativoRepository;

    private String obtenerCodigo(Future<String> future) {
        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("Timeout esperando el incremento concurrente", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            String mensajes = cadenaMensajes(causa).toLowerCase(Locale.ROOT);
            if (mensajes.contains("deadlock")) {
                throw new AssertionError("Deadlock durante el incremento concurrente", causa);
            }
            if (mensajes.contains("lock wait timeout")
                    || mensajes.contains("cannot acquire lock")
                    || mensajes.contains("could not obtain lock")) {
                throw new AssertionError("Lock timeout durante el incremento concurrente", causa);
            }
            throw new AssertionError("Falló una operación concurrente", causa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupción esperando el incremento concurrente", e);
        }
    }

    private String cadenaMensajes(Throwable error) {
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
    void dosIncrementosSimultaneos_mismaSerie_generanConsecutivosSinDuplicados() throws Exception {
        correlativoRepository.deleteAllInBatch();

        Correlativo correlativo = new Correlativo();
        correlativo.setTipo(TIPO);
        correlativo.setSerie(SERIE);
        correlativo.setNumeroActual(0);
        correlativoRepository.saveAndFlush(correlativo);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        String codigoPrimero;
        String codigoSegundo;
        try {
            Future<String> primero = executor.submit(() -> {
                barrier.await();
                return correlativoService.incrementarYObtenerCodigo(TIPO, SERIE);
            });
            Future<String> segundo = executor.submit(() -> {
                barrier.await();
                return correlativoService.incrementarYObtenerCodigo(TIPO, SERIE);
            });

            codigoPrimero = obtenerCodigo(primero);
            codigoSegundo = obtenerCodigo(segundo);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "El executor concurrente no terminó");
        }

        assertNotNull(codigoPrimero, "La primera operación debe devolver un código");
        assertNotNull(codigoSegundo, "La segunda operación debe devolver un código");
        assertNotEquals(codigoPrimero, codigoSegundo, "Los códigos no deben duplicarse");

        Set<String> codigos = new HashSet<>(List.of(codigoPrimero, codigoSegundo));
        assertEquals(2, codigos.size(), "Ningún código debe estar duplicado");
        assertEquals(Set.of("NV001-0001", "NV001-0002"), codigos,
                "Los incrementos deben ser consecutivos");

        Correlativo actualizado = correlativoRepository.findByTipoAndSerie(TIPO, SERIE)
                .orElseThrow();
        assertEquals(2, actualizado.getNumeroActual(), "numeroActual final debe ser 2");

        long filas = correlativoRepository.findAll().stream()
                .filter(item -> TIPO.equals(item.getTipo()) && SERIE.equals(item.getSerie()))
                .count();
        assertEquals(1L, filas, "Debe existir una sola fila para tipo y serie");
    }
}
