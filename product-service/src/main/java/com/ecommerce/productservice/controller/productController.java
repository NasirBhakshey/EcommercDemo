package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.productRequest;
import com.ecommerce.productservice.dto.productResponse;
import com.ecommerce.productservice.service.productService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class productController {

    private final productService productService;

    public productController(productService productService){
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<productResponse> createProduct(@Valid @RequestBody productRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));

    }

    @GetMapping("/{id}")
    public ResponseEntity<productResponse> getProductById(@PathVariable Long id){

        return ResponseEntity.ok(productService.getProductById(id));

    }

    @GetMapping
    public ResponseEntity<List<productResponse>> getAllProduct(){
        return ResponseEntity.ok(productService.getAllProduct());
    }

    @PutMapping("/{id}")
    public ResponseEntity<productResponse> updateProduct(@PathVariable Long id,
                                                         @Valid @RequestBody productRequest request){
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id){
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
