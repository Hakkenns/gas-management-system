package com.gas.sistema_gas.service.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.gas.sistema_gas.Mapper.CatalogoProveedorMapper;
import com.gas.sistema_gas.Repository.CatalogoProveedorRepository;
import com.gas.sistema_gas.Repository.ProductoRepository;
import com.gas.sistema_gas.Repository.ProveedorRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoProveedorServiceImplementTest {

    @Mock
    private CatalogoProveedorRepository repository;
    @Mock
    private CatalogoProveedorMapper mapper;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private CatalogoProveedorServiceImplement service;

    @Test
    void buscarProveedores_sinTexto_usaLimiteDefaultYMapeaRespuesta() {
        when(repository.buscarProveedoresActivos(eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<Object[]>(List.<Object[]>of(new Object[] { 7L, "20123456789", "Vitagas", 15L })));

        var result = service.buscarProveedores(null, null);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).idProveedor());
        assertEquals(15L, result.get(0).cantidadProductos());
        verify(repository).buscarProveedoresActivos("", Pageable.ofSize(30));
    }

    @Test
    void buscarProveedores_limiteMayorAlMaximo_normalizaA50() {
        when(repository.buscarProveedoresActivos(eq("manguera"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, service.buscarProveedores("manguera", 500).size());
        verify(repository).buscarProveedoresActivos("manguera", Pageable.ofSize(50));
    }
}
