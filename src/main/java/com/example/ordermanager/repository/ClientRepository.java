package com.example.ordermanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.entity.Client;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
  List<Client> findByCompanyId(Long companyId);
}
