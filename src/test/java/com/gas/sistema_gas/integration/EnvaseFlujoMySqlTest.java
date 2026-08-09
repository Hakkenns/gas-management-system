package com.gas.sistema_gas.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.Perfil;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.ClienteRepository;
import com.gas.sistema_gas.Repository.ControlEnvaseRepository;
import com.gas.sistema_gas.Repository.PedidoRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.service.PedidoService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
}
