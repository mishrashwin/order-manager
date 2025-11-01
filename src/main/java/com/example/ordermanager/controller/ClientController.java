package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Client;
import com.example.ordermanager.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public String listClients(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "clients/list";
    }

    @GetMapping("/new")
    public String newClientForm(Model model) {
        model.addAttribute("client", new Client());
        return "clients/form";
    }

    @PostMapping
    public String saveClient(@ModelAttribute Client client) {
        clientService.saveClient(client);
        return "redirect:/clients";
    }

    @GetMapping("/edit/{id}")
    public String editClient(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.getClientById(id));
        return "clients/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return "redirect:/clients";
    }
}
