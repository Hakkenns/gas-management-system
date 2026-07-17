package com.gas.sistema_gas.dto;

import java.util.List;

public class ConfirmarEntregaMixtaDTO {
    public Long idPedido;
    public List<PagoRegistroDTO> pagos;

    public ConfirmarEntregaMixtaDTO() {}

    public ConfirmarEntregaMixtaDTO(Long idPedido, List<PagoRegistroDTO> pagos) {
        this.idPedido = idPedido;
        this.pagos = pagos;
    }
}