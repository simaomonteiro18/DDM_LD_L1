package pt.istec.ishopping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pt.istec.ishopping.config.InicializadorUtilizador;

@SpringBootTest
@ActiveProfiles("test")
class IshoppingApplicationTests {

	@MockitoBean
	private InicializadorUtilizador inicializadorUtilizador;

	@Test
	void contextLoads() {
	}

}
