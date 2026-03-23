package com.example.ordermanager.client.service;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.client.exception.ClientHasActiveOrdersException;
import com.example.ordermanager.client.repository.ClientRepository;
import com.example.ordermanager.order.repository.OrderRepository;
import com.example.ordermanager.utils.Helper;
import com.example.ordermanager.utils.PhoneNumberUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

  private final ClientRepository clientRepository;
  private final OrderRepository orderRepository;
  private final CompanyService companyService;
  private final Helper helper;

  public ClientService(ClientRepository clientRepository, OrderRepository orderRepository,
      CompanyService companyService, Helper helper) {
    this.clientRepository = clientRepository;
    this.orderRepository = orderRepository;
    this.companyService = companyService;
    this.helper = helper;
  }

  public List<Client> getClientsByCompanyId(Long companyId) {
    return clientRepository.findByCompanyId(companyId);
  }

  public Client getClientById(Long id) {
    return clientRepository.findById(id).orElse(null);
  }

  public void saveClientWithCompany(Client client, Long companyId) {
    Company company =
        companyService.getCompanyById(companyId).orElseThrow(() -> new IllegalArgumentException(
            "Company not found. Cannot create client without a company."));

    client.setCompany(company);

    if (client.getName() != null) {
      client.setName(client.getName().toUpperCase());
    }
    if (client.getContactPerson() != null) {
      client.setContactPerson(helper.toTitleCase(client.getContactPerson()));
    }

    client.setPhone(
        PhoneNumberUtils.normalizeOptionalInternational(client.getPhone(), "Client Phone No"));

    clientRepository.save(client);
  }

  public void deleteClient(Long id) {
    long orderCount = orderRepository.countByClientId(id);

    if (orderCount > 0) {
      throw new ClientHasActiveOrdersException(id, orderCount);
    }

    clientRepository.deleteById(id);
  }
}
