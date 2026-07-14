package com.gas.sistema_gas.dto;

import java.math.BigDecimal;

public class PedidoPagoYapeDTO {
    public Long idPedido;
    public Long idMetodo;
    public BigDecimal montoRecibido;
    public String numOperacion;

    public PedidoPagoYapeDTO() {}

    public PedidoPagoYapeDTO(Long idPedido, Long idMetodo, BigDecimal montoRecibido, String numOperacion) {
        this.idPedido = idPedido;
        this.idMetodo = idMetodo;
        this.montoRecibido = montoRecibido;
        this.numOperacion = numOperacion;
    }
}
