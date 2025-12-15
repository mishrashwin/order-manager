package com.example.ordermanager.user.service;

import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RegistrationService registrationService;

    public UserService(UserRepository userRepository, RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.registrationService = registrationService;
    }

    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(false); // not verified yet
        User saved = userRepository.save(user);

        registrationService.sendVerificationEmail(saved);
        return saved;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
