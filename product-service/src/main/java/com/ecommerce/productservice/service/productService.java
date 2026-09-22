package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.productRequest;
import com.ecommerce.productservice.dto.productResponse;

import java.util.List;

public interface productService {

    public productResponse createProduct(productRequest request);

    public productResponse getProductById(Long id);

    public List<productResponse> getAllProduct();

    public productResponse updateProduct(Long id, productRequest request);

    public void deleteProduct(Long id);
}
