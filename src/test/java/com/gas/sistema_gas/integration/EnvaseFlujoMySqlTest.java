package com.gas.sistema_gas.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import com.gas.sistema_gas.SistemaGasApplication;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.ControlEnvase;
import com.gas.sistema_gas.Model.Correlativo;
import com.gas.sistema_gas.Model.Compra;
import com.gas.sistema_gas.Model.DetallePedido;
import com.gas.sistema_gas.Model.Empleado;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.MetodoPago;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Model.Rubro;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.Repository.DetallePedidoRepository;
import com.gas.sistema_gas.Repository.MetodoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoPagoRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.service.PedidoService;
import com.gas.sistema_gas.service.PedidoPagosService;
import com.gas.sistema_gas.service.EnvaseService;
import com.gas.sistema_gas.dto.ConfirmarEntregaMixtaDTO;
import com.gas.sistema_gas.dto.EnvioEnvaseDTO;
import com.gas.sistema_gas.dto.PagoRegistroDTO;
import com.gas.sistema_gas.dto.PedidoDTO;
import com.gas.sistema_gas.dto.PedidoPagoYapeDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(classes = SistemaGasApplication.class)
@ContextConfiguration(
        initializers = InventarioConcurrenciaMySqlTest.TestDbInitializer.class
)
class EnvaseFlujoMySqlTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private PedidoPagoRepository pedidoPagoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private PedidoPagosService pedidoPagosService;

    @Autowired
    private ControlEnvaseRepository controlEnvaseRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvaseService envaseService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private Long pedidoId;
    private Long productoId;
    private Long clienteId;
    private Long usuarioId;
    private Long empleadoId;
    private Long efectivoId;
    private Long yapeId;

    @BeforeEach
    void prepararFixture() {
        assertEquals("sistema_gas_concurrency_test",
                jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'control_envase' "
                        + "AND COLUMN_NAME IN ('fecha_limite_devolucion', 'tipo_prestamo')",
                Integer.class));
        String sufijoUnico = UUID.randomUUID().toString().substring(0, 8);

        transactionTemplate.executeWithoutResult(status -> {
            Perfil perfil = new Perfil();
            perfil.setNombrePerfil("Perfil Envase Test");
            perfil.setEstado(1);
            entityManager.persist(perfil);

            Categoria categoria = new Categoria();
            categoria.setNombre("Categoria Envase Test");
            categoria.setEstado(1);
            entityManager.persist(categoria);
            entityManager.flush();

            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente Envase Test");
            cliente.setDni("12345678");
            cliente.setTelefono("999999999");
            cliente.setDireccion("Direccion de prueba");
            cliente.setEstado(1);
            cliente = clienteRepository.saveAndFlush(cliente);

            Usuario usuario = new Usuario();
            usuario.setUserName("usuario.envase." + sufijoUnico);
            usuario.setPassword("password");
            usuario.setCorreo("usuario.envase." + sufijoUnico + "@test.com");
            usuario.setEstado(1);
            usuario.setPerfil(perfil);
            usuario = usuarioRepository.saveAndFlush(usuario);

            Empleado empleado = new Empleado();
            long identificador = Math.floorMod(System.nanoTime(), 100_000_000L);
            empleado.setNombre("Motorizado Envase Test");
            empleado.setDni(String.format("%08d", identificador));
            empleado.setTelefono(String.format("9%08d", identificador));
            empleado.setCorreo("motorizado." + sufijoUnico + "@gmail.com");
            empleado.setEstado(1);
            entityManager.persist(empleado);

            MetodoPago efectivo = metodoPagoRepository.findByNombre("Efectivo").orElseGet(() -> {
                MetodoPago metodo = new MetodoPago();
                metodo.setNombre("Efectivo");
                metodo.setEstado(1);
                return metodoPagoRepository.saveAndFlush(metodo);
            });
            MetodoPago yape = metodoPagoRepository.findByNombre("Yape").orElseGet(() -> {
                MetodoPago metodo = new MetodoPago();
                metodo.setNombre("Yape");
                metodo.setEstado(1);
                return metodoPagoRepository.saveAndFlush(metodo);
            });

            Producto producto = new Producto();
            producto.setNombre("Envase Test");
            producto.setPrecioCompra(new BigDecimal("10.00"));
            producto.setGananciaProducto(BigDecimal.ZERO);
            producto.setPrecioVenta(new BigDecimal("10.00"));
            producto.setStockLlenos(BigDecimal.ZERO);
            producto.setStockMinimo(BigDecimal.ZERO);
            producto.setStockVacios(4);
            producto.setStockReservado(BigDecimal.ZERO);
            producto.setRequiereEnvase(true);
            producto.setEstado(1);
            producto.setCategoria(categoria);
            producto = productoRepository.saveAndFlush(producto);

            Pedido pedido = new Pedido();
            pedido.setCodigo("NV-ENVASE-" + sufijoUnico);
            pedido.setFechaSolicitud(LocalDateTime.now());
            pedido.setEstadoPedido("PENDIENTE");
            pedido.setEstadoPago("PENDIENTE");
            pedido.setTipoVenta("DOMICILIO");
            pedido.setSubtotal(BigDecimal.ZERO);
            pedido.setMontoTotal(BigDecimal.ZERO);
            pedido.setCliente(cliente);
            pedido.setUsuario(usuario);
            pedido = pedidoRepository.saveAndFlush(pedido);

            ControlEnvase controlEnvase = new ControlEnvase();
            controlEnvase.setCantidadPrestada(1);
            controlEnvase.setCantidadDevuelta(0);
            controlEnvase.setEstado("PRESTADO");
            LocalDateTime fechaPrestamo = LocalDateTime.now();
            controlEnvase.setFechaPrestamo(fechaPrestamo);
            controlEnvase.setFechaLimiteDevolucion(fechaPrestamo.toLocalDate().plusDays(3));
            controlEnvase.setFechaDevolucion(null);
            controlEnvase.setTipoPrestamo("NORMAL");
            controlEnvase.setProducto(producto);
            controlEnvase.setCliente(cliente);
            controlEnvase.setPedido(pedido);
            controlEnvaseRepository.saveAndFlush(controlEnvase);

            pedidoId = pedido.getId();
            productoId = producto.getId();
            clienteId = cliente.getId();
            usuarioId = usuario.getId();
            empleadoId = empleado.getId();
            efectivoId = efectivo.getId();
            yapeId = yape.getId();
        });
    }

    @Test
    void anularPedidoPendienteConPrestamo_debeEliminarDeudaYReponerEnvase() {
        pedidoService.deleteOrder(pedidoId);

        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        Producto producto = productoRepository.findById(productoId).orElseThrow();
        List<ControlEnvase> deudas = controlEnvaseRepository.findByPedido_Id(pedidoId);

        assertEquals("ANULADO", pedido.getEstadoPedido());
        assertAll(
                () -> assertEquals(5, producto.getStockVacios()),
                () -> assertEquals(0, deudas.size())
        );
    }

    @Test
    void cambiarEstadoAAnuladoConPrestamo_debeEliminarDeudaYReponerEnvase() {
        pedidoService.updateEstadoPedido(pedidoId, "ANULADO");

        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        Producto producto = productoRepository.findById(productoId).orElseThrow();
        List<ControlEnvase> deudas = controlEnvaseRepository.findByPedido_Id(pedidoId);

        assertEquals("ANULADO", pedido.getEstadoPedido());
        assertAll(
                () -> assertEquals(5, producto.getStockVacios()),
                () -> assertEquals(0, deudas.size())
        );
    }

    @Test
    void crearPrestamoSinStockSuficiente_debeRechazarYNoCrearDeuda() {
        String rucUnico = String.format("20%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
        transactionTemplate.executeWithoutResult(status -> {
            List<Correlativo> correlativos = entityManager.createQuery(
                    "FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie", Correlativo.class)
                    .setParameter("tipo", "VENTA_NOTA")
                    .setParameter("serie", "NV001")
                    .getResultList();
            if (correlativos.isEmpty()) {
                Correlativo correlativo = new Correlativo();
                correlativo.setTipo("VENTA_NOTA");
                correlativo.setSerie("NV001");
                correlativo.setNumeroActual(0);
                entityManager.persist(correlativo);
            }

            Producto producto = productoRepository.findById(productoId).orElseThrow();
            producto.setStockVacios(1);

            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro préstamo " + rucUnico);
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Proveedor préstamo " + rucUnico);
            proveedor.setTelefono("999999999");
            proveedor.setCorreo("proveedor." + rucUnico + "@test.com");
            proveedor.setRuc(rucUnico);
            proveedor.setEstado(1);
            proveedor.setRubro(rubro);
            entityManager.persist(proveedor);

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
            compra.setFechaCompra(LocalDateTime.now());
            compra.setMontoTotal(new BigDecimal("10.00"));
            compra.setSituacion(1);
            entityManager.persist(compra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.ONE);
            lote.setCantidadActual(BigDecimal.ONE);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
        });

        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();
        PedidoDTO.SimpleResponse respuesta = null;
        ResponseStatusException excepcion = null;

        try {
            respuesta = pedidoService.createOrder(new PedidoDTO.Create(
                    null,
                    clienteId,
                    null,
                    "Cliente Envase Test",
                    null,
                    null,
                    null,
                    null,
                    usuarioId,
                    null,
                    null,
                    null,
                    null,
                    "LOCAL",
                    null,
                    List.of(),
                    List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), null)),
                    "PRESTAMO",
                    List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, 2, null, null, null, null))
            ), usuarioId);
        } catch (ResponseStatusException error) {
            excepcion = error;
        }

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        long controlesDespues = controlEnvaseRepository.count();
        long pedidosDespues = pedidoRepository.count();
        ResponseStatusException excepcionCapturada = excepcion;
        PedidoDTO.SimpleResponse respuestaCapturada = respuesta;

        assertAll(
                () -> assertNotNull(excepcionCapturada, "createOrder debe rechazar el préstamo sin stock suficiente"),
                () -> assertTrue(excepcionCapturada.getReason() != null
                        && excepcionCapturada.getReason().contains("Stock de envases insuficiente")),
                () -> assertEquals(1, producto.getStockVacios()),
                () -> assertEquals(controlesAntes, controlesDespues),
                () -> assertEquals(pedidosAntes, pedidosDespues),
                () -> assertEquals(null, respuestaCapturada, "No debe devolverse un pedido creado")
        );
    }

    @Test
    void crearVentaEnvase_debeDescontarStockVaciosYNoCrearDeuda() {
        String rucUnico = String.format("20%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
        transactionTemplate.executeWithoutResult(status -> {
            List<Correlativo> correlativos = entityManager.createQuery(
                    "FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie", Correlativo.class)
                    .setParameter("tipo", "VENTA_NOTA")
                    .setParameter("serie", "NV001")
                    .getResultList();
            if (correlativos.isEmpty()) {
                Correlativo correlativo = new Correlativo();
                correlativo.setTipo("VENTA_NOTA");
                correlativo.setSerie("NV001");
                correlativo.setNumeroActual(0);
                entityManager.persist(correlativo);
            }

            Producto producto = productoRepository.findById(productoId).orElseThrow();
            producto.setStockVacios(5);

            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro venta envase " + rucUnico);
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Proveedor venta envase " + rucUnico);
            proveedor.setTelefono("999999999");
            proveedor.setCorreo("proveedor.venta." + rucUnico + "@test.com");
            proveedor.setRuc(rucUnico);
            proveedor.setEstado(1);
            proveedor.setRubro(rubro);
            entityManager.persist(proveedor);

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
            compra.setFechaCompra(LocalDateTime.now());
            compra.setMontoTotal(new BigDecimal("10.00"));
            compra.setSituacion(1);
            entityManager.persist(compra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.ONE);
            lote.setCantidadActual(BigDecimal.ONE);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
        });

        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(new PedidoDTO.Create(
                null,
                clienteId,
                null,
                "Cliente Envase Test",
                null,
                null,
                null,
                null,
                usuarioId,
                null,
                null,
                null,
                null,
                "LOCAL",
                null,
                List.of(),
                List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), null)),
                "VENTA",
                List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, 2, new BigDecimal("10.00"), null, null, null))
        ), usuarioId);

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        Pedido pedido = pedidoRepository.findById(respuesta.idPedido()).orElseThrow();
        long detallesEnvase = transactionTemplate.execute(status -> entityManager.createQuery(
                "SELECT COUNT(d) FROM DetallePedido d WHERE d.pedido.id = :idPedido "
                        + "AND d.producto.id = :idProducto AND d.cantidad = :cantidad", Long.class)
                .setParameter("idPedido", respuesta.idPedido())
                .setParameter("idProducto", productoId)
                .setParameter("cantidad", 2)
                .getSingleResult());

        assertAll(
                () -> assertNotNull(respuesta),
                () -> assertEquals(pedidosAntes + 1, pedidoRepository.count()),
                () -> assertNotNull(pedido),
                () -> assertEquals(3, producto.getStockVacios()),
                () -> assertTrue(producto.getStockVacios() >= 0),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count()),
                () -> assertEquals(1, detallesEnvase),
                () -> assertEquals(new BigDecimal("30.00"), respuesta.montoTotal())
        );
    }

    @Test
    void crearVentaEnvaseSinStockSuficiente_debeRechazarYHacerRollback() {
        String rucUnico = String.format("20%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
        transactionTemplate.executeWithoutResult(status -> {
            List<Correlativo> correlativos = entityManager.createQuery(
                    "FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie", Correlativo.class)
                    .setParameter("tipo", "VENTA_NOTA")
                    .setParameter("serie", "NV001")
                    .getResultList();
            if (correlativos.isEmpty()) {
                Correlativo correlativo = new Correlativo();
                correlativo.setTipo("VENTA_NOTA");
                correlativo.setSerie("NV001");
                correlativo.setNumeroActual(0);
                entityManager.persist(correlativo);
            }

            Producto producto = productoRepository.findById(productoId).orElseThrow();
            producto.setStockVacios(1);

            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro venta sin stock " + rucUnico);
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Proveedor venta sin stock " + rucUnico);
            proveedor.setTelefono("999999999");
            proveedor.setCorreo("proveedor.venta.sin.stock." + rucUnico + "@test.com");
            proveedor.setRuc(rucUnico);
            proveedor.setEstado(1);
            proveedor.setRubro(rubro);
            entityManager.persist(proveedor);

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
            compra.setFechaCompra(LocalDateTime.now());
            compra.setMontoTotal(new BigDecimal("10.00"));
            compra.setSituacion(1);
            entityManager.persist(compra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.ONE);
            lote.setCantidadActual(BigDecimal.ONE);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
        });

        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();
        long detallesAntes = transactionTemplate.execute(status -> entityManager.createQuery(
                "SELECT COUNT(d) FROM DetallePedido d", Long.class).getSingleResult());

        PedidoDTO.SimpleResponse respuesta = null;
        ResponseStatusException excepcion = null;
        try {
            respuesta = pedidoService.createOrder(new PedidoDTO.Create(
                    null,
                    clienteId,
                    null,
                    "Cliente Envase Test",
                    null,
                    null,
                    null,
                    null,
                    usuarioId,
                    null,
                    null,
                    null,
                    null,
                    "LOCAL",
                    null,
                    List.of(),
                    List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), null)),
                    "VENTA",
                    List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, 2, new BigDecimal("10.00"), null, null, null))
            ), usuarioId);
        } catch (ResponseStatusException error) {
            excepcion = error;
        }

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        long detallesDespues = transactionTemplate.execute(status -> entityManager.createQuery(
                "SELECT COUNT(d) FROM DetallePedido d", Long.class).getSingleResult());
        ResponseStatusException excepcionCapturada = excepcion;
        PedidoDTO.SimpleResponse respuestaCapturada = respuesta;

        assertAll(
                () -> assertNotNull(excepcionCapturada),
                () -> assertTrue(excepcionCapturada.getReason() != null
                        && excepcionCapturada.getReason().contains("Stock de envases insuficiente")),
                () -> assertEquals(1, producto.getStockVacios()),
                () -> assertTrue(producto.getStockVacios() >= 0),
                () -> assertEquals(pedidosAntes, pedidoRepository.count()),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count()),
                () -> assertEquals(detallesAntes, detallesDespues),
                () -> assertEquals(null, respuestaCapturada)
        );
    }

    @Test
    void ventaEnvaseDomicilio_debeConsumirSoloContenidoYMantenerVaciosAlEntregar() {
        prepararInventarioParaPrestamo(5, 3);

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(
                crearPedidoMovimientoDomicilio("VENTA"), usuarioId);
        Pedido pedidoCreado = pedidoRepository.findById(respuesta.idPedido()).orElseThrow();
        List<DetallePedido> detallesCreados = detallePedidoRepository.findByPedido_Id(respuesta.idPedido());

        assertAll(
                () -> assertEquals("PENDIENTE", pedidoCreado.getEstadoPedido()),
                () -> assertEquals(4, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(0, BigDecimal.ONE.compareTo(
                        productoRepository.findById(productoId).orElseThrow().getStockReservado())),
                () -> assertEquals(0, new BigDecimal("3").compareTo(stockActualLotes())),
                () -> assertEquals(2, detallesCreados.size()),
                () -> assertEquals(1, detallesCreados.stream()
                        .filter(detalle -> Boolean.TRUE.equals(detalle.getEsEnvaseVendido())).count()),
                () -> assertEquals(0, controlEnvaseRepository.findByPedido_Id(respuesta.idPedido()).size())
        );

        pedidoService.updateEstadoPedido(respuesta.idPedido(), "ACEPTADO");
        pedidoService.updateEstadoPedido(respuesta.idPedido(), "CARGADO");

        assertAll(
                () -> assertEquals(0, new BigDecimal("2").compareTo(stockActualLotes())),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(
                        productoRepository.findById(productoId).orElseThrow().getStockReservado())),
                () -> assertEquals(4, productoRepository.findById(productoId).orElseThrow().getStockVacios())
        );

        pedidoService.updateEstadoPedido(respuesta.idPedido(), "EN_CAMINO");
        pedidoService.updateEstadoPedido(respuesta.idPedido(), "EN_DOMICILIO");
        pedidoPagosService.confirmarEntregaPagoUnico(new PedidoPagoYapeDTO(
                respuesta.idPedido(), efectivoId, new BigDecimal("20.00"), ""), null, null);

        assertAll(
                () -> assertEquals("ENTREGADO",
                        pedidoRepository.findById(respuesta.idPedido()).orElseThrow().getEstadoPedido()),
                () -> assertEquals(0, new BigDecimal("2").compareTo(stockActualLotes())),
                () -> assertEquals(4, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(0, controlEnvaseRepository.findByPedido_Id(respuesta.idPedido()).size()),
                () -> assertTrue(detallePedidoRepository.findByPedido_Id(respuesta.idPedido()).stream()
                        .allMatch(detalle -> Integer.valueOf(0).equals(detalle.getCantidadCanje())))
        );
    }

    @Test
    void anularVentaEnvaseDomicilio_debeRestaurarVaciosPorProductoSinDuplicar() {
        prepararInventarioParaPrestamo(8, 5);
        Long productoDosId = crearSegundoProductoConLote(9, 4);
        PedidoDTO.Create pedidoVenta = new PedidoDTO.Create(
                null, clienteId, null, "Cliente Envase Test", null, null, null, empleadoId, usuarioId,
                null, null, null, null, "DOMICILIO", null, List.of(),
                List.of(
                        new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), null),
                        new PedidoDTO.DetalleCreate(productoDosId, 2, new BigDecimal("10.00"), null)),
                "VENTA",
                List.of(
                        new PedidoDTO.EnvaseMovimientoCreate(productoId, 3, new BigDecimal("10.00"), null, null, null),
                        new PedidoDTO.EnvaseMovimientoCreate(productoDosId, 4, new BigDecimal("10.00"), null, null, null)));

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(pedidoVenta, usuarioId);
        assertAll(
                () -> assertEquals(5, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(5, productoRepository.findById(productoDosId).orElseThrow().getStockVacios()),
                () -> assertEquals(0, BigDecimal.ONE.compareTo(
                        productoRepository.findById(productoId).orElseThrow().getStockReservado())),
                () -> assertEquals(0, new BigDecimal("2").compareTo(
                        productoRepository.findById(productoDosId).orElseThrow().getStockReservado()))
        );

        pedidoService.deleteOrder(respuesta.idPedido());

        assertAll(
                () -> assertEquals("ANULADO",
                        pedidoRepository.findById(respuesta.idPedido()).orElseThrow().getEstadoPedido()),
                () -> assertEquals(8, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(9, productoRepository.findById(productoDosId).orElseThrow().getStockVacios()),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(
                        productoRepository.findById(productoId).orElseThrow().getStockReservado())),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(
                        productoRepository.findById(productoDosId).orElseThrow().getStockReservado())),
                () -> assertEquals(0, new BigDecimal("5").compareTo(stockActualLotes())),
                () -> assertEquals(0, controlEnvaseRepository.findByPedido_Id(respuesta.idPedido()).size())
        );

        ResponseStatusException repetida = assertThrows(ResponseStatusException.class,
                () -> pedidoService.deleteOrder(respuesta.idPedido()));
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, repetida.getStatusCode()),
                () -> assertEquals(8, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(9, productoRepository.findById(productoDosId).orElseThrow().getStockVacios())
        );
    }

    @Test
    void prestamoExplicitoConCantidadPrestadaLegada_noDebeDuplicarDeuda() {
        String rucUnico = String.format("20%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
        transactionTemplate.executeWithoutResult(status -> {
            List<Correlativo> correlativos = entityManager.createQuery(
                    "FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie", Correlativo.class)
                    .setParameter("tipo", "VENTA_NOTA")
                    .setParameter("serie", "NV001")
                    .getResultList();
            if (correlativos.isEmpty()) {
                Correlativo correlativo = new Correlativo();
                correlativo.setTipo("VENTA_NOTA");
                correlativo.setSerie("NV001");
                correlativo.setNumeroActual(0);
                entityManager.persist(correlativo);
            }

            Producto producto = productoRepository.findById(productoId).orElseThrow();
            producto.setStockVacios(5);
            producto.setRequiereEnvase(true);

            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro préstamo duplicado " + rucUnico);
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Proveedor préstamo duplicado " + rucUnico);
            proveedor.setTelefono("999999999");
            proveedor.setCorreo("proveedor.prestamo.duplicado." + rucUnico + "@test.com");
            proveedor.setRuc(rucUnico);
            proveedor.setEstado(1);
            proveedor.setRubro(rubro);
            entityManager.persist(proveedor);

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
            compra.setFechaCompra(LocalDateTime.now());
            compra.setMontoTotal(new BigDecimal("10.00"));
            compra.setSituacion(1);
            entityManager.persist(compra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.ONE);
            lote.setCantidadActual(BigDecimal.ONE);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
        });

        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(new PedidoDTO.Create(
                null,
                clienteId,
                null,
                "Cliente Envase Test",
                null,
                null,
                null,
                null,
                usuarioId,
                null,
                null,
                null,
                null,
                "LOCAL",
                null,
                List.of(),
                List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), 1)),
                "PRESTAMO",
                List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, 1, null, null, null, null))
        ), usuarioId);

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        List<ControlEnvase> controlesNuevoPedido = controlEnvaseRepository.findByPedido_Id(respuesta.idPedido());

        assertAll(
                () -> assertNotNull(respuesta),
                () -> assertEquals(pedidosAntes + 1, pedidoRepository.count()),
                () -> assertEquals(4, producto.getStockVacios()),
                () -> assertEquals(controlesAntes + 1, controlEnvaseRepository.count()),
                () -> assertEquals(1, controlesNuevoPedido.size()),
                () -> assertTrue(controlesNuevoPedido.stream().allMatch(control ->
                        productoId.equals(control.getProducto().getId())
                                && respuesta.idPedido().equals(control.getPedido().getId())
                                && Integer.valueOf(1).equals(control.getCantidadPrestada())
                                && Integer.valueOf(0).equals(control.getCantidadDevuelta())
                                && "PRESTADO".equals(control.getEstado())))
        );
    }

    @Test
    void ningunoConCantidadPrestadaLegada_noDebeCrearPrestamo() {
        String rucUnico = String.format("20%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
        transactionTemplate.executeWithoutResult(status -> {
            List<Correlativo> correlativos = entityManager.createQuery(
                    "FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie", Correlativo.class)
                    .setParameter("tipo", "VENTA_NOTA")
                    .setParameter("serie", "NV001")
                    .getResultList();
            if (correlativos.isEmpty()) {
                Correlativo correlativo = new Correlativo();
                correlativo.setTipo("VENTA_NOTA");
                correlativo.setSerie("NV001");
                correlativo.setNumeroActual(0);
                entityManager.persist(correlativo);
            }

            Producto producto = productoRepository.findById(productoId).orElseThrow();
            producto.setStockVacios(5);
            producto.setRequiereEnvase(true);

            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro ninguno legado " + rucUnico);
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Proveedor ninguno legado " + rucUnico);
            proveedor.setTelefono("999999999");
            proveedor.setCorreo("proveedor.ninguno.legado." + rucUnico + "@test.com");
            proveedor.setRuc(rucUnico);
            proveedor.setEstado(1);
            proveedor.setRubro(rubro);
            entityManager.persist(proveedor);

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
            compra.setFechaCompra(LocalDateTime.now());
            compra.setMontoTotal(new BigDecimal("10.00"));
            compra.setSituacion(1);
            entityManager.persist(compra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.ONE);
            lote.setCantidadActual(BigDecimal.ONE);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
        });

        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(new PedidoDTO.Create(
                null,
                clienteId,
                null,
                "Cliente Envase Test",
                null,
                null,
                null,
                null,
                usuarioId,
                null,
                null,
                null,
                null,
                "LOCAL",
                null,
                List.of(),
                List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), 1)),
                "NINGUNO",
                List.of()
        ), usuarioId);

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        List<ControlEnvase> controlesNuevoPedido = controlEnvaseRepository.findByPedido_Id(respuesta.idPedido());

        assertAll(
                () -> assertNotNull(respuesta),
                () -> assertEquals(pedidosAntes + 1, pedidoRepository.count()),
                () -> assertEquals(5, producto.getStockVacios()),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count()),
                () -> assertEquals(0, controlesNuevoPedido.size())
        );
    }

    @Test
    void prestamoLegadoSinTipoMovimiento_debeSeguirFuncionando() {
        String rucUnico = String.format("20%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
        transactionTemplate.executeWithoutResult(status -> {
            List<Correlativo> correlativos = entityManager.createQuery(
                    "FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie", Correlativo.class)
                    .setParameter("tipo", "VENTA_NOTA")
                    .setParameter("serie", "NV001")
                    .getResultList();
            if (correlativos.isEmpty()) {
                Correlativo correlativo = new Correlativo();
                correlativo.setTipo("VENTA_NOTA");
                correlativo.setSerie("NV001");
                correlativo.setNumeroActual(0);
                entityManager.persist(correlativo);
            }

            Producto producto = productoRepository.findById(productoId).orElseThrow();
            producto.setStockVacios(5);
            producto.setRequiereEnvase(true);

            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro préstamo legado " + rucUnico);
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Proveedor préstamo legado " + rucUnico);
            proveedor.setTelefono("999999999");
            proveedor.setCorreo("proveedor.prestamo.legado." + rucUnico + "@test.com");
            proveedor.setRuc(rucUnico);
            proveedor.setEstado(1);
            proveedor.setRubro(rubro);
            entityManager.persist(proveedor);

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
            compra.setFechaCompra(LocalDateTime.now());
            compra.setMontoTotal(new BigDecimal("10.00"));
            compra.setSituacion(1);
            entityManager.persist(compra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.ONE);
            lote.setCantidadActual(BigDecimal.ONE);
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
        });

        long controlesAntes = controlEnvaseRepository.count();

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(new PedidoDTO.Create(
                null,
                clienteId,
                null,
                "Cliente Envase Test",
                null,
                null,
                null,
                null,
                usuarioId,
                null,
                null,
                null,
                null,
                "LOCAL",
                null,
                List.of(),
                List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), 1)),
                null,
                List.of()
        ), usuarioId);

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        List<ControlEnvase> controlesNuevoPedido = controlEnvaseRepository.findByPedido_Id(respuesta.idPedido());

        assertAll(
                () -> assertNotNull(respuesta),
                () -> assertEquals(4, producto.getStockVacios()),
                () -> assertEquals(controlesAntes + 1, controlEnvaseRepository.count()),
                () -> assertEquals(1, controlesNuevoPedido.size()),
                () -> assertEquals(productoId, controlesNuevoPedido.get(0).getProducto().getId()),
                () -> assertEquals(1, controlesNuevoPedido.get(0).getCantidadPrestada()),
                () -> assertEquals(0, controlesNuevoPedido.get(0).getCantidadDevuelta()),
                () -> assertEquals("PRESTADO", controlesNuevoPedido.get(0).getEstado())
        );
    }

    @Test
    void prestamoNormalSinFecha_debeAsignarLimiteTresDias() {
        prepararInventarioParaPrestamo(5);

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(
                crearPedidoPrestamo(null, null), usuarioId);
        ControlEnvase prestamo = unicoPrestamo(respuesta.idPedido());

        assertAll(
                () -> assertEquals("NORMAL", prestamo.getTipoPrestamo()),
                () -> assertEquals(prestamo.getFechaPrestamo().toLocalDate().plusDays(3),
                        prestamo.getFechaLimiteDevolucion()),
                () -> assertNull(prestamo.getFechaDevolucion())
        );
    }

    @Test
    void prestamoNormalConFechaValida_debeConservarFechaSolicitada() {
        prepararInventarioParaPrestamo(5);
        LocalDate fechaSolicitada = LocalDate.now().plusDays(2);

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(
                crearPedidoPrestamo(fechaSolicitada.toString(), "NORMAL"), usuarioId);
        ControlEnvase prestamo = unicoPrestamo(respuesta.idPedido());

        assertAll(
                () -> assertEquals("NORMAL", prestamo.getTipoPrestamo()),
                () -> assertEquals(fechaSolicitada, prestamo.getFechaLimiteDevolucion()),
                () -> assertNull(prestamo.getFechaDevolucion())
        );
    }

    @Test
    void prestamoNormalConFechaMayorATresDias_debeRechazarse() {
        prepararInventarioParaPrestamo(5);
        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> pedidoService.createOrder(crearPedidoPrestamo(LocalDate.now().plusDays(4).toString(), "NORMAL"), usuarioId));

        assertRollbackPrestamo(error, pedidosAntes, controlesAntes, 5);
    }

    @Test
    void prestamoNormalConFechaPasada_debeRechazarse() {
        prepararInventarioParaPrestamo(5);
        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> pedidoService.createOrder(crearPedidoPrestamo(LocalDate.now().minusDays(1).toString(), "NORMAL"), usuarioId));

        assertRollbackPrestamo(error, pedidosAntes, controlesAntes, 5);
    }

    @Test
    void prestamoEspecialClienteNoAutorizado_debeRechazarse() {
        actualizarPrestamoIlimitado(false);
        prepararInventarioParaPrestamo(5);
        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> pedidoService.createOrder(crearPedidoPrestamo(null, "ESPECIAL"), usuarioId));

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode()),
                () -> assertEquals("El cliente no está autorizado para préstamos especiales", error.getReason()),
                () -> assertEquals(5, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(pedidosAntes, pedidoRepository.count()),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count())
        );
    }

    @Test
    void prestamoEspecialClienteAutorizado_debeCrearseSinFechaLimite() {
        actualizarPrestamoIlimitado(true);
        prepararInventarioParaPrestamo(5);

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(
                crearPedidoPrestamo(null, "ESPECIAL"), usuarioId);
        ControlEnvase prestamo = unicoPrestamo(respuesta.idPedido());

        assertAll(
                () -> assertEquals("ESPECIAL", prestamo.getTipoPrestamo()),
                () -> assertNotNull(prestamo.getFechaPrestamo()),
                () -> assertNull(prestamo.getFechaLimiteDevolucion()),
                () -> assertNull(prestamo.getFechaDevolucion())
        );
    }

    @Test
    void prestamoEspecialConFecha_debeRechazarse() {
        actualizarPrestamoIlimitado(true);
        prepararInventarioParaPrestamo(5);
        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> pedidoService.createOrder(crearPedidoPrestamo(LocalDate.now().toString(), "ESPECIAL"), usuarioId));

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode()),
                () -> assertEquals("El préstamo especial no utiliza fecha límite de devolución", error.getReason()),
                () -> assertEquals(5, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(pedidosAntes, pedidoRepository.count()),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count())
        );
    }

    @Test
    void crearPrestamoTipoLegado_debeRechazarse() {
        prepararInventarioParaPrestamo(5);
        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> pedidoService.createOrder(crearPedidoPrestamo(null, "LEGADO"), usuarioId));

        assertRollbackPrestamo(error, pedidosAntes, controlesAntes, 5);
    }

    @Test
    void devolucionPrestamoNormal_noDebeSobrescribirFechaLimite() {
        prepararInventarioParaPrestamo(5);
        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(
                crearPedidoPrestamo(null, "NORMAL"), usuarioId);
        ControlEnvase prestamo = unicoPrestamo(respuesta.idPedido());
        LocalDate fechaLimiteAntes = prestamo.getFechaLimiteDevolucion();

        envaseService.registrarDevolucion(new EnvioEnvaseDTO.DevolucionRequest(prestamo.getId(), 1));

        ControlEnvase devuelto = controlEnvaseRepository.findById(prestamo.getId()).orElseThrow();
        assertAll(
                () -> assertEquals(fechaLimiteAntes, devuelto.getFechaLimiteDevolucion()),
                () -> assertNotNull(devuelto.getFechaDevolucion()),
                () -> assertEquals(1, devuelto.getCantidadDevuelta()),
                () -> assertEquals("SALDADO", devuelto.getEstado())
        );
    }

    @Test
    void prestamoLegadoSinTipoMovimiento_debeAsignarNormalYTresDias() {
        prepararInventarioParaPrestamo(5);

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(new PedidoDTO.Create(
                null, clienteId, null, "Cliente Envase Test", null, null, null, null, usuarioId,
                null, null, null, null, "LOCAL", null, List.of(),
                List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), 1)),
                null, List.of()), usuarioId);
        ControlEnvase prestamo = unicoPrestamo(respuesta.idPedido());

        assertAll(
                () -> assertEquals("NORMAL", prestamo.getTipoPrestamo()),
                () -> assertEquals(prestamo.getFechaPrestamo().toLocalDate().plusDays(3),
                        prestamo.getFechaLimiteDevolucion()),
                () -> assertNull(prestamo.getFechaDevolucion())
        );
    }

    @Test
    void crearCanjeLocal_debeIncrementarVaciosSinDeudaNiDetalleAdicional() {
        prepararInventarioParaPrestamo(4);
        long controlesAntes = controlEnvaseRepository.count();

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(
                crearPedidoCanje(1, List.of()), usuarioId);

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        long detalles = contarDetallesPedido(respuesta.idPedido());
        BigDecimal loteDisponible = stockActualLotes();

        assertAll(
                () -> assertEquals("ENTREGADO", respuesta.estadoPedido()),
                () -> assertEquals(5, producto.getStockVacios()),
                () -> assertEquals(0, producto.getStockLlenos().compareTo(BigDecimal.ZERO)),
                () -> assertEquals(0, loteDisponible.compareTo(BigDecimal.ZERO)),
                () -> assertEquals(1, detalles, "CANJE no debe crear un detalle de envase adicional"),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count(),
                        "CANJE no debe crear deuda de envase"),
                () -> assertEquals(0, respuesta.montoTotal().compareTo(new BigDecimal("10.00")))
        );
    }

    @Test
    void crearCanjeLocal_cantidadN_debeIncrementarVaciosYDescontarLlenosUnaSolaVez() {
        prepararInventarioParaPrestamo(4, 2);
        long controlesAntes = controlEnvaseRepository.count();

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(
                crearPedidoCanje(2, List.of()), usuarioId);

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        long detalles = contarDetallesPedido(respuesta.idPedido());
        BigDecimal loteDisponible = stockActualLotes();

        assertAll(
                () -> assertEquals(6, producto.getStockVacios()),
                () -> assertEquals(0, producto.getStockLlenos().compareTo(BigDecimal.ZERO)),
                () -> assertEquals(0, loteDisponible.compareTo(BigDecimal.ZERO)),
                () -> assertEquals(1, detalles, "La venta de gas debe persistirse una sola vez"),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count()),
                () -> assertEquals(0, respuesta.montoTotal().compareTo(new BigDecimal("20.00")))
        );
    }

    @Test
    void crearCanjeLocal_falloPosteriorDebeRevertirVaciosYVenta() {
        prepararInventarioParaPrestamo(4);
        long pedidosAntes = pedidoRepository.count();
        long controlesAntes = controlEnvaseRepository.count();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> pedidoService.createOrder(crearPedidoCanje(1,
                        List.of(new PedidoDTO.PagoCreate(999_999L, new BigDecimal("10.00"), null))), usuarioId));

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        BigDecimal loteDisponible = stockActualLotes();

        assertAll(
                () -> assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode()),
                () -> assertEquals(4, producto.getStockVacios()),
                () -> assertEquals(0, loteDisponible.compareTo(BigDecimal.ONE)),
                () -> assertEquals(pedidosAntes, pedidoRepository.count()),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count())
        );
    }

    @Test
    void crearCanjeDomicilio_debeReservarYPersistirCanjeSinIncrementarVacios() {
        prepararInventarioParaPrestamo(4);
        long controlesAntes = controlEnvaseRepository.count();

        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(crearPedidoCanjeDomicilio(1), usuarioId);
        Producto producto = productoRepository.findById(productoId).orElseThrow();
        DetallePedido detalle = unicoDetalle(respuesta.idPedido());

        assertAll(
                () -> assertEquals("PENDIENTE", respuesta.estadoPedido()),
                () -> assertEquals(4, producto.getStockVacios()),
                () -> assertEquals(0, producto.getStockReservado().compareTo(BigDecimal.ONE)),
                () -> assertEquals(1, detalle.getCantidadCanje()),
                () -> assertEquals(1, contarDetallesPedido(respuesta.idPedido())),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count()),
                () -> assertEquals(0, respuesta.montoTotal().compareTo(new BigDecimal("10.00")))
        );
    }

    @Test
    void entregarCanjeDomicilio_q1YSegundaConfirmacion_debeAplicarUnaSolaVez() {
        prepararInventarioParaPrestamo(4);
        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(crearPedidoCanjeDomicilio(1), usuarioId);
        avanzarHastaDomicilio(respuesta.idPedido());

        PedidoPagoYapeDTO pago = new PedidoPagoYapeDTO(
                respuesta.idPedido(), efectivoId, new BigDecimal("10.00"), "");
        pedidoPagosService.confirmarEntregaPagoUnico(pago, null, null);
        pedidoPagosService.confirmarEntregaPagoUnico(pago, null, null);

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        Pedido pedido = pedidoRepository.findById(respuesta.idPedido()).orElseThrow();
        DetallePedido detalle = unicoDetalle(respuesta.idPedido());
        assertAll(
                () -> assertEquals("ENTREGADO", pedido.getEstadoPedido()),
                () -> assertEquals(5, producto.getStockVacios()),
                () -> assertEquals(0, detalle.getCantidadCanje()),
                () -> assertEquals(1, pedidoPagoRepository.findByPedido_Id(respuesta.idPedido()).size())
        );
    }

    @Test
    void entregarCanjeDomicilio_cantidadNConPagoMixto_debeSumarExactamenteN() {
        prepararInventarioParaPrestamo(4, 3);
        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(crearPedidoCanjeDomicilio(3), usuarioId);
        avanzarHastaDomicilio(respuesta.idPedido());

        pedidoPagosService.confirmarEntregaConPagos(new ConfirmarEntregaMixtaDTO(
                respuesta.idPedido(),
                List.of(
                        new PagoRegistroDTO(efectivoId, new BigDecimal("10.00"), "", BigDecimal.ZERO),
                        new PagoRegistroDTO(yapeId, new BigDecimal("20.00"), "op-yape", BigDecimal.ZERO))),
                null, null);

        assertAll(
                () -> assertEquals(7, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(0, unicoDetalle(respuesta.idPedido()).getCantidadCanje()),
                () -> assertEquals(2, pedidoPagoRepository.findByPedido_Id(respuesta.idPedido()).size())
        );
    }

    @Test
    void entregarCanjeDomicilio_multiplesProductos_debeAplicarCadaCantidad() {
        prepararInventarioParaPrestamo(4, 2);
        Long productoDosId = crearSegundoProductoConLote(7, 3);
        PedidoDTO.Create dto = crearPedidoCanjeDomicilio(
                List.of(
                        new PedidoDTO.DetalleCreate(productoId, 2, new BigDecimal("10.00"), null),
                        new PedidoDTO.DetalleCreate(productoDosId, 3, new BigDecimal("10.00"), null)),
                List.of(
                        new PedidoDTO.EnvaseMovimientoCreate(productoId, 2, null, null, null, null),
                        new PedidoDTO.EnvaseMovimientoCreate(productoDosId, 3, null, null, null, null)));
        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(dto, usuarioId);
        avanzarHastaDomicilio(respuesta.idPedido());

        pedidoPagosService.confirmarEntregaPagoUnico(new PedidoPagoYapeDTO(
                respuesta.idPedido(), efectivoId, new BigDecimal("50.00"), ""), null, null);

        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_Id(respuesta.idPedido());
        assertAll(
                () -> assertEquals(6, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(10, productoRepository.findById(productoDosId).orElseThrow().getStockVacios()),
                () -> assertEquals(2, detalles.size()),
                () -> assertTrue(detalles.stream()
                        .allMatch(detalle -> Integer.valueOf(0).equals(detalle.getCantidadCanje())))
        );
    }

    @Test
    void entregarOtrosMovimientosDomicilio_noDebeSumarVaciosAutomaticamente() {
        prepararInventarioParaPrestamo(10, 3);

        PedidoDTO.SimpleResponse ninguno = pedidoService.createOrder(crearPedidoMovimientoDomicilio("NINGUNO"), usuarioId);
        avanzarHastaDomicilio(ninguno.idPedido());
        pedidoPagosService.confirmarEntregaPagoUnico(new PedidoPagoYapeDTO(
                ninguno.idPedido(), efectivoId, new BigDecimal("10.00"), ""), null, null);
        assertEquals(10, productoRepository.findById(productoId).orElseThrow().getStockVacios());

        PedidoDTO.SimpleResponse venta = pedidoService.createOrder(crearPedidoMovimientoDomicilio("VENTA"), usuarioId);
        colocarEnDomicilioParaProbarEntrega(venta.idPedido());
        pedidoPagosService.confirmarEntregaPagoUnico(new PedidoPagoYapeDTO(
                venta.idPedido(), efectivoId, new BigDecimal("20.00"), ""), null, null);
        assertEquals(9, productoRepository.findById(productoId).orElseThrow().getStockVacios());

        PedidoDTO.SimpleResponse prestamo = pedidoService.createOrder(crearPedidoMovimientoDomicilio("PRESTAMO"), usuarioId);
        avanzarHastaDomicilio(prestamo.idPedido());
        pedidoPagosService.confirmarEntregaPagoUnico(new PedidoPagoYapeDTO(
                prestamo.idPedido(), efectivoId, new BigDecimal("10.00"), ""), null, null);

        assertAll(
                () -> assertEquals(8, productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(1, controlEnvaseRepository.findByPedido_Id(prestamo.idPedido()).size()),
                () -> assertEquals(0, unicoDetalle(ninguno.idPedido()).getCantidadCanje()),
                () -> assertEquals(0, unicoDetalle(prestamo.idPedido()).getCantidadCanje())
        );
    }

    @Test
    void confirmarCanjeDomicilio_falloPosteriorDebeRevertirEntregaPagoYCanje() {
        prepararInventarioParaPrestamo(Integer.MAX_VALUE);
        PedidoDTO.SimpleResponse respuesta = pedidoService.createOrder(crearPedidoCanjeDomicilio(1), usuarioId);
        avanzarHastaDomicilio(respuesta.idPedido());

        assertThrows(ArithmeticException.class, () -> pedidoPagosService.confirmarEntregaPagoUnico(
                new PedidoPagoYapeDTO(respuesta.idPedido(), efectivoId, new BigDecimal("10.00"), ""),
                null, null));

        assertAll(
                () -> assertEquals("EN_DOMICILIO",
                        pedidoRepository.findById(respuesta.idPedido()).orElseThrow().getEstadoPedido()),
                () -> assertEquals(Integer.MAX_VALUE,
                        productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(1, unicoDetalle(respuesta.idPedido()).getCantidadCanje()),
                () -> assertEquals(0, pedidoPagoRepository.findByPedido_Id(respuesta.idPedido()).size())
        );
    }

    private void prepararInventarioParaPrestamo(int stockVacios) {
        prepararInventarioParaPrestamo(stockVacios, 1);
    }

    private void prepararInventarioParaPrestamo(int stockVacios, int cantidadLote) {
        String rucUnico = String.format("20%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
        transactionTemplate.executeWithoutResult(status -> {
            List<Correlativo> correlativos = entityManager.createQuery(
                    "FROM Correlativo c WHERE c.tipo = :tipo AND c.serie = :serie", Correlativo.class)
                    .setParameter("tipo", "VENTA_NOTA")
                    .setParameter("serie", "NV001")
                    .getResultList();
            if (correlativos.isEmpty()) {
                Correlativo correlativo = new Correlativo();
                correlativo.setTipo("VENTA_NOTA");
                correlativo.setSerie("NV001");
                correlativo.setNumeroActual(0);
                entityManager.persist(correlativo);
            }

            Producto producto = productoRepository.findById(productoId).orElseThrow();
            producto.setStockVacios(stockVacios);

            Rubro rubro = new Rubro();
            rubro.setNombre("Rubro fecha préstamo " + rucUnico);
            rubro.setEstado(1);
            entityManager.persist(rubro);

            Proveedor proveedor = new Proveedor();
            proveedor.setNombre("Proveedor fecha préstamo " + rucUnico);
            proveedor.setTelefono("999999999");
            proveedor.setCorreo("proveedor.fecha." + rucUnico + "@test.com");
            proveedor.setRuc(rucUnico);
            proveedor.setEstado(1);
            proveedor.setRubro(rubro);
            entityManager.persist(proveedor);

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
            compra.setFechaCompra(LocalDateTime.now());
            compra.setMontoTotal(new BigDecimal("10.00"));
            compra.setSituacion(1);
            entityManager.persist(compra);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.valueOf(cantidadLote));
            lote.setCantidadActual(BigDecimal.valueOf(cantidadLote));
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
        });
    }

    private PedidoDTO.Create crearPedidoPrestamo(String fechaLimiteDevolucion, String tipoPrestamo) {
        return new PedidoDTO.Create(
                null, clienteId, null, "Cliente Envase Test", null, null, null, null, usuarioId,
                null, null, null, null, "LOCAL", null, List.of(),
                List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), null)),
                "PRESTAMO",
                List.of(new PedidoDTO.EnvaseMovimientoCreate(
                        productoId, 1, null, fechaLimiteDevolucion, null, tipoPrestamo)));
    }

    private PedidoDTO.Create crearPedidoCanje(int cantidad, List<PedidoDTO.PagoCreate> pagos) {
        return new PedidoDTO.Create(
                null, clienteId, null, "Cliente Envase Test", null, null, null, null, usuarioId,
                null, null, null, null, "LOCAL", null, pagos,
                List.of(new PedidoDTO.DetalleCreate(productoId, cantidad, new BigDecimal("10.00"), null)),
                "CANJE",
                List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, cantidad, null, null, null, null)));
    }

    private PedidoDTO.Create crearPedidoCanjeDomicilio(int cantidad) {
        return crearPedidoCanjeDomicilio(
                List.of(new PedidoDTO.DetalleCreate(
                        productoId, cantidad, new BigDecimal("10.00"), null)),
                List.of(new PedidoDTO.EnvaseMovimientoCreate(
                        productoId, cantidad, null, null, null, null)));
    }

    private PedidoDTO.Create crearPedidoCanjeDomicilio(
            List<PedidoDTO.DetalleCreate> detalles,
            List<PedidoDTO.EnvaseMovimientoCreate> movimientos) {
        return new PedidoDTO.Create(
                null, clienteId, null, "Cliente Envase Test", null, null, null, empleadoId, usuarioId,
                null, null, null, null, "DOMICILIO", null, List.of(), detalles, "CANJE", movimientos);
    }

    private PedidoDTO.Create crearPedidoMovimientoDomicilio(String tipoMovimiento) {
        List<PedidoDTO.EnvaseMovimientoCreate> movimientos;
        if ("VENTA".equals(tipoMovimiento)) {
            movimientos = List.of(new PedidoDTO.EnvaseMovimientoCreate(
                    productoId, 1, new BigDecimal("10.00"), null, null, null));
        } else if ("PRESTAMO".equals(tipoMovimiento)) {
            movimientos = List.of(new PedidoDTO.EnvaseMovimientoCreate(
                    productoId, 1, null, null, null, "NORMAL"));
        } else {
            movimientos = List.of();
        }
        return new PedidoDTO.Create(
                null, clienteId, null, "Cliente Envase Test", null, null, null, empleadoId, usuarioId,
                null, null, null, null, "DOMICILIO", null, List.of(),
                List.of(new PedidoDTO.DetalleCreate(productoId, 1, new BigDecimal("10.00"), null)),
                tipoMovimiento, movimientos);
    }

    private void avanzarHastaDomicilio(Long idPedido) {
        pedidoService.updateEstadoPedido(idPedido, "ACEPTADO");
        pedidoService.updateEstadoPedido(idPedido, "CARGADO");
        pedidoService.updateEstadoPedido(idPedido, "EN_CAMINO");
        pedidoService.updateEstadoPedido(idPedido, "EN_DOMICILIO");
    }

    private void colocarEnDomicilioParaProbarEntrega(Long idPedido) {
        transactionTemplate.executeWithoutResult(status -> {
            Pedido pedido = pedidoRepository.findById(idPedido).orElseThrow();
            pedido.setEstadoPedido("EN_DOMICILIO");
            pedidoRepository.saveAndFlush(pedido);
        });
    }

    private Long crearSegundoProductoConLote(int stockVacios, int cantidadLote) {
        return transactionTemplate.execute(status -> {
            Categoria categoria = entityManager.createQuery("FROM Categoria", Categoria.class)
                    .getResultList().get(0);
            Proveedor proveedor = entityManager.createQuery("FROM Proveedor", Proveedor.class)
                    .getResultList().get(0);
            Compra compra = entityManager.createQuery("FROM Compra", Compra.class)
                    .getResultList().get(0);

            Producto producto = new Producto();
            producto.setNombre("Envase Test Secundario " + UUID.randomUUID());
            producto.setPrecioCompra(new BigDecimal("10.00"));
            producto.setGananciaProducto(BigDecimal.ZERO);
            producto.setPrecioVenta(new BigDecimal("10.00"));
            producto.setStockLlenos(BigDecimal.valueOf(cantidadLote));
            producto.setStockMinimo(BigDecimal.ZERO);
            producto.setStockVacios(stockVacios);
            producto.setStockReservado(BigDecimal.ZERO);
            producto.setRequiereEnvase(true);
            producto.setEstado(1);
            producto.setCategoria(categoria);
            entityManager.persist(producto);

            InventarioLote lote = new InventarioLote();
            lote.setProducto(producto);
            lote.setProveedor(proveedor);
            lote.setCompra(compra);
            lote.setCantidadInicial(BigDecimal.valueOf(cantidadLote));
            lote.setCantidadActual(BigDecimal.valueOf(cantidadLote));
            lote.setPrecioCompra(new BigDecimal("10.00"));
            lote.setPrecioVenta(new BigDecimal("10.00"));
            entityManager.persist(lote);
            entityManager.flush();
            return producto.getId();
        });
    }

    private DetallePedido unicoDetalle(Long idPedido) {
        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_Id(idPedido);
        assertEquals(1, detalles.size());
        return detalles.get(0);
    }

    private long contarDetallesPedido(Long idPedido) {
        return transactionTemplate.execute(status -> entityManager.createQuery(
                "SELECT COUNT(d) FROM DetallePedido d WHERE d.pedido.id = :idPedido", Long.class)
                .setParameter("idPedido", idPedido)
                .getSingleResult());
    }

    private BigDecimal stockActualLotes() {
        return transactionTemplate.execute(status -> entityManager.createQuery(
                "SELECT COALESCE(SUM(i.cantidadActual), 0) FROM InventarioLote i "
                        + "WHERE i.producto.id = :idProducto", BigDecimal.class)
                .setParameter("idProducto", productoId)
                .getSingleResult());
    }

    private ControlEnvase unicoPrestamo(Long idPedido) {
        List<ControlEnvase> controles = controlEnvaseRepository.findByPedido_Id(idPedido);
        assertEquals(1, controles.size());
        return controles.get(0);
    }

    private void actualizarPrestamoIlimitado(boolean prestamoIlimitado) {
        transactionTemplate.executeWithoutResult(status -> {
            Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
            cliente.setPrestamoIlimitado(prestamoIlimitado);
            clienteRepository.saveAndFlush(cliente);
        });
    }

    private void assertRollbackPrestamo(ResponseStatusException error, long pedidosAntes,
            long controlesAntes, int stockVaciosEsperado) {
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode()),
                () -> assertEquals(stockVaciosEsperado,
                        productoRepository.findById(productoId).orElseThrow().getStockVacios()),
                () -> assertEquals(pedidosAntes, pedidoRepository.count()),
                () -> assertEquals(controlesAntes, controlEnvaseRepository.count())
        );
    }
}
