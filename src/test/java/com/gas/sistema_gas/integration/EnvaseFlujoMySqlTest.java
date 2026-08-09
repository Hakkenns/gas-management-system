package com.gas.sistema_gas.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import com.gas.sistema_gas.SistemaGasApplication;
import com.gas.sistema_gas.Model.Categoria;
import com.gas.sistema_gas.Model.Cliente;
import com.gas.sistema_gas.Model.ControlEnvase;
import com.gas.sistema_gas.Model.Correlativo;
import com.gas.sistema_gas.Model.Compra;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Model.Rubro;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.service.PedidoService;
import com.gas.sistema_gas.dto.PedidoDTO;

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
    private ControlEnvaseRepository controlEnvaseRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private Long pedidoId;
    private Long productoId;
    private Long clienteId;
    private Long usuarioId;

    @BeforeEach
    void prepararFixture() {
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
            controlEnvase.setProducto(producto);
            controlEnvase.setCliente(cliente);
            controlEnvase.setPedido(pedido);
            controlEnvaseRepository.saveAndFlush(controlEnvase);

            pedidoId = pedido.getId();
            productoId = producto.getId();
            clienteId = cliente.getId();
            usuarioId = usuario.getId();
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
                    List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, 2, null, null, null))
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
                List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, 2, new BigDecimal("10.00"), null, null))
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
                    List.of(new PedidoDTO.EnvaseMovimientoCreate(productoId, 2, new BigDecimal("10.00"), null, null))
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
}
