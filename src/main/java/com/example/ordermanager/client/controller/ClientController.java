package com.example.ordermanager.client.controller;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.client.exception.ClientHasActiveOrdersException;
import com.example.ordermanager.client.service.ClientService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clients")
public class ClientController {

  private final ClientService clientService;
  private final SecurityContextHelper securityContextHelper;

  public ClientController(ClientService clientService,
      SecurityContextHelper securityContextHelper) {
    this.clientService = clientService;
    this.securityContextHelper = securityContextHelper;
  }

  @GetMapping
  public String listClients(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    var clients = clientService.getClientsByCompanyId(companyId);
    model.addAttribute("clients", clients);
    return "clients/list";
  }

  @GetMapping("/new")
  public String newClientForm(Model model) {
    model.addAttribute("client", new Client());
    return "clients/form";
  }

  @PostMapping
  public String saveClient(@ModelAttribute Client client, Model model) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      clientService.saveClientWithCompany(client, companyId);
      return "redirect:/clients";
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      model.addAttribute("error", message);
      if (isPhoneValidationError(message)) {
        model.addAttribute("phoneError", message);
      }
      model.addAttribute("client", client);
      return "clients/form";
    } catch (IllegalStateException e) {
      model.addAttribute("error", "You must be logged in to create a client");
      model.addAttribute("client", client);
      return "clients/form";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving client: " + e.getMessage());
      model.addAttribute("client", client);
      return "clients/form";
    }
  }

  private boolean isPhoneValidationError(String message) {
    return message != null && message.toLowerCase().contains("phone");
  }

  @GetMapping("/edit/{id}")
  public String editClient(@PathVariable Long id, Model model) {
    model.addAttribute("client", clientService.getClientById(id));
    return "clients/form";
  }

  @GetMapping("/delete/{id}")
  public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      clientService.deleteClient(id);
      redirectAttributes.addFlashAttribute("success", "Client deleted successfully");
    } catch (ClientHasActiveOrdersException e) {
      redirectAttributes.addFlashAttribute("error",
          String.format(
              "Cannot delete client: This client has %d active order(s). "
                  + "Please delete or reassign the orders before deleting the client.",
              e.getOrderCount()));
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error deleting client: " + e.getMessage());
    }
    return "redirect:/clients";
  }
}
