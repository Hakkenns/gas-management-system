package com.gas.sistema_gas.integration;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.SistemaGasApplication;
import com.gas.sistema_gas.Model.AsignacionLotePedido;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.Compra;
import com.gas.sistema_gas.Model.ControlEnvase;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Model.Rubro;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.AsignacionLotePedidoRepository;
import com.gas.sistema_gas.Repository.CompraRepository;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.service.CompraService;
import com.gas.sistema_gas.service.CorrelativoService;
import com.gas.sistema_gas.service.InventarioLoteService;
import com.gas.sistema_gas.service.PedidoService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = SistemaGasApplication.class)
@ContextConfiguration(initializers = InventarioConcurrenciaMySqlTest.TestDbInitializer.class)
@Import(InventarioConcurrenciaMySqlTest.TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InventarioConcurrenciaMySqlTest {

    private static final String DB_SOURCE = "sistema_gas";
    private static final String DB_TARGET = "sistema_gas_concurrency_test";
    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3307/" + DB_SOURCE
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "";

    public static class TestDbInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            if (DB_SOURCE == null || DB_SOURCE.isBlank()) {
                throw new IllegalStateException("DB_SOURCE no puede ser null ni blank");
            }
            if (DB_TARGET == null || DB_TARGET.isBlank()) {
                throw new IllegalStateException("DB_TARGET no puede ser null ni blank");
            }
            if (DB_SOURCE.equals(DB_TARGET)) {
                throw new IllegalStateException("DB_SOURCE y DB_TARGET no pueden ser iguales");
            }
            if (!DB_TARGET.endsWith("_concurrency_test")) {
                throw new IllegalStateException("Destino inválido: " + DB_TARGET);
            }

            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
                 Statement stmt = conn.createStatement()) {

                ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-datasource", Map.ofEntries(
                        Map.entry("spring.datasource.url",
                                "jdbc:mysql://127.0.0.1:3307/" + DB_TARGET
                                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima"),
                        Map.entry("spring.datasource.username", "root"),
                        Map.entry("spring.datasource.password", ""),
                        Map.entry("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver"),
                        Map.entry("spring.jpa.hibernate.ddl-auto", "none"),
                        Map.entry("spring.jpa.show-sql", "false"),
                        Map.entry("spring.jpa.open-in-view", "false"),
                        Map.entry("spring.datasource.hikari.maximum-pool-size", "8"),
                        Map.entry("spring.datasource.hikari.minimum-idle", "2"),
                        Map.entry("spring.datasource.hikari.connection-timeout", "10000")
                )));

                ResultSet rs = stmt.executeQuery("SELECT DATABASE()");
                rs.next();
                String db = rs.getString(1);
                if (!DB_SOURCE.equals(db)) {
                    throw new IllegalStateException("Fuente inesperada: " + db);
                }

                stmt.execute("DROP DATABASE IF EXISTS " + DB_TARGET);
                stmt.execute("CREATE DATABASE " + DB_TARGET
                        + " CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");

                ResultSet tables = stmt.executeQuery(
                        "SELECT TABLE_NAME FROM information_schema.TABLES "
                                + "WHERE TABLE_SCHEMA = '" + DB_SOURCE + "' "
                                + "AND TABLE_TYPE = 'BASE TABLE' "
                                + "ORDER BY TABLE_NAME");
                List<String> tableNames = new ArrayList<>();
                while (tables.next()) {
                    tableNames.add(tables.getString("TABLE_NAME"));
                }
                tables.close();

                for (String t : tableNames) {
                    stmt.execute("CREATE TABLE " + DB_TARGET + ".`" + t + "` LIKE " + DB_SOURCE + ".`" + t + "`");
                }
            } catch (Exception ex) {
                throw new IllegalStateException("No se pudo inicializar la base de datos de prueba", ex);
            }
        }
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        @Primary
        public CorrelativoService correlativoService() {
            return new CorrelativoService() {
                private final AtomicLong seq = new AtomicLong(0);

                @Override
                public String previsualizarCodigoSiguiente(String tipo, String serie) {
                    return String.format("%s-%04d", serie, seq.get() + 1);
                }

                @Override
                public String incrementarYObtenerCodigo(String tipo, String serie) {
                    long next = seq.incrementAndGet();
                    return String.format("%s-%04d", serie, next);
                }
            };
        }
    }

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InventarioLoteRepository inventarioLoteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ControlEnvaseRepository controlEnvaseRepository;

    @Autowired
    private AsignacionLotePedidoRepository asignacionLotePedidoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CompraService compraService;

    @Autowired
    private InventarioLoteService inventarioLoteService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private Long idProducto;
    private Long idProducto2;
    private Long idUsuario;
    private Long idEmpleado;
    private Long idCompra;
    private Long idCliente;

    private void truncateAll() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet database = statement.executeQuery("SELECT DATABASE()")) {
                    assertTrue(database.next(), "SELECT DATABASE() no devolvió resultados");
                    String db = database.getString(1);
                    assertEquals(DB_TARGET, db, "Base de datos activa incorrecta: " + db);
                }

                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                try {
                    List<String> tables = new ArrayList<>();
                    try (ResultSet resultSet = statement.executeQuery(
                            "SELECT TABLE_NAME FROM information_schema.TABLES "
                                    + "WHERE TABLE_SCHEMA = '" + DB_TARGET + "' "
                                    + "AND TABLE_TYPE = 'BASE TABLE' "
                                    + "ORDER BY TABLE_NAME")) {
                        while (resultSet.next()) {
                            tables.add(resultSet.getString("TABLE_NAME"));
                        }
                    }
                    for (String table : tables) {
                        statement.execute("TRUNCATE TABLE `" + table + "`");
                    }
                } finally {
                    statement.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            }
            return null;
        });
    }

    private void ensureRubroCategoriaProveedor() {
        transactionTemplate.execute(status -> {
            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro Test");
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Categoria categoria = new Categoria();
            categoria.setNombre("Cat Test");
            categoria.setEstado(1);
            entityManager.persist(categoria);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Prov Test");
            proveedor.setTelefono("999999999");
            proveedor.setRuc("99999999999");
            proveedor.setRubro(rubro);
            proveedor.setEstado(1);
            entityManager.persist(proveedor);

            return null;
        });
    }

    private Long ensureProducto(BigDecimal stockLlenos, BigDecimal stockReservado) {
        return transactionTemplate.execute(status -> {
            Rubro rubro = entityManager.createQuery("FROM Rubro", Rubro.class).getResultList().get(0);
            Categoria categoria = entityManager.createQuery("FROM Categoria", Categoria.class).getResultList().get(0);

            Producto p = new Producto();
            p.setNombre("Producto Test");
            p.setPrecioCompra(new BigDecimal("10.00"));
            p.setGananciaProducto(BigDecimal.ZERO);
            p.setPrecioVenta(new BigDecimal("10.00"));
            p.setStockLlenos(stockLlenos);
            p.setStockMinimo(BigDecimal.ZERO);
            p.setStockVacios(0);
            p.setStockReservado(stockReservado);
            p.setRequiereEnvase(false);
            p.setEstado(1);
            p.setCategoria(categoria);
            entityManager.persist(p);
            return p.getId();
        });
    }

    private Long ensureProducto2(BigDecimal stockLlenos, BigDecimal stockReservado) {
        return transactionTemplate.execute(status -> {
            Rubro rubro = entityManager.createQuery("FROM Rubro", Rubro.class).getResultList().get(0);
            Categoria categoria = entityManager.createQuery("FROM Categoria", Categoria.class).getResultList().get(0);

            Producto p = new Producto();
            p.setNombre("Producto Test 2");
            p.setPrecioCompra(new BigDecimal("10.00"));
            p.setGananciaProducto(BigDecimal.ZERO);
            p.setPrecioVenta(new BigDecimal("10.00"));
            p.setStockLlenos(stockLlenos);
            p.setStockMinimo(BigDecimal.ZERO);
            p.setStockVacios(0);
            p.setStockReservado(stockReservado);
            p.setRequiereEnvase(false);
            p.setEstado(1);
            p.setCategoria(categoria);
            entityManager.persist(p);
            return p.getId();
        });
    }

    private Long ensureUsuario() {
        return transactionTemplate.execute(status -> {
            Perfil perfil = new Perfil();
            perfil.setNombrePerfil("Perfil Test");
            perfil.setEstado(1);
            entityManager.persist(perfil);

            Usuario u = new Usuario();
            u.setUserName("Usuario Test");
            u.setPassword("password");
            u.setCorreo("test@test.com");
            u.setEstado(1);
            u.setPerfil(perfil);
            entityManager.persist(u);
            return u.getId();
        });
    }

    private Long ensureEmpleado() {
        return transactionTemplate.execute(status -> {
            Empleado e = new Empleado();
            e.setNombre("Motorizado Test");
            e.setDni("99999999");
            e.setTelefono("999999999");
            e.setCorreo("motorizado.test@gmail.com");
            e.setEstado(1);
            entityManager.persist(e);
            return e.getId();
        });
    }

    private Long ensureCompra() {
        return transactionTemplate.execute(status -> {
            Proveedor proveedor = entityManager.createQuery("FROM Proveedor", Proveedor.class).getResultList().get(0);
            Usuario usuario = entityManager.createQuery("FROM Usuario", Usuario.class).getResultList().get(0);

            Compra c = new Compra();
            c.setProveedor(proveedor);
            c.setUsuario(usuario);
            c.setFechaCompra(LocalDateTime.now());
            c.setMontoTotal(new BigDecimal("100.00"));
            c.setSituacion(1);
            entityManager.persist(c);
            return c.getId();
        });
    }

    private void ensureLote(BigDecimal cantidadInicial, BigDecimal cantidadActual) {
        transactionTemplate.execute(status -> {
            Producto producto = entityManager.find(Producto.class, idProducto);
            Proveedor proveedor = entityManager.createQuery("FROM Proveedor", Proveedor.class).getResultList().get(0);
            Compra compra = entityManager.find(Compra.class, idCompra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(cantidadInicial);
            lote.setCantidadActual(cantidadActual);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
            return null;
        });
    }

    private void ensureLoteParaProducto(Long idProductoDestino, BigDecimal cantidadInicial, BigDecimal cantidadActual) {
        transactionTemplate.execute(status -> {
            Producto producto = entityManager.find(Producto.class, idProductoDestino);
            Proveedor proveedor = entityManager.createQuery("FROM Proveedor", Proveedor.class).getResultList().get(0);
            Compra compra = entityManager.find(Compra.class, idCompra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(cantidadInicial);
            lote.setCantidadActual(cantidadActual);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
            return null;
        });
    }

    private Long ensureCliente() {
        return transactionTemplate.execute(status -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente Concurrencia");
            cliente.setDni("99999999");
            cliente.setTelefono("999999999");
            cliente.setDireccion("Dirección de prueba");
            cliente.setReferencia("Referencia de prueba");
            cliente.setEstado(1);
            entityManager.persist(cliente);
            return cliente.getId();
        });
    }

    private record ResultadoConcurrente<T>(T valor, Throwable error) {
        boolean exitoso() {
            return error == null;
        }
    }

    private record ParResultados<T>(
            ResultadoConcurrente<T> primero,
            ResultadoConcurrente<T> segundo
    ) {}

    private <T> ResultadoConcurrente<T> obtenerResultado(Future<T> future) {
        try {
            return new ResultadoConcurrente<>(future.get(20, TimeUnit.SECONDS), null);
        } catch (TimeoutException e) {
            throw new AssertionError("Timeout esperando resultado concurrente", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            return new ResultadoConcurrente<>(null, causa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupción esperando resultado concurrente", e);
        }
    }

    private <T> ParResultados<T> ejecutarConcurrente(
            Callable<T> op1,
            Callable<T> op2) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Future<T> f1 = executor.submit(() -> { barrier.await(); return op1.call(); });
        Future<T> f2 = executor.submit(() -> { barrier.await(); return op2.call(); });

        try {
            ResultadoConcurrente<T> r1 = obtenerResultado(f1);
            ResultadoConcurrente<T> r2 = obtenerResultado(f2);
            return new ParResultados<>(r1, r2);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void assertNoLockingError(Throwable error) {
        if (error == null) {
            return;
        }
        Throwable cur = error;
        while (cur != null) {
            String name = cur.getClass().getName();
            if (cur instanceof TimeoutException
                    || name.contains("CannotAcquireLockException")
                    || name.contains("PessimisticLockingFailureException")
                    || name.contains("LockAcquisitionException")
                    || name.contains("SQLTransactionRollbackException")) {
                fail("Error de bloqueo detectado: " + name, cur);
            }
            String msg = cur.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("deadlock") || lower.contains("lock wait timeout")) {
                    fail("Error de bloqueo detectado: " + msg, cur);
                }
            }
            cur = cur.getCause();
        }
    }

    private void assertRechazoStock(Throwable error) {
        assertNotNull(error, "Se esperaba un error en la operación fallida");
        assertNoLockingError(error);
        Throwable cur = error;
        while (cur != null) {
            if (cur instanceof ResponseStatusException rse) {
                assertEquals(HttpStatus.BAD_REQUEST, rse.getStatusCode(),
                        "El rechazo debe ser BAD_REQUEST, no " + rse.getStatusCode());
                String mensaje = rse.getReason() != null ? rse.getReason() : rse.getMessage();
                assertNotNull(mensaje, "El rechazo de stock debe incluir un mensaje");
                assertTrue(mensaje.toLowerCase(Locale.ROOT).contains("stock insuficiente"),
                        "El rechazo debe corresponder a stock insuficiente: " + mensaje);
                return;
            }
            cur = cur.getCause();
        }
        fail("La operación fallida debe contener ResponseStatusException en su cadena de causas", error);
    }

    private void assertRechazoStockEnvases(Throwable error) {
        assertNotNull(error, "Se esperaba un error en la operación fallida");
        assertNoLockingError(error);
        Throwable cur = error;
        while (cur != null) {
            if (cur instanceof ResponseStatusException rse) {
                assertEquals(HttpStatus.BAD_REQUEST, rse.getStatusCode(),
                        "El rechazo debe ser BAD_REQUEST, no " + rse.getStatusCode());
                String mensaje = rse.getReason() != null ? rse.getReason() : rse.getMessage();
                assertNotNull(mensaje, "El rechazo de stock de envases debe incluir un mensaje");
                assertTrue(mensaje.contains("Stock de envases insuficiente"),
                        "El rechazo debe corresponder a stock de envases insuficiente: " + mensaje);
                return;
            }
            cur = cur.getCause();
        }
        fail("La operación fallida debe contener ResponseStatusException en su cadena de causas", error);
    }

    private void assertRechazoConflicto(Throwable error) {
        assertNotNull(error, "Se esperaba un error en la operación fallida");
        assertNoLockingError(error);
        Throwable cur = error;
        while (cur != null) {
            if (cur instanceof ResponseStatusException rse) {
                assertEquals(HttpStatus.CONFLICT, rse.getStatusCode(),
                        "El rechazo debe ser CONFLICT, no " + rse.getStatusCode());
                return;
            }
            cur = cur.getCause();
        }
        fail("La operación fallida debe contener ResponseStatusException en su cadena de causas", error);
    }

    @BeforeEach
    void setUp() {
        truncateAll();
        entityManager.clear();

        ensureRubroCategoriaProveedor();
        idProducto = ensureProducto(new BigDecimal("10.00"), BigDecimal.ZERO);
        idUsuario = ensureUsuario();
        idEmpleado = ensureEmpleado();
        idCompra = ensureCompra();
        ensureLote(new BigDecimal("10.00"), new BigDecimal("10.00"));
        idCliente = ensureCliente();
    }

    private PedidoDTO.Create buildPedido(String tipoVenta, int cantidad) {
        return new PedidoDTO.Create(
                null,
                idCliente,
                null,
                "Cliente Concurrencia",
                "Dirección de prueba",
                "Referencia de prueba",
                "999999999",
                idEmpleado,
                idUsuario,
                null,
                null,
                null,
                null,
                tipoVenta,
                null,
                List.<PedidoDTO.PagoCreate>of(),
                List.of(new PedidoDTO.DetalleCreate(idProducto, cantidad, new BigDecimal("10.00"), 0)),
                "NINGUNO",
                List.<PedidoDTO.EnvaseMovimientoCreate>of()
        );
    }

    private PedidoDTO.Create buildPedidoConDetalles(String tipoVenta, List<PedidoDTO.DetalleCreate> detalles) {
        return new PedidoDTO.Create(
                null,
                idCliente,
                null,
                "Cliente Concurrencia",
                "Dirección de prueba",
                "Referencia de prueba",
                "999999999",
                idEmpleado,
                idUsuario,
                null,
                null,
                null,
                null,
                tipoVenta,
                null,
                List.<PedidoDTO.PagoCreate>of(),
                detalles,
                "NINGUNO",
                List.<PedidoDTO.EnvaseMovimientoCreate>of()
        );
    }

    private PedidoDTO.Create buildPedidoPrestamoEnvase() {
        return new PedidoDTO.Create(
                null,
                idCliente,
                null,
                "Cliente Concurrencia",
                "Dirección de prueba",
                "Referencia de prueba",
                "999999999",
                idEmpleado,
                idUsuario,
                null,
                null,
                null,
                null,
                "DOMICILIO",
                null,
                List.<PedidoDTO.PagoCreate>of(),
                List.of(new PedidoDTO.DetalleCreate(idProducto, 1, new BigDecimal("10.00"), 0)),
                "PRESTAMO",
                List.of(new PedidoDTO.EnvaseMovimientoCreate(idProducto, 1, null, null, null))
        );
    }

    private PedidoDTO.Create buildPedidoVentaEnvase() {
        return new PedidoDTO.Create(
                null,
                idCliente,
                null,
                "Cliente Concurrencia",
                "Dirección de prueba",
                "Referencia de prueba",
                "999999999",
                idEmpleado,
                idUsuario,
                null,
                null,
                null,
                null,
                "DOMICILIO",
                null,
                List.<PedidoDTO.PagoCreate>of(),
                List.of(new PedidoDTO.DetalleCreate(idProducto, 1, new BigDecimal("10.00"), 0)),
                "VENTA",
                List.of(new PedidoDTO.EnvaseMovimientoCreate(idProducto, 1, new BigDecimal("10.00"), null, null))
        );
    }

    @Test
    @Timeout(30)
    void ventaYPrestamoUltimoEnvase_soloUnoDebeTenerExito() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            Producto producto = productoRepository.findById(idProducto).orElseThrow();
            producto.setStockVacios(1);
            producto.setRequiereEnvase(true);
            productoRepository.saveAndFlush(producto);
        });

        PedidoDTO.Create dtoVenta = buildPedidoVentaEnvase();
        PedidoDTO.Create dtoPrestamo = buildPedidoPrestamoEnvase();
        ParResultados<PedidoDTO.SimpleResponse> par = ejecutarConcurrente(
                () -> pedidoService.createOrder(dtoVenta, idUsuario),
                () -> pedidoService.createOrder(dtoPrestamo, idUsuario)
        );

        ResultadoConcurrente<PedidoDTO.SimpleResponse> venta = par.primero();
        ResultadoConcurrente<PedidoDTO.SimpleResponse> prestamo = par.segundo();
        int exitos = (venta.exitoso() ? 1 : 0) + (prestamo.exitoso() ? 1 : 0);
        int rechazos = (venta.exitoso() ? 0 : 1) + (prestamo.exitoso() ? 0 : 1);

        assertEquals(1, exitos, "Debe haber exactamente una operación exitosa");
        assertEquals(1, rechazos, "Debe haber exactamente una operación rechazada");
        assertNoLockingError(venta.error());
        assertNoLockingError(prestamo.error());

        Producto productoFinal = productoRepository.findById(idProducto).orElseThrow();
        assertEquals(0, productoFinal.getStockVacios(), "Debe agotarse el único envase vacío");
        assertTrue(productoFinal.getStockVacios() >= 0, "stockVacios no debe ser negativo");
        assertEquals(new BigDecimal("1.00"), productoFinal.getStockReservado(),
                "Solo debe quedar la reserva del pedido exitoso");
        assertEquals(1, pedidoRepository.count(), "No debe persistir un segundo pedido parcial");

        if (venta.exitoso()) {
            assertRechazoStockEnvases(prestamo.error());
            assertEquals(0, controlEnvaseRepository.count());
            long detallesVenta = transactionTemplate.execute(status -> entityManager.createQuery(
                    "SELECT COUNT(d) FROM DetallePedido d WHERE d.pedido.id = :idPedido", Long.class)
                    .setParameter("idPedido", venta.valor().idPedido())
                    .getSingleResult());
            assertEquals(2, detallesVenta, "La venta exitosa debe tener detalle normal y detalle de envase");
            System.out.println("GANADOR VENTA VS PRESTAMO: VENTA");
        } else {
            assertRechazoStockEnvases(venta.error());
            assertEquals(1, controlEnvaseRepository.count());
            ControlEnvase control = controlEnvaseRepository.findAll().get(0);
            assertEquals(idProducto, control.getProducto().getId());
            assertEquals(1, control.getCantidadPrestada());
            assertEquals(0, control.getCantidadDevuelta());
            assertEquals("PRESTADO", control.getEstado());
            long detallesPrestamo = transactionTemplate.execute(status -> entityManager.createQuery(
                    "SELECT COUNT(d) FROM DetallePedido d WHERE d.pedido.id = :idPedido", Long.class)
                    .setParameter("idPedido", prestamo.valor().idPedido())
                    .getSingleResult());
            assertEquals(1, detallesPrestamo, "El préstamo exitoso debe tener solo el detalle normal");
            System.out.println("GANADOR VENTA VS PRESTAMO: PRESTAMO");
        }
    }

    @Test
    @Timeout(30)
    void dosPrestamosUltimoEnvase_soloUnoDebeTenerExito() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            Producto producto = productoRepository.findById(idProducto).orElseThrow();
            producto.setStockVacios(1);
            producto.setRequiereEnvase(true);
            productoRepository.saveAndFlush(producto);
        });

        PedidoDTO.Create dtoPrestamo = buildPedidoPrestamoEnvase();
        ParResultados<PedidoDTO.SimpleResponse> par = ejecutarConcurrente(
                () -> pedidoService.createOrder(dtoPrestamo, idUsuario),
                () -> pedidoService.createOrder(dtoPrestamo, idUsuario)
        );

        ResultadoConcurrente<PedidoDTO.SimpleResponse> primero = par.primero();
        ResultadoConcurrente<PedidoDTO.SimpleResponse> segundo = par.segundo();
        int exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        int rechazos = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);

        assertEquals(1, exitos, "Debe haber exactamente un préstamo exitoso");
        assertEquals(1, rechazos, "Debe haber exactamente un préstamo rechazado");
        assertNoLockingError(primero.error());
        assertNoLockingError(segundo.error());
        if (!primero.exitoso()) {
            assertRechazoStockEnvases(primero.error());
        }
        if (!segundo.exitoso()) {
            assertRechazoStockEnvases(segundo.error());
        }

        Producto productoFinal = productoRepository.findById(idProducto).orElseThrow();
        assertEquals(0, productoFinal.getStockVacios(), "Debe agotarse el único envase vacío");
        assertTrue(productoFinal.getStockVacios() >= 0, "stockVacios no debe ser negativo");
        assertEquals(new BigDecimal("1.00"), productoFinal.getStockReservado(),
                "Solo debe quedar la reserva del pedido exitoso");

        assertEquals(1, controlEnvaseRepository.count(), "Debe existir un único préstamo de envase");
        assertEquals(1, pedidoRepository.count(), "Debe persistir únicamente el pedido exitoso");

        ControlEnvase control = controlEnvaseRepository.findAll().get(0);
        assertEquals(idProducto, control.getProducto().getId());
        assertEquals(1, control.getCantidadPrestada());
        assertEquals(0, control.getCantidadDevuelta());
        assertEquals("PRESTADO", control.getEstado());
    }

    @Test
    @Timeout(30)
    void dosDomicilio_ultimoStock_noSobreReserva() throws Exception {
        ParResultados<Void> par = ejecutarConcurrente(
                () -> { pedidoService.createOrder(buildPedido("DOMICILIO", 6), idUsuario); return null; },
                () -> { pedidoService.createOrder(buildPedido("DOMICILIO", 6), idUsuario); return null; }
        );

        ResultadoConcurrente<Void> primero = par.primero();
        ResultadoConcurrente<Void> segundo = par.segundo();

        int exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        int errores = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);

        assertEquals(1, exitos, "Debe haber exactamente 1 éxito");
        assertEquals(1, errores, "Debe haber exactamente 1 error");

        assertNoLockingError(primero.error());
        assertNoLockingError(segundo.error());

        if (!primero.exitoso()) {
            assertRechazoStock(primero.error());
        }
        if (!segundo.exitoso()) {
            assertRechazoStock(segundo.error());
        }

        long pedidos = pedidoRepository.count();
        assertEquals(1, pedidos, "Debe persistir exactamente un pedido");

        List<Pedido> todos = pedidoRepository.findAll();
        assertFalse(todos.isEmpty());
        assertEquals("PENDIENTE", todos.get(0).getEstadoPedido(), "El pedido debe estar PENDIENTE");

        Producto producto = productoRepository.findById(idProducto).orElseThrow();
        BigDecimal suma = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
        BigDecimal reservado = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;

        assertEquals(new BigDecimal("10.00"), producto.getStockLlenos(), "stockLlenos no debe cambiar");
        assertEquals(new BigDecimal("10.00"), suma, "lote cantidadActual no debe cambiar");
        assertEquals(new BigDecimal("6.00"), reservado, "Debe reservarse exactamente 6");
        assertEquals(0, suma.subtract(reservado).compareTo(new BigDecimal("4.00")), "Disponible debe ser 4");

        assertTrue(reservado.signum() >= 0, "stockReservado no debe ser negativo");
        assertTrue(suma.signum() >= 0, "cantidadActual no debe ser negativa");

        long asignaciones = asignacionLotePedidoRepository.count();
        assertEquals(0, asignaciones, "No debe crear asignaciones para DOMICILIO");
    }

    @Test
    @Timeout(30)
    void domicilioYLocal_simultaneos_noSobrevenden() throws Exception {
        ParResultados<Void> par = ejecutarConcurrente(
                () -> { pedidoService.createOrder(buildPedido("DOMICILIO", 6), idUsuario); return null; },
                () -> { pedidoService.createOrder(buildPedido("LOCAL", 6), idUsuario); return null; }
        );

        ResultadoConcurrente<Void> primero = par.primero();
        ResultadoConcurrente<Void> segundo = par.segundo();

        int exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        int errores = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);

        assertEquals(1, exitos, "Debe haber exactamente 1 éxito");
        assertEquals(1, errores, "Debe haber exactamente 1 error");

        assertNoLockingError(primero.error());
        assertNoLockingError(segundo.error());

        if (!primero.exitoso()) {
            assertRechazoStock(primero.error());
        }
        if (!segundo.exitoso()) {
            assertRechazoStock(segundo.error());
        }

        long pedidos = pedidoRepository.count();
        assertEquals(1, pedidos, "Debe persistir exactamente un pedido");

        Producto producto = productoRepository.findById(idProducto).orElseThrow();
        BigDecimal suma = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
        BigDecimal reservado = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;

        assertEquals(0, suma.subtract(reservado).compareTo(new BigDecimal("4.00")), "Stock disponible final debe ser 4");
        assertTrue(reservado.signum() >= 0, "stockReservado no debe ser negativo");
        assertTrue(suma.signum() >= 0, "cantidadActual no debe ser negativa");

        List<Pedido> todos = pedidoRepository.findAll();
        assertFalse(todos.isEmpty());
        String estado = todos.get(0).getEstadoPedido();
        if ("PENDIENTE".equalsIgnoreCase(estado)) {
            assertEquals(new BigDecimal("10.00"), producto.getStockLlenos());
            assertEquals(new BigDecimal("10.00"), suma);
            assertEquals(new BigDecimal("6.00"), reservado);
            assertEquals(0, asignacionLotePedidoRepository.count());
        } else if ("ENTREGADO".equalsIgnoreCase(estado)) {
            assertEquals(new BigDecimal("4.00"), producto.getStockLlenos());
            assertEquals(new BigDecimal("4.00"), suma);
            assertEquals(0, reservado.compareTo(BigDecimal.ZERO), "stockReservado debe ser 0");
            List<AsignacionLotePedido> asignaciones = asignacionLotePedidoRepository.findAll();
            assertEquals(1, asignaciones.size(), "Debe haber exactamente una asignación");
            AsignacionLotePedido asignacion = asignaciones.get(0);
            assertEquals(AsignacionLotePedido.EstadoAsignacion.DESCONTADA, asignacion.getEstado(),
                    "La asignación debe estar DESCONTADA");
            assertEquals(0, asignacion.getCantidadDescontada().compareTo(new BigDecimal("6.00")),
                    "cantidadDescontada debe ser 6");
            assertEquals(0, asignacion.getCantidadDevuelta().compareTo(BigDecimal.ZERO),
                    "cantidadDevuelta debe ser 0");
        } else {
            fail("Estado de pedido inesperado: " + estado);
        }
    }

    @Test
    @Timeout(30)
    void dosPedidosACargado_serializanConsumo() throws Exception {
        PedidoDTO.SimpleResponse pedido1 = pedidoService.createOrder(buildPedido("DOMICILIO", 5), idUsuario);
        PedidoDTO.SimpleResponse pedido2 = pedidoService.createOrder(buildPedido("DOMICILIO", 5), idUsuario);

        assertNotNull(pedido1);
        assertNotNull(pedido2);
        assertEquals("PENDIENTE", pedido1.estadoPedido());
        assertEquals("PENDIENTE", pedido2.estadoPedido());

        Producto producto = productoRepository.findById(idProducto).orElseThrow();
        BigDecimal reservadoInicial = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;
        assertEquals(new BigDecimal("10.00"), reservadoInicial, "Deben estar reservados 10");

        Long idPedido1 = pedido1.idPedido();
        Long idPedido2 = pedido2.idPedido();

        ParResultados<Void> par = ejecutarConcurrente(
                () -> { pedidoService.updateEstadoPedido(idPedido1, "ACEPTADO"); return null; },
                () -> { pedidoService.updateEstadoPedido(idPedido2, "ACEPTADO"); return null; }
        );

        ResultadoConcurrente<Void> primero = par.primero();
        ResultadoConcurrente<Void> segundo = par.segundo();

        assertNoLockingError(primero.error());
        assertNoLockingError(segundo.error());

        ParResultados<Void> parCarga = ejecutarConcurrente(
                () -> { pedidoService.updateEstadoPedido(idPedido1, "CARGADO"); return null; },
                () -> { pedidoService.updateEstadoPedido(idPedido2, "CARGADO"); return null; }
        );

        ResultadoConcurrente<Void> carga1 = parCarga.primero();
        ResultadoConcurrente<Void> carga2 = parCarga.segundo();

        int exitos = (carga1.exitoso() ? 1 : 0) + (carga2.exitoso() ? 1 : 0);
        int errores = (carga1.exitoso() ? 0 : 1) + (carga2.exitoso() ? 0 : 1);

        assertEquals(2, exitos, "Ambas transiciones a CARGADO deben triunfar");
        assertEquals(0, errores, "No debe haber errores en la transición a CARGADO");

        assertNoLockingError(carga1.error());
        assertNoLockingError(carga2.error());

        Producto productoFinal = productoRepository.findById(idProducto).orElseThrow();
        BigDecimal sumaFinal = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
        BigDecimal reservadoFinal = productoFinal.getStockReservado() != null ? productoFinal.getStockReservado() : BigDecimal.ZERO;

        assertEquals(new BigDecimal("0.00"), productoFinal.getStockLlenos(), "stockLlenos debe ser 0");
        assertEquals(0, sumaFinal.compareTo(new BigDecimal("0.00")), "lote cantidadActual debe ser 0");
        assertEquals(0, reservadoFinal.compareTo(BigDecimal.ZERO), "stockReservado debe ser 0");

        List<AsignacionLotePedido> asignaciones = asignacionLotePedidoRepository.findAll();
        assertEquals(2, asignaciones.size(), "Debe haber exactamente 2 asignaciones");

        long descontadas = asignaciones.stream()
                .filter(a -> AsignacionLotePedido.EstadoAsignacion.DESCONTADA.equals(a.getEstado()))
                .count();
        assertEquals(2, descontadas, "Ambas asignaciones deben estar DESCONTADA");

        for (AsignacionLotePedido asignacion : asignaciones) {
            assertEquals(0, asignacion.getCantidadDescontada().compareTo(new BigDecimal("5.00")),
                    "cantidadDescontada debe ser 5");
            assertEquals(0, asignacion.getCantidadDevuelta().compareTo(BigDecimal.ZERO),
                    "cantidadDevuelta debe ser 0");
            assertTrue(asignacion.getCantidadDescontada().signum() >= 0, "cantidadDescontada no debe ser negativa");
            assertTrue(asignacion.getCantidadDevuelta().signum() >= 0, "cantidadDevuelta no debe ser negativa");
        }

        Pedido p1 = pedidoRepository.findById(idPedido1).orElseThrow();
        Pedido p2 = pedidoRepository.findById(idPedido2).orElseThrow();
        assertEquals("CARGADO", p1.getEstadoPedido(), "Pedido 1 debe estar CARGADO");
        assertEquals("CARGADO", p2.getEstadoPedido(), "Pedido 2 debe estar CARGADO");
    }

    @Test
    @Timeout(30)
    void devolucionYVentaSimultaneas_sinPerdidaDeStock() throws Exception {
        PedidoDTO.SimpleResponse pedidoOriginal = pedidoService.createOrder(buildPedido("LOCAL", 6), idUsuario);
        assertNotNull(pedidoOriginal);
        assertEquals("ENTREGADO", pedidoOriginal.estadoPedido());

        Long idPedidoOriginal = pedidoOriginal.idPedido();

        Producto producto = productoRepository.findById(idProducto).orElseThrow();
        BigDecimal suma = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
        assertEquals(0, new BigDecimal("4.00").compareTo(suma), "Lote debe quedar en 4");
        assertEquals(0, new BigDecimal("4.00").compareTo(producto.getStockLlenos()), "stockLlenos debe ser 4");

        List<AsignacionLotePedido> asignacionesIniciales = asignacionLotePedidoRepository.findAll();
        assertEquals(1, asignacionesIniciales.size(), "Debe haber exactamente una asignación inicial");
        assertEquals(AsignacionLotePedido.EstadoAsignacion.DESCONTADA, asignacionesIniciales.get(0).getEstado());

        ParResultados<Void> par = ejecutarConcurrente(
                () -> { inventarioLoteService.devolverStockDePedido(idPedidoOriginal); return null; },
                () -> { pedidoService.createOrder(buildPedido("LOCAL", 6), idUsuario); return null; }
        );

        ResultadoConcurrente<Void> primero = par.primero();
        ResultadoConcurrente<Void> segundo = par.segundo();

        assertNoLockingError(primero.error());
        assertNoLockingError(segundo.error());

        int exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        int errores = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);

        Producto productoFinal = productoRepository.findById(idProducto).orElseThrow();
        BigDecimal sumaFinal = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
        BigDecimal reservadoFinal = productoFinal.getStockReservado() != null ? productoFinal.getStockReservado() : BigDecimal.ZERO;

        assertEquals(0, reservadoFinal.compareTo(BigDecimal.ZERO), "stockReservado debe ser 0 en ambas ramas");

        if (primero.exitoso() && segundo.exitoso()) {
            assertEquals(2, exitos, "Rama A: ambas operaciones deben triunfar");
            assertEquals(0, errores, "Rama A: no debe haber errores");

            assertEquals(0, new BigDecimal("4.00").compareTo(sumaFinal), "Rama A: lote final debe ser 4");
            assertEquals(0, new BigDecimal("4.00").compareTo(productoFinal.getStockLlenos()), "Rama A: stockLlenos final debe ser 4");

            List<Pedido> todos = pedidoRepository.findAll();
            long entregados = todos.stream()
                    .filter(p -> "ENTREGADO".equalsIgnoreCase(p.getEstadoPedido()))
                    .count();
            assertEquals(2, entregados, "Rama A: debe haber 2 pedidos ENTREGADO");

            List<AsignacionLotePedido> asignaciones = asignacionLotePedidoRepository.findAll();
            assertEquals(2, asignaciones.size(), "Rama A: debe haber exactamente 2 asignaciones");

            boolean encontroDevuelta = false;
            boolean encontroDescontada = false;
            for (AsignacionLotePedido asignacion : asignaciones) {
                if (AsignacionLotePedido.EstadoAsignacion.DEVUELTA.equals(asignacion.getEstado())) {
                    encontroDevuelta = true;
                    assertEquals(0, asignacion.getCantidadDescontada().compareTo(new BigDecimal("6.00")),
                            "Rama A: asignación DEVUELTA cantidadDescontada debe ser 6");
                    assertEquals(0, asignacion.getCantidadDevuelta().compareTo(new BigDecimal("6.00")),
                            "Rama A: asignación DEVUELTA cantidadDevuelta debe ser 6");
                } else if (AsignacionLotePedido.EstadoAsignacion.DESCONTADA.equals(asignacion.getEstado())) {
                    encontroDescontada = true;
                    assertEquals(0, asignacion.getCantidadDescontada().compareTo(new BigDecimal("6.00")),
                            "Rama A: asignación DESCONTADA cantidadDescontada debe ser 6");
                    assertEquals(0, asignacion.getCantidadDevuelta().compareTo(BigDecimal.ZERO),
                            "Rama A: asignación DESCONTADA cantidadDevuelta debe ser 0");
                }
            }
            assertTrue(encontroDevuelta, "Rama A: debe existir una asignación DEVUELTA");
            assertTrue(encontroDescontada, "Rama A: debe existir una asignación DESCONTADA");

        } else if (primero.exitoso() && !segundo.exitoso()) {
            assertEquals(1, exitos, "Rama B: solo la devolución debe triunfar");
            assertEquals(1, errores, "Rama B: la venta debe fallar");

            assertRechazoStock(segundo.error());

            assertEquals(0, new BigDecimal("10.00").compareTo(sumaFinal), "Rama B: lote final debe ser 10");
            assertEquals(0, new BigDecimal("10.00").compareTo(productoFinal.getStockLlenos()), "Rama B: stockLlenos final debe ser 10");

            List<Pedido> todos = pedidoRepository.findAll();
            assertEquals(1, todos.size(), "Rama B: debe persistir solo el pedido original");
            assertEquals("ENTREGADO", todos.get(0).getEstadoPedido(), "Rama B: el pedido original debe estar ENTREGADO");

            List<AsignacionLotePedido> asignaciones = asignacionLotePedidoRepository.findAll();
            assertEquals(1, asignaciones.size(), "Rama B: debe haber exactamente una asignación");
            AsignacionLotePedido asignacion = asignaciones.get(0);
            assertEquals(AsignacionLotePedido.EstadoAsignacion.DEVUELTA, asignacion.getEstado(),
                    "Rama B: la asignación debe estar DEVUELTA");
            assertEquals(0, asignacion.getCantidadDescontada().compareTo(new BigDecimal("6.00")),
                    "Rama B: cantidadDescontada debe ser 6");
            assertEquals(0, asignacion.getCantidadDevuelta().compareTo(new BigDecimal("6.00")),
                    "Rama B: cantidadDevuelta debe ser 6");
        } else {
            fail("Escenario no válido: se esperaba Rama A (ambos éxitos) o Rama B (solo devolución exitosa)");
        }

        assertTrue(sumaFinal.signum() >= 0, "cantidadActual no debe ser negativa");
        assertTrue(reservadoFinal.signum() >= 0, "stockReservado no debe ser negativo");
    }

    @Test
    @Timeout(30)
    void anulacionCompraYVentaSimultaneas_estadoConsistente() throws Exception {
        ParResultados<Void> par = ejecutarConcurrente(
                () -> { compraService.anularCompra(idCompra); return null; },
                () -> { pedidoService.createOrder(buildPedido("LOCAL", 6), idUsuario); return null; }
        );

        ResultadoConcurrente<Void> primero = par.primero();
        ResultadoConcurrente<Void> segundo = par.segundo();

        int exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        int errores = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);

        assertEquals(1, exitos, "Debe haber exactamente 1 éxito");
        assertEquals(1, errores, "Debe haber exactamente 1 error");

        assertNoLockingError(primero.error());
        assertNoLockingError(segundo.error());

        Producto producto = productoRepository.findById(idProducto).orElseThrow();
        BigDecimal suma = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
        BigDecimal reservado = producto.getStockReservado() != null ? producto.getStockReservado() : BigDecimal.ZERO;

        assertEquals(0, reservado.compareTo(BigDecimal.ZERO), "stockReservado debe ser 0 en ambas ramas");
        assertTrue(suma.signum() >= 0, "cantidadActual no debe ser negativa");

        if (primero.exitoso() && !segundo.exitoso()) {
            assertRechazoStock(segundo.error());

            Compra compra = compraRepository.findById(idCompra).orElseThrow();
            assertEquals(2, compra.getSituacion(), "Rama A: Compra debe estar anulada");

            List<InventarioLote> lotes = inventarioLoteRepository.findHistorialCompletoByProductoIdOrderByCreatedAtDesc(idProducto);
            assertEquals(0, lotes.size(), "Rama A: el lote de la compra debe estar eliminado");

            assertEquals(0, producto.getStockLlenos().compareTo(BigDecimal.ZERO), "Rama A: stockLlenos debe ser 0");
            assertEquals(0, suma.compareTo(BigDecimal.ZERO), "Rama A: no debe quedar stock en lotes");

            List<Pedido> todos = pedidoRepository.findAll();
            assertEquals(0, todos.size(), "Rama A: no debe persistir un pedido nuevo");

        } else if (!primero.exitoso() && segundo.exitoso()) {
            assertRechazoConflicto(primero.error());

            Compra compra = compraRepository.findById(idCompra).orElseThrow();
            assertEquals(1, compra.getSituacion(), "Rama B: Compra no debe estar anulada");

            List<InventarioLote> lotes = inventarioLoteRepository.findHistorialCompletoByProductoIdOrderByCreatedAtDesc(idProducto);
            assertEquals(1, lotes.size(), "Rama B: el lote no debe eliminarse");
            assertEquals(0, new BigDecimal("4.00").compareTo(lotes.get(0).getCantidadActual()),
                    "Rama B: lote cantidadActual debe ser 4");

            assertEquals(0, new BigDecimal("4.00").compareTo(producto.getStockLlenos()),
                    "Rama B: stockLlenos debe ser 4");

            List<Pedido> todos = pedidoRepository.findAll();
            assertEquals(1, todos.size(), "Rama B: debe persistir el pedido nuevo");
            assertEquals("ENTREGADO", todos.get(0).getEstadoPedido(), "Rama B: el pedido nuevo debe estar ENTREGADO");

            List<AsignacionLotePedido> asignaciones = asignacionLotePedidoRepository.findAll();
            assertEquals(1, asignaciones.size(), "Rama B: debe haber exactamente una asignación");
            AsignacionLotePedido asignacion = asignaciones.get(0);
            assertEquals(AsignacionLotePedido.EstadoAsignacion.DESCONTADA, asignacion.getEstado(),
                    "Rama B: la asignación debe estar DESCONTADA");
            assertEquals(0, asignacion.getCantidadDescontada().compareTo(new BigDecimal("6.00")),
                    "Rama B: cantidadDescontada debe ser 6");
            assertEquals(0, asignacion.getCantidadDevuelta().compareTo(BigDecimal.ZERO),
                    "Rama B: cantidadDevuelta debe ser 0");
        } else {
            fail("Escenario no válido: se esperaba Rama A (anulación exitosa) o Rama B (venta exitosa)");
        }
    }

    @Test
    @Timeout(30)
    void ordenInversoDetalles_noProduceDeadlock() throws Exception {
        idProducto2 = ensureProducto2(new BigDecimal("10.00"), BigDecimal.ZERO);
        ensureLoteParaProducto(idProducto2, new BigDecimal("10.00"), new BigDecimal("10.00"));

        List<PedidoDTO.DetalleCreate> detallesA = List.of(
                new PedidoDTO.DetalleCreate(idProducto, 3, new BigDecimal("10.00"), 0),
                new PedidoDTO.DetalleCreate(idProducto2, 3, new BigDecimal("10.00"), 0)
        );

        List<PedidoDTO.DetalleCreate> detallesB = List.of(
                new PedidoDTO.DetalleCreate(idProducto2, 3, new BigDecimal("10.00"), 0),
                new PedidoDTO.DetalleCreate(idProducto, 3, new BigDecimal("10.00"), 0)
        );

        ParResultados<PedidoDTO.SimpleResponse> par = ejecutarConcurrente(
                () -> pedidoService.createOrder(buildPedidoConDetalles("LOCAL", detallesA), idUsuario),
                () -> pedidoService.createOrder(buildPedidoConDetalles("LOCAL", detallesB), idUsuario)
        );

        ResultadoConcurrente<PedidoDTO.SimpleResponse> primero = par.primero();
        ResultadoConcurrente<PedidoDTO.SimpleResponse> segundo = par.segundo();

        int exitos = (primero.exitoso() ? 1 : 0) + (segundo.exitoso() ? 1 : 0);
        int errores = (primero.exitoso() ? 0 : 1) + (segundo.exitoso() ? 0 : 1);

        assertEquals(2, exitos, "Ambos pedidos deben crearse exitosamente");
        assertEquals(0, errores, "No debe haber errores");

        assertNoLockingError(primero.error());
        assertNoLockingError(segundo.error());

        Producto producto1 = productoRepository.findById(idProducto).orElseThrow();
        Producto producto2 = productoRepository.findById(idProducto2).orElseThrow();
        BigDecimal suma1 = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto);
        BigDecimal suma2 = inventarioLoteRepository.sumCantidadActualByProductoId(idProducto2);
        BigDecimal reservado1 = producto1.getStockReservado() != null ? producto1.getStockReservado() : BigDecimal.ZERO;
        BigDecimal reservado2 = producto2.getStockReservado() != null ? producto2.getStockReservado() : BigDecimal.ZERO;

        List<Pedido> todos = pedidoRepository.findAll();
        List<AsignacionLotePedido> asignaciones = asignacionLotePedidoRepository.findAll();

        assertEquals(0, new BigDecimal("4.00").compareTo(suma1), "Producto 1: lote cantidadActual debe ser 4");
        assertEquals(0, new BigDecimal("4.00").compareTo(suma2), "Producto 2: lote cantidadActual debe ser 4");
        assertEquals(0, new BigDecimal("4.00").compareTo(producto1.getStockLlenos()), "Producto 1: stockLlenos debe ser 4");
        assertEquals(0, new BigDecimal("4.00").compareTo(producto2.getStockLlenos()), "Producto 2: stockLlenos debe ser 4");
        assertEquals(0, reservado1.compareTo(BigDecimal.ZERO), "Producto 1: stockReservado debe ser 0");
        assertEquals(0, reservado2.compareTo(BigDecimal.ZERO), "Producto 2: stockReservado debe ser 0");

        assertEquals(2, todos.size(), "Debe haber exactamente 2 pedidos");
        long entregados = todos.stream()
                .filter(p -> "ENTREGADO".equalsIgnoreCase(p.getEstadoPedido()))
                .count();
        assertEquals(2, entregados, "Ambos pedidos deben estar ENTREGADO");

        assertEquals(4, asignaciones.size(), "Debe haber exactamente 4 asignaciones");

        long descontadas = asignaciones.stream()
                .filter(a -> AsignacionLotePedido.EstadoAsignacion.DESCONTADA.equals(a.getEstado()))
                .count();
        assertEquals(4, descontadas, "Todas las asignaciones deben estar DESCONTADA");

        for (AsignacionLotePedido asignacion : asignaciones) {
            assertEquals(0, asignacion.getCantidadDescontada().compareTo(new BigDecimal("3.00")),
                    "cantidadDescontada debe ser 3");
            assertEquals(0, asignacion.getCantidadDevuelta().compareTo(BigDecimal.ZERO),
                    "cantidadDevuelta debe ser 0");
            assertTrue(asignacion.getCantidadDescontada().signum() >= 0, "cantidadDescontada no debe ser negativa");
            assertTrue(asignacion.getCantidadDevuelta().signum() >= 0, "cantidadDevuelta no debe ser negativa");
        }

        List<InventarioLote> lotesP1 = inventarioLoteRepository.findHistorialCompletoByProductoIdOrderByCreatedAtDesc(idProducto);
        List<InventarioLote> lotesP2 = inventarioLoteRepository.findHistorialCompletoByProductoIdOrderByCreatedAtDesc(idProducto2);
        assertEquals(1, lotesP1.size(), "Producto 1 debe tener exactamente 1 lote");
        assertEquals(1, lotesP2.size(), "Producto 2 debe tener exactamente 1 lote");
    }
}
