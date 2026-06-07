package com.gas.sistema_gas.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gas.sistema_gas.Model.Opcion;
import com.gas.sistema_gas.Repository.OpcionRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private OpcionRepository opcionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<Opcion> opcionVentas = opcionRepository.findByRuta("ventas");
        if (opcionVentas.isEmpty()) {
            Opcion ventas = new Opcion();
            ventas.setIcono("fas fa-cash-register");
            ventas.setNombre("Ventas");
            ventas.setRuta("ventas");
            ventas.setEstado(1);
            opcionRepository.save(ventas);
        } else {
            Opcion existente = opcionVentas.get();
            if (existente.getEstado() == 0) {
                existente.setEstado(1);
                opcionRepository.save(existente);
            }
        }
    }
}
