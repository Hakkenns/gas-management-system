package com.gas.sistema_gas;

import com.gas.sistema_gas.integration.InventarioConcurrenciaMySqlTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ContextConfiguration(
        initializers = InventarioConcurrenciaMySqlTest.TestDbInitializer.class
)
class SistemaGasApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
		assertEquals("sistema_gas_concurrency_test", database);
	}

}
