package com.pedropaulo.minhasFinancas;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MinhasFinancasApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MinhasFinancasApplicationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void contextoDeveCarregarSemErros() {
    }

    @Test
    public void devePermitirCorsParaLocalhost3000() throws Exception {
        mvc.perform(options("/api/test")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", is("GET,POST,PUT,DELETE,OPTIONS")));
    }

    @Test
    public void deveNegarCorsParaOrigemNaoPermitida() throws Exception {
        mvc.perform(options("/api/test")
                        .header("Origin", "http://malicioso.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class DummyController {
        @GetMapping("/api/test")
        public String test() {
            return "ok";
        }
    }
}
