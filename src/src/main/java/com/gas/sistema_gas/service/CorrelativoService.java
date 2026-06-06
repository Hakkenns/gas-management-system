package com.gas.sistema_gas.service;

public interface CorrelativoService {

    /**
     * Obtiene el siguiente código correlativo sin incrementar el contador.
     */
    String previsualizarCodigoSiguiente(String tipo, String serie);

    /**
     * Incrementa el contador y devuelve el código correlativo definitivo.
     */
    String incrementarYObtenerCodigo(String tipo, String serie);
}
