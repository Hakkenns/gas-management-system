package com.gas.sistema_gas.dto;

import java.math.BigDecimal;

public class PagoRegistroDTO {
    public Long idMetodo;
    public BigDecimal monto;
    public String numOperacion;
    public BigDecimal vuelto;

    public PagoRegistroDTO() {}

    public PagoRegistroDTO(Long idMetodo, BigDecimal monto, String numOperacion, BigDecimal vuelto) {
        this.idMetodo = idMetodo;
        this.monto = monto;
        this.numOperacion = numOperacion;
        this.vuelto = vuelto;
    }
}