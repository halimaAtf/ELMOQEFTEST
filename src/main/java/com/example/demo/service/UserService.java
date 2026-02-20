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

    // ── 1. Liste des Providers en attente (Utilisé par l'écran Provider Approvals) ──
    public List<User> getPendingProviders() {
        return userRepository.findByRoleAndStatus("PROVIDER", "PENDING");
    }

    // ── 2. Validation/Rejet avec retour d'objet pour EmailService ──
    public User validateProvider(Long id, boolean accept) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

        // Mise à jour du statut selon le choix de l'admin
        user.setStatus(accept ? "ACTIVE" : "REJECTED");

        // On retourne l'utilisateur sauvegardé pour que le Controller accède à user.getEmail()
        return userRepository.save(user);
    }

    // ── 3. Gestion globale des utilisateurs (Ecran User Management) ──
    public List<User> getAllUsers() {
        //
        return userRepository.findAll();
    }

    // ── 4. Suspension (Action du bouton Ban) ──
    public void suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

        user.setStatus("SUSPENDED"); // Changement vers l'état banni
        userRepository.save(user);
    }

    // ── 5. Suppression définitive (Action du bouton Trash) ──
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur introuvable avec l'ID : " + id);
        }
        userRepository.deleteById(id); 
    }
}