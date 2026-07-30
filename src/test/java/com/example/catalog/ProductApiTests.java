package com.example.catalog;

import com.example.catalog.dto.ProductRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "catalog.seed.enabled=false")
class ProductApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createGetUpdateDeleteLifecycle() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .category("Electronics")
                .price(new BigDecimal("29.99"))
                .stockQuantity(50)
                .sku("SKU-TEST-001")
                .build();

        String body = objectMapper.writeValueAsString(request);

        String location = mockMvc.perform(post("/api/products")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-TEST-001"));

        request.setPrice(new BigDecimal("24.99"));
        mockMvc.perform(put(location)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(24.99));

        mockMvc.perform(delete(location)).andExpect(status().isNoContent());
        mockMvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void listSupportsFilteringAndPagination() throws Exception {
        for (int i = 0; i < 3; i++) {
            ProductRequest request = ProductRequest.builder()
                    .name("Book " + i)
                    .category("Books")
                    .price(new BigDecimal("9.99"))
                    .stockQuantity(5)
                    .sku("SKU-BOOK-" + i)
                    .build();
            mockMvc.perform(post("/api/products")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)));
        }

        mockMvc.perform(get("/api/products")
                        .param("category", "Books")
                        .param("minPrice", "5")
                        .param("maxPrice", "20")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}
