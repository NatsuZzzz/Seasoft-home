package com.store.seasoft;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeasoftApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void protectedEndpointWithoutToken_returns401Json() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Bạn cần đăng nhập"));
	}

	@Test
	void protectedEndpointWithGarbageToken_returns401NotServerError() throws Exception {
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());
	}
}
