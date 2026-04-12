package com.example.ordermanager.client.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.client.entity.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
  @EntityGraph(attributePaths = {"company"})
  List<Client> findByCompanyId(Long companyId);

  Page<Client> findByCompanyId(Long companyId, Pageable pageable);

  Page<Client> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String search, Pageable pageable);

  @EntityGraph(attributePaths = {"company"})
  Optional<Client> findByIdAndCompanyId(Long id, Long companyId);
}
