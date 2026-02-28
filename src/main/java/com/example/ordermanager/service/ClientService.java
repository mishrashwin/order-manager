package com.example.ordermanager.service;

import com.example.ordermanager.entity.Client;
import com.example.ordermanager.entity.Company;
import com.example.ordermanager.repository.ClientRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

  private final ClientRepository clientRepository;
  private final CompanyService companyService;
  private Helper helper;

  public ClientService(ClientRepository clientRepository, CompanyService companyService,
      Helper helper) {
    this.clientRepository = clientRepository;
    this.companyService = companyService;
    this.helper = helper;
  }

  public List<Client> getAllClients() {
    return clientRepository.findAll();
  }

  /**
   * TENANT-AWARE: Get all clients for a specific company
   *
   * @param companyId Company ID
   * @return List of clients for the company
   */
  public List<Client> getClientsByCompanyId(Long companyId) {
    return clientRepository.findByCompanyId(companyId);
  }

  public Client getClientById(Long id) {
    return clientRepository.findById(id).orElse(null);
  }

  /**
   * TENANT-AWARE: Save client with company association The client is automatically assigned to the
   * authenticated user's company
   *
   * @param client Client entity
   * @param companyId Company ID
   */
  public void saveClientWithCompany(Client client, Long companyId) {
    // Get company and assign to client
    Company company =
        companyService.getCompanyById(companyId).orElseThrow(() -> new IllegalArgumentException(
            "Company not found. Cannot create client without a company."));

    client.setCompany(company);

    // Apply formatting
    if (client.getName() != null) {
      client.setName(client.getName().toUpperCase());
    }
    if (client.getContactPerson() != null) {
      client.setContactPerson(helper.toTitleCase(client.getContactPerson()));
    }

    clientRepository.save(client);
  }

  public void deleteClient(Long id) {
    clientRepository.deleteById(id);
  }
}
