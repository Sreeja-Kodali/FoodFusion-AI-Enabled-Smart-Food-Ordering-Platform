package com.foodfusion.productservice.service;


import com.foodfusion.productservice.dto.ProductRequest;
import com.foodfusion.productservice.dto.ProductResponse;
import com.foodfusion.productservice.model.Product;
import com.foodfusion.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public void createProduct(ProductRequest productRequest) {
        Product product = Product.builder()
                .name(productRequest.getName())
                .skuCode(productRequest.getSkuCode())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .category(productRequest.getCategory() == null ? "Other" : productRequest.getCategory().trim())
                .imageUrl(productRequest.getImageUrl())
                .available(productRequest.getAvailable() == null || productRequest.getAvailable())
                .build();

        productRepository.save(product);
        log.info("Product with id {} is saved", product.getId());
    }

    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    public List<ProductResponse> searchFoods(String query, String category) {
        List<Product> products;
        if (query != null && !query.isBlank()) {
            String normalized = query.trim();
            products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    normalized, normalized);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByCategoryIgnoreCase(category.trim().toLowerCase(Locale.ROOT));
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .filter(product -> product.getAvailable() == null || product.getAvailable())
                .map(this::mapToProductResponse)
                .toList();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .skuCode(product.getSkuCode())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .available(product.getAvailable() == null || product.getAvailable())
                .build();
    }
}