package com.gas.sistema_gas.service.impl;

import com.gas.sistema_gas.Model.Correlativo;
import com.gas.sistema_gas.Repository.CorrelativoRepository;
import com.gas.sistema_gas.service.CorrelativoService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CorrelativoServiceImpl implements CorrelativoService {

    private final CorrelativoRepository correlativoRepository;

    public CorrelativoServiceImpl(CorrelativoRepository correlativoRepository) {
        this.correlativoRepository = correlativoRepository;
    }

    @Override
    public String previsualizarCodigoSiguiente(String tipo, String serie) {
        Correlativo c = correlativoRepository.findByTipoAndSerie(tipo, serie)
                .orElseThrow(() -> new RuntimeException("No existe correlativo para tipo=" + tipo + " serie=" + serie));
        int siguiente = (c.getNumeroActual() == null ? 0 : c.getNumeroActual()) + 1;
        return String.format("%s-%04d", serie, siguiente);
    }

    @Override
    @Transactional
    public String incrementarYObtenerCodigo(String tipo, String serie) {
        Correlativo c = correlativoRepository.findWithLockByTipoAndSerie(tipo, serie)
                .orElseThrow(() -> new RuntimeException("No existe correlativo para tipo=" + tipo + " serie=" + serie));
        int siguiente = (c.getNumeroActual() == null ? 0 : c.getNumeroActual()) + 1;
        c.setNumeroActual(siguiente);
        correlativoRepository.save(c);
        return String.format("%s-%04d", serie, siguiente);
    }
}
