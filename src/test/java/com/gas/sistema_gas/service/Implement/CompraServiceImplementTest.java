package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gas.sistema_gas.Mapper.CompraMapper;
import com.gas.sistema_gas.Model.Compra;
import com.gas.sistema_gas.Model.DetalleCompra;
import com.gas.sistema_gas.Model.InventarioLote;
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Model.Producto;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.CompraRepository;
import com.gas.sistema_gas.Repository.CatalogoProveedorRepository;
import com.gas.sistema_gas.Repository.DetalleCompraRepository;
import com.gas.sistema_gas.Repository.InventarioLoteRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.ProveedorRepository;
import com.gas.sistema_gas.Repository.UsuarioRepository;
import com.gas.sistema_gas.dto.CompraDTO;
import com.gas.sistema_gas.service.CorrelativoService;
import com.gas.sistema_gas.service.InventarioLoteService;

@ExtendWith(MockitoExtension.class)
class CompraServiceImplementTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private DetalleCompraRepository detalleCompraRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CompraMapper compraMapper;

    @Mock
    private CorrelativoService correlativoService;

    @Mock
    private InventarioLoteService inventarioLoteService;

    @Mock
    private CatalogoProveedorRepository catalogoProveedorRepository;

    @Mock
    private InventarioLoteRepository inventarioLoteRepository;

    @InjectMocks
    private CompraServiceImplement compraService;

    @Test
    void create_ignoraFechaEnviadaYUsaFechaActualDelServidor() {
        CompraDTO.Create request = new CompraDTO.Create(
            10L,
            "NC-CLIENTE-FUTURO",
            "2000-01-01T00:00",
            BigDecimal.TEN,
            List.of()
        );
        Proveedor proveedor = new Proveedor();
        proveedor.setId(10L);
        Usuario usuario = new Usuario();
        Compra compra = new Compra();
        CompraDTO.SimpleResponse response = new CompraDTO.SimpleResponse(
            1L, "NC001-0001", "Proveedor", BigDecimal.TEN,
            "01/01/2026 10:00", "Usuario", 1
        );

        when(proveedorRepository.findById(10L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(compraMapper.toEntity(request)).thenReturn(compra);
        when(correlativoService.incrementarYObtenerCodigo("COMPRA_NOTA", "NC001"))
            .thenReturn("NC001-0001");
        when(compraRepository.save(compra)).thenReturn(compra);
        when(compraMapper.toSimpleResponse(compra)).thenReturn(response);

        LocalDateTime antes = LocalDateTime.now();
        compraService.create(request, 20L);
        LocalDateTime despues = LocalDateTime.now();

        assertTrue(!compra.getFechaCompra().isBefore(antes)
            && !compra.getFechaCompra().isAfter(despues));
        verify(compraRepository).save(compra);
    }

    @Test
    void create_rechazaProductoFueraDelCatalogoDelProveedor() {
        CompraDTO.Create request = new CompraDTO.Create(
            10L, "NC-CLIENTE", null, BigDecimal.TEN,
            List.of(new CompraDTO.DetalleItem(99L, 1, BigDecimal.ONE))
        );
        Proveedor proveedor = new Proveedor();
        proveedor.setId(10L);
        Usuario usuario = new Usuario();
        when(proveedorRepository.findById(10L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(catalogoProveedorRepository.existsRelacionActiva(10L, 99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> compraService.create(request, 20L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El producto seleccionado no pertenece al catÃ¡logo activo del proveedor.", exception.getReason());
        verify(compraRepository, never()).save(any());
        verify(detalleCompraRepository, never()).save(any());
        verify(inventarioLoteService, never()).registrarLote(any());
        verify(correlativoService, never()).incrementarYObtenerCodigo(any(), any());
    }

    @Test
    void getDetalleByCompraId_devuelveDatosHistoricosYMultiplesProductos() {
        Compra compra = new Compra();
        compra.setId(7L);
        compra.setNumDocumento("NC001-0053");
        compra.setFechaCompra(LocalDateTime.of(2026, 8, 19, 11, 21, 34));
        compra.setSituacion(2);
        compra.setMontoTotal(new BigDecimal("772.00"));
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("AGUACIX");
        compra.setProveedor(proveedor);
        Usuario usuario = new Usuario();
        usuario.setUserName("admin_zair");
        compra.setUsuario(usuario);

        Producto bidon = new Producto();
        bidon.setId(10L);
        bidon.setNombre("Bidón Agua 20 L");
        bidon.setUnidadMedida("L");
        bidon.setCapacidad(new BigDecimal("20.00"));
        DetalleCompra detalleBidon = detalle(compra, bidon, 1, new BigDecimal("20.00"));

        Producto balon = new Producto();
        balon.setId(11L);
        balon.setNombre("Balón de Gas GLP");
        balon.setUnidadMedida("KG");
        balon.setCapacidad(new BigDecimal("10.00"));
        DetalleCompra detalleBalon = detalle(compra, balon, 10, new BigDecimal("50.00"));

        Producto sinCapacidad = new Producto();
        sinCapacidad.setId(12L);
        sinCapacidad.setNombre("Producto sin capacidad");
        sinCapacidad.setUnidadMedida("UND");
        DetalleCompra detalleSinCapacidad = detalle(compra, sinCapacidad, 3, new BigDecimal("4.00"));

        Producto manguera = new Producto();
        manguera.setId(20L);
        manguera.setNombre("Manguera X");
        manguera.setUnidadMedida("M");
        manguera.setCapacidad(new BigDecimal("99.00"));
        DetalleCompra detalleManguera = detalle(compra, manguera, 120, new BigDecimal("2.00"));
        InventarioLote loteManguera = new InventarioLote();
        loteManguera.setProducto(manguera);
        loteManguera.setMetrosPorRollo(new BigDecimal("60.00"));

        when(compraRepository.findById(7L)).thenReturn(Optional.of(compra));
        when(detalleCompraRepository.findByCompraId(7L))
            .thenReturn(List.of(detalleBidon, detalleBalon, detalleSinCapacidad, detalleManguera));
        when(inventarioLoteRepository.findByCompraId(7L)).thenReturn(List.of(loteManguera));

        CompraDTO.DetailResponse response = compraService.getDetalleByCompraId(7L);

        assertEquals(7L, response.idCompra());
        assertEquals("NC001-0053", response.numDocumento());
        assertEquals("AGUACIX", response.proveedor());
        assertEquals("admin_zair", response.usuario());
        assertEquals(LocalDateTime.of(2026, 8, 19, 11, 21, 34), response.fechaCompra());
        assertEquals(2, response.situacion());
        assertEquals(new BigDecimal("772.00"), response.montoTotal());
        assertEquals(4, response.detalles().size());
        assertEquals(new BigDecimal("20.00"), response.detalles().get(0).capacidad());
        assertEquals(1, response.detalles().get(0).cantidad());
        assertEquals(new BigDecimal("20.00"), response.detalles().get(0).precioCostoUnitario());
        assertEquals(new BigDecimal("20.00"), response.detalles().get(0).subtotal());
        assertEquals(new BigDecimal("10.00"), response.detalles().get(1).capacidad());
        assertEquals(10, response.detalles().get(1).cantidad());
        assertEquals(new BigDecimal("50.00"), response.detalles().get(1).precioCostoUnitario());
        assertEquals(new BigDecimal("500.00"), response.detalles().get(1).subtotal());
        assertNull(response.detalles().get(2).capacidad());
        assertEquals(3, response.detalles().get(2).cantidad());
        assertEquals(120, response.detalles().get(3).cantidad());
        assertEquals(new BigDecimal("2.00"), response.detalles().get(3).precioCostoUnitario());
        assertEquals(new BigDecimal("240.00"), response.detalles().get(3).subtotal());
        assertEquals(new BigDecimal("60.00"), response.detalles().get(3).metrosPorRollo());
    }

    @Test
    void getDetalleByCompraId_inexistente_lanzaNotFound() {
        when(compraRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> compraService.getDetalleByCompraId(99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(detalleCompraRepository, never()).findByCompraId(99L);
        verify(inventarioLoteRepository, never()).findByCompraId(99L);
    }

    // PRUEBA: anularCompra con ID nulo lanza BadRequest
    @Test
    void anularCompra_idNulo_lanzaBadRequest() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> compraService.anularCompra(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El ID de la compra es obligatorio", exception.getReason());
        verify(compraRepository, never()).findByIdForUpdate(any());
        verify(inventarioLoteService, never()).anularLotesCompra(any());
        verify(compraRepository, never()).save(any());
        verify(productoRepository, never()).findAllByIdInForUpdate(any());
        verify(detalleCompraRepository, never()).findByCompraId(any());
    }

    // PRUEBA: anularCompra bloquea Compra antes de anular inventario
    @Test
    void anularCompra_bloqueaCompraAntesDeAnularInventario() {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setSituacion(1);

        when(compraRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(compra));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));
        when(compraMapper.toSimpleResponse(any(Compra.class)))
            .thenReturn(new CompraDTO.SimpleResponse(1L, "NC001", "Proveedor", BigDecimal.TEN, "01/01/2026 10:00", "Usuario", 2));

        compraService.anularCompra(1L);

        InOrder inOrder = inOrder(compraRepository, inventarioLoteService);
        inOrder.verify(compraRepository).findByIdForUpdate(1L);
        inOrder.verify(inventarioLoteService).anularLotesCompra(1L);
        inOrder.verify(compraRepository).save(any(Compra.class));
    }

    // PRUEBA: anularCompra ya anulada lanza Conflict
    @Test
    void anularCompra_yaAnulada_lanzaConflict() {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setSituacion(2);

        when(compraRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(compra));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> compraService.anularCompra(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("La compra ya está anulada", exception.getReason());
        verify(inventarioLoteService, never()).anularLotesCompra(any());
        verify(compraRepository, never()).save(any());
        verify(detalleCompraRepository, never()).delete(any());
    }

    // PRUEBA: anularCompra inventario falla no marca compra anulada
    @Test
    void anularCompra_inventarioFalla_noMarcaCompraAnulada() {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setSituacion(1);

        when(compraRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(compra));
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Inconsistencia de inventario"))
            .when(inventarioLoteService).anularLotesCompra(1L);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> compraService.anularCompra(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(1, compra.getSituacion());
        verify(compraRepository, never()).save(any());
        verify(detalleCompraRepository, never()).delete(any());
    }

    // PRUEBA: anularCompra valida marca situacion dos
    @Test
    void anularCompra_valida_marcaSituacionDos() {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setSituacion(1);

        when(compraRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(compra));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));
        when(compraMapper.toSimpleResponse(any(Compra.class)))
            .thenReturn(new CompraDTO.SimpleResponse(1L, "NC001", "Proveedor", BigDecimal.TEN, "01/01/2026 10:00", "Usuario", 2));

        CompraDTO.SimpleResponse response = compraService.anularCompra(1L);

        assertEquals(2, compra.getSituacion());
        assertEquals(2, response.situacion());
        verify(inventarioLoteService, times(1)).anularLotesCompra(1L);
        verify(compraRepository, times(1)).save(any(Compra.class));
    }

    // PRUEBA: anularCompra valida conserva detalles
    @Test
    void anularCompra_valida_conservaDetalles() {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setSituacion(1);

        when(compraRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(compra));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));
        when(compraMapper.toSimpleResponse(any(Compra.class)))
            .thenReturn(new CompraDTO.SimpleResponse(1L, "NC001", "Proveedor", BigDecimal.TEN, "01/01/2026 10:00", "Usuario", 2));

        compraService.anularCompra(1L);

        verify(detalleCompraRepository, never()).delete(any(DetalleCompra.class));
        verify(detalleCompraRepository, never()).deleteAll(any());
        verify(detalleCompraRepository, never()).findByCompraId(any());
        verify(detalleCompraRepository, never()).deleteAll();
    }

    private DetalleCompra detalle(Compra compra, Producto producto, int cantidad, BigDecimal precio) {
        DetalleCompra detalle = new DetalleCompra();
        detalle.setCompra(compra);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioCostoUnitario(precio);
        return detalle;
    }
}
