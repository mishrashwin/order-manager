package com.example.ordermanager.client.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.client.entity.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
  List<Client> findByCompanyId(Long companyId);

  Optional<Client> findByIdAndCompanyId(Long id, Long companyId);
}
