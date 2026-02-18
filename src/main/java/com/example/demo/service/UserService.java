package com.example.demo.service;

import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── Providers en attente ──────────────────────────────
    public List<User> getPendingProviders() {
        return userRepository.findByRoleAndStatus("PROVIDER", "PENDING");
    }

    // ── Valider ou rejeter → retourne User (obligatoire pour l'email) ──
    public User validateProvider(Long id, boolean accept) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

        user.setStatus(accept ? "ACTIVE" : "REJECTED");
        return userRepository.save(user); // ← retourne User, pas void
    }

    //  Tous les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Suspendre un user
    public void suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));
        user.setStatus("SUSPENDED");
        userRepository.save(user);
    }

    //  Supprimer un user
    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur introuvable avec l'ID : " + id);
        }

        userRepository.deleteById(id);
    }
}