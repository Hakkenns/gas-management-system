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
        // La inicialización del menú "Ventas" general ha sido deshabilitada ya que fue
        // reemplazada por los menús específicos "Ventas Local" y "Ventas Domicilio".
    }
}
