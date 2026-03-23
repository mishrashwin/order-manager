package com.example.ordermanager.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.client.repository.ClientRepository;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.repository.OrderRepository;
import com.example.ordermanager.utils.Helper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

  @Mock
  private ClientRepository clientRepository;
  @Mock
  private OrderRepository orderRepository;
  @Mock
  private CompanyService companyService;

  private ClientService clientService;

  @BeforeEach
  void setUp() {
    clientService =
        new ClientService(clientRepository, orderRepository, companyService, new Helper());
  }

  @Test
  void saveClientWithCompany_normalizesPhoneAndFormatsNames() {
    Company company = new Company();
    company.setId(9L);

    Client client = new Client();
    client.setName("acme retail");
    client.setContactPerson("john DOE");
    client.setPhone("+91 98765 43210");

    when(companyService.getCompanyById(9L)).thenReturn(Optional.of(company));
    when(clientRepository.save(any(Client.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    clientService.saveClientWithCompany(client, 9L);

    assertThat(client.getName()).isEqualTo("ACME RETAIL");
    assertThat(client.getContactPerson()).isEqualTo("John Doe");
    assertThat(client.getPhone()).isEqualTo("919876543210");
    assertThat(client.getCompany().getId()).isEqualTo(9L);
  }

  @Test
  void saveClientWithCompany_rejectsInvalidInternationalPhone() {
    Company company = new Company();
    company.setId(9L);

    Client client = new Client();
    client.setName("acme retail");
    client.setPhone("12345");

    when(companyService.getCompanyById(9L)).thenReturn(Optional.of(company));

    assertThatThrownBy(() -> clientService.saveClientWithCompany(client, 9L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Client phone is invalid");

    verify(clientRepository, never()).save(any(Client.class));
  }
}

