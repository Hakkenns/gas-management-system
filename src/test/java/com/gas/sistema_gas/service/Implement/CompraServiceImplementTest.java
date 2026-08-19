package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.gas.sistema_gas.Model.Proveedor;
import com.gas.sistema_gas.Model.Usuario;
import com.gas.sistema_gas.Repository.CompraRepository;
import com.gas.sistema_gas.Repository.DetalleCompraRepository;
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
}
