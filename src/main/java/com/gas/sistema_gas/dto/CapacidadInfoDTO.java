package com.gas.sistema_gas.dto;

import java.math.BigDecimal;

public class CapacidadInfoDTO {
    private BigDecimal valor;
    private boolean enUso;

    public CapacidadInfoDTO(BigDecimal valor, boolean enUso) {
        this.valor = valor;
        this.enUso = enUso;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean isEnUso() {
        return enUso;
    }
}