package com.ecommerce.productservice.service.Impl;

import com.ecommerce.productservice.dto.productRequest;
import com.ecommerce.productservice.dto.productResponse;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.productNotFoundException;
import com.ecommerce.productservice.repository.productRepository;
import com.ecommerce.productservice.service.productService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class productServiceImpl implements productService {


    private final productRepository repository;

    public productServiceImpl(productRepository repository){
        this.repository = repository;
    }


    @Override
    public productResponse createProduct(productRequest request) {

        Product product=new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        Product save=repository.save(product);

        return mapToResponse(save);
    }

    @Override
    public productResponse getProductById(Long id) {

        Product product = repository.findById(id).orElseThrow(()->
                new productNotFoundException("Product Not Found By Id " +id));

        return mapToResponse(product);
    }

    @Override
    public List<productResponse> getAllProduct() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public productResponse updateProduct(Long id, productRequest request) {

        Product product = repository.findById(id).orElseThrow(()->
                new productNotFoundException("Product Not Found By ID..." +id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        Product update = repository.save(product);
        return mapToResponse(update);
    }

    @Override
    public void deleteProduct(Long id) {

        Product product = repository.findById(id).orElseThrow(()->
                new productNotFoundException("Product Not Found By ID..." +id));

        repository.delete(product);

    }

    private productResponse mapToResponse(Product product){

        return new productResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock()
        );

    }
}
