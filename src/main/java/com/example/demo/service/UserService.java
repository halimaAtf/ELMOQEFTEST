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

    public List<User> getPendingProviders() {
        return userRepository.findByRoleAndStatus("PROVIDER", "PENDING");
    }

    public User validateProvider(Long id, boolean accept) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

        // Mise à jour du statut selon le choix de l'admin
        user.setStatus(accept ? "AWAITING_VERIFICATION" : "REJECTED");

        // On retourne l'utilisateur sauvegardé pour que le Controller accède à user.getEmail()
        return userRepository.save(user);
    }

    // ── 3. Gestion globale des utilisateurs (Ecran User Management) ──
    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !"DELETED".equals(u.getStatus()))
                .collect(java.util.stream.Collectors.toList());
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID : " + id));

        user.setStatus("DELETED");
        long timestamp = System.currentTimeMillis();
        user.setUsername(user.getUsername() + "_deleted_" + timestamp);
        user.setEmail(user.getEmail() + "_deleted_" + timestamp);
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            user.setPhone(user.getPhone() + "_deleted_" + timestamp);
        }
        
        userRepository.save(user);
    }
}