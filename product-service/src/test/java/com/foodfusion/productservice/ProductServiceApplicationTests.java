package com.foodfusion.productservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodfusion.productservice.dto.ProductRequest;
import com.foodfusion.productservice.dto.ProductResponse;
import com.foodfusion.productservice.repository.ProductRepository;
import com.foodfusion.productservice.service.ProductService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@TestPropertySource(properties = "foodfusion.seed-data.enabled=false")
class ProductServiceApplicationTests {
	/*
	troubleshooting: https://stackoverflow.com/questions/61108655/test-container-test-cases-are-failing-due-to-could-not-find-a-valid-docker-envi
	 */


	// creating a local docker container with mongodb version mentioned as argument
	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.2");
	@Autowired
	private MockMvc mockMvc;

	// used to map object to String and vise-versa
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProductService productService;

	@Autowired
	private ProductRepository productRepository;

	@BeforeEach
	void clearProducts() {
		productRepository.deleteAll();
	}

	// setting the connection to mongodb
	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry){
		dynamicPropertyRegistry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
	}

	void getNoProductResponse() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/product")).andExpect(status().isOk());
	}

	@Test
	void shouldCreateProduct() throws Exception {
		var productRequest = getProductRequest();
		var objectMapperString = objectMapper.writeValueAsString(productRequest);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapperString))
				.andExpect(status().isCreated());
		Assertions.assertEquals(1, productRepository.findAll().size());
	}

	@Test
	public void getProductResponse() throws Exception{
		var productRequest = getProductRequest();
		var objectMapperString = objectMapper.writeValueAsString(productRequest);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapperString))
				.andExpect(status().isCreated());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/product")).andExpect(status().isOk());
		Assertions.assertEquals(1, productRepository.findAll().size());

		List<ProductResponse> productResponseList = productService.getAllProducts();
		Assertions.assertEquals(1, productResponseList.size());

	}

	/*
		returns an object of productRequest with sample data.
	 */
	private ProductRequest getProductRequest() {
		return ProductRequest.builder()
				.name("samsung_s22")
				.description("samsung_s22")
				.price(BigDecimal.valueOf(1000))
				.build();
	}

}
