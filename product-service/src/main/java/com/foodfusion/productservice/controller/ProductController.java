package com.foodfusion.productservice.controller;

import com.foodfusion.productservice.dto.ProductRequest;
import com.foodfusion.productservice.dto.ProductResponse;
import com.foodfusion.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/product", "/api/food", "/api/foods"})
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createProduct(@RequestBody ProductRequest productRequest){
        productService.createProduct(productRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductResponse> getAllProducts(){
        return productService.getAllProducts();
    }

    @GetMapping("/search")
    public List<ProductResponse> searchFoods(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        return productService.searchFoods(q, category);
    }
}
