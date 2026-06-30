package com.gas.sistema_gas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.gas.sistema_gas", "com.gas.sistema_gas.Mapper" })
public class SistemaGasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaGasApplication.class, args);
	}
}
