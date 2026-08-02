package com.gas.sistema_gas.service;



import java.util.List;
import com.gas.sistema_gas.dto.InventarioLoteDTO;
import com.gas.sistema_gas.Model.DetallePedido;

public interface InventarioLoteService {

    // 1. Para cuando se registra una Factura de Compra (Crea un nuevo Lote)
    InventarioLoteDTO.SimpleResponse registrarLote(InventarioLoteDTO.Create createDto);

    // 2. Para cuando el administrador edita el Precio de Venta por la competencia en el modal
    InventarioLoteDTO.SimpleResponse actualizarPrecioVenta(Long idLote, InventarioLoteDTO.Update updateDto);

    // 3. Para alimentar el NUEVO MODAL de desglose: Trae el historial de lotes de un producto específico
    List<InventarioLoteDTO.SimpleResponse> listarLotesPorProducto(Long idProducto);

    // 4. 🧠 EL MOTOR PEPS (Interno): Descuenta las unidades de los lotes más antiguos al vender
    void descontarStockPorPEPS(DetallePedido detallePedido);

    // 5. Para cuando se anula una compra y hay lotes asociados a esa factura
    void anularLotesCompra(Long idCompra);

    // 6. FASE 2A: Devolución exacta de stock de un pedido
    void devolverStockDePedido(Long idPedido);

    // 7. FASE 2A: Validar que el stock fue devuelto para reactivar pedido
    void validarStockDevueltoParaReactivacion(Long idPedido);
}
