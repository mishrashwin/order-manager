package com.example.ordermanager.client.controller;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.client.exception.ClientHasActiveOrdersException;
import com.example.ordermanager.client.service.ClientService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clients")
public class ClientController {

  private static final String CLIENT_FALLBACK_PATH = "/clients";

  private final ClientService clientService;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;

  public ClientController(ClientService clientService, SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService) {
    this.clientService = clientService;
    this.securityContextHelper = securityContextHelper;
    this.passwordVerificationService = passwordVerificationService;
  }

  @GetMapping
  public String listClients(@RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int size, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Pageable pageable = PageRequest.of(page, size);
    Page<com.example.ordermanager.client.entity.Client> clientsPage =
        clientService.searchClients(companyId, search, pageable);
    model.addAttribute("clients", clientsPage.getContent());
    model.addAttribute("currentPage", page + 1);
    model.addAttribute("totalPages", clientsPage.getTotalPages());
    model.addAttribute("totalElements", clientsPage.getTotalElements());
    model.addAttribute("search", search);
    return "clients/list";
  }

  @GetMapping("/new")
  public String newClientForm(@RequestParam(required = false) String returnTo, Model model) {
    model.addAttribute("client", new Client());
    model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
    return "clients/form";
  }

  @PostMapping
  public String saveClient(@ModelAttribute Client client,
      @RequestParam(required = false) String returnTo, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      boolean isUpdate = client.getId() != null;
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      clientService.saveClientWithCompany(client, companyId);
      redirectAttributes.addFlashAttribute("success",
          isUpdate ? "Client updated successfully" : "Client added successfully");
      return "redirect:" + sanitizeReturnTo(returnTo);
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      model.addAttribute("error", message);
      if (isPhoneValidationError(message)) {
        model.addAttribute("phoneError", message);
      }
      model.addAttribute("client", client);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
      return "clients/form";
    } catch (IllegalStateException e) {
      model.addAttribute("error", "You must be logged in to create a client");
      model.addAttribute("client", client);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
      return "clients/form";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving client: " + e.getMessage());
      model.addAttribute("client", client);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
      return "clients/form";
    }
  }

  private boolean isPhoneValidationError(String message) {
    return message != null && message.toLowerCase().contains("phone");
  }

  @GetMapping("/edit/{id}")
  public String editClient(@PathVariable Long id, @RequestParam(required = false) String returnTo,
      Model model) {
    model.addAttribute("client", clientService.getClientById(id));
    model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
    return "clients/form";
  }

  @PostMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public String deleteClient(@PathVariable Long id, @RequestParam String password,
      RedirectAttributes redirectAttributes) {
    if (!passwordVerificationService.verifyCurrentUserPassword(password)) {
      redirectAttributes.addFlashAttribute("error", "Incorrect password. Client was not deleted.");
      return "redirect:/clients";
    }
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

  private String sanitizeReturnTo(String returnTo) {
    if (returnTo == null || returnTo.isBlank()) {
      return CLIENT_FALLBACK_PATH;
    }
    String normalized = returnTo.trim();
    try {
      normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ignored) {
    }
    int delimiter = normalized.indexOf(',');
    if (delimiter >= 0) {
      normalized = normalized.substring(0, delimiter).trim();
    }
    if (!normalized.startsWith("/") || normalized.startsWith("//") || normalized.contains("\r")
        || normalized.contains("\n")) {
      return CLIENT_FALLBACK_PATH;
    }
    return normalized.startsWith("/orders") || normalized.startsWith("/clients") ? normalized
        : CLIENT_FALLBACK_PATH;
  }
}
