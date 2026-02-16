package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String username, String email, String phone, String password, String role) {
        // Vérifier si l'utilisateur existe déjà
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Ce nom d'utilisateur existe déjà");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        // Les clients sont validés automatiquement, les prestataires doivent attendre
        user.setValidated(role.equals("CLIENT"));

        return userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        if (!user.isValidated()) {
            throw new RuntimeException("Votre compte est en attente de validation par  l'administrateur");
        }

        return user;
    }

    public List<User> getPendingProviders() {
        return userRepository.findByRoleAndIsValidated("PROVIDER", false);
    }

    public void validateProvider(Long id, boolean accept) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (accept) {
            user.setValidated(true);
            userRepository.save(user);
        } else {
            userRepository.delete(user);
        }
    }
}
