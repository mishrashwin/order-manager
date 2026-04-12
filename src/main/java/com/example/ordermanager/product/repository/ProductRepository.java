package com.example.ordermanager.product.repository;

import com.example.ordermanager.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
  @EntityGraph(attributePaths = {"company", "vendor"})
  List<Product> findByCompanyId(Long companyId);

  Page<Product> findByCompanyId(Long companyId, Pageable pageable);

  Page<Product> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String search, Pageable pageable);
}
