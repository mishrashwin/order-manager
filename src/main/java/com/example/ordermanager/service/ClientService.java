package com.example.ordermanager.service;

import com.example.ordermanager.entity.Client;
import com.example.ordermanager.repository.ClientRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    private Helper helper;

    public ClientService(ClientRepository clientRepository, Helper helper) {
        this.clientRepository = clientRepository;
        this.helper = helper;
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }

    public void saveClient(Client client) {
        if (client.getName() != null) {
            client.setName(client.getName().toUpperCase());
        }
        if (client.getContactPerson() != null){
            client.setContactPerson(helper.toTitleCase(client.getContactPerson()));
        }
        clientRepository.save(client);
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }
}
