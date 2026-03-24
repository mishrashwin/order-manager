package com.example.ordermanager.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.client.service.ClientService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

  @Mock
  private ClientService clientService;
  @Mock
  private SecurityContextHelper securityContextHelper;

  private ClientController clientController;

  @BeforeEach
  void setUp() {
    clientController = new ClientController(clientService, securityContextHelper);
  }

  @Test
  void saveClient_addSuccess_redirectsToClientListWithMessage() {
    Client client = new Client();
    client.setName("Acme");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = clientController.saveClient(client, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/clients");
    assertThat(redirectAttributes.getFlashAttributes().get("success"))
        .isEqualTo("Client added successfully");
    verify(clientService).saveClientWithCompany(client, 7L);
  }

  @Test
  void saveClient_updateSuccess_redirectsToClientListWithMessage() {
    Client client = new Client();
    client.setId(9L);
    client.setName("Acme");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = clientController.saveClient(client, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/clients");
    assertThat(redirectAttributes.getFlashAttributes().get("success"))
        .isEqualTo("Client updated successfully");
    verify(clientService).saveClientWithCompany(client, 7L);
  }

  @Test
  void saveClient_phoneValidationFailure_returnsFormWithInlinePhoneError() {
    Client client = new Client();
    client.setPhone("+9777894561230");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    doThrow(new IllegalArgumentException(
        "Client Phone No is invalid. Use country code + mobile number.")).when(clientService)
        .saveClientWithCompany(client, 7L);

    String view = clientController.saveClient(client, model, redirectAttributes);

    assertThat(view).isEqualTo("clients/form");
    assertThat(model.getAttribute("error"))
        .isEqualTo("Client Phone No is invalid. Use country code + mobile number.");
    assertThat(model.getAttribute("phoneError"))
        .isEqualTo("Client Phone No is invalid. Use country code + mobile number.");
    assertThat(model.getAttribute("client")).isEqualTo(client);
  }
}


