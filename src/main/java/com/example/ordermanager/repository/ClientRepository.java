package com.example.ordermanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
