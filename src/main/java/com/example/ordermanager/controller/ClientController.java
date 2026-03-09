package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Client;
import com.example.ordermanager.exception.ClientHasActiveOrdersException;
import com.example.ordermanager.service.ClientService;
import com.example.ordermanager.service.CompanyService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clients")
public class ClientController {

  private final ClientService clientService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;

  public ClientController(ClientService clientService, CompanyService companyService,
      SecurityContextHelper securityContextHelper) {
    this.clientService = clientService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
  }

  @GetMapping
  public String listClients(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
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
          String.format("Cannot delete client: This client has %d active order(s). " +
              "Please delete or reassign the orders before deleting the client.",
              e.getOrderCount()));
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "Error deleting client: " + e.getMessage());
    }
    return "redirect:/clients";
  }
}
