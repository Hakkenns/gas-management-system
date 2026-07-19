package com.gas.sistema_gas.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gas.sistema_gas.Model.Evidencia;
import com.gas.sistema_gas.Model.Pedido;
import com.gas.sistema_gas.Model.PedidoPago;
import com.gas.sistema_gas.Repository.EvidenciaRepository;

@WebMvcTest(VentaController.class)
class EvidenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvidenciaRepository evidenciaRepository;

    @Test
    void shouldReturnEvidenceGroupedByType() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId(7L);

        PedidoPago pago = new PedidoPago();
        pago.setId(10L);
        pago.setPedido(pedido);
        pago.setNumOperacion("123456");

        Evidencia evidenciaPago = new Evidencia();
        evidenciaPago.setId(1L);
        evidenciaPago.setPedidoPago(pago);
        evidenciaPago.setTipoEvidencia("PAGO");
        evidenciaPago.setUrlImagen("/images/pago.jpg");

        Evidencia evidenciaVuelto = new Evidencia();
        evidenciaVuelto.setId(2L);
        evidenciaVuelto.setPedidoPago(pago);
        evidenciaVuelto.setTipoEvidencia("VUELTO");
        evidenciaVuelto.setUrlImagen("/images/vuelto.jpg");

        when(evidenciaRepository.findByPedidoPago_Id(7L)).thenReturn(List.of(evidenciaPago, evidenciaVuelto));

        mockMvc.perform(get("/api/evidencias/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(7))
                .andExpect(jsonPath("$.pago[0].urlImagen").value("/images/pago.jpg"))
                .andExpect(jsonPath("$.pago[0].numOperacion").value("123456"))
                .andExpect(jsonPath("$.vuelto[0].tipoEvidencia").value("VUELTO"));
    }
}
