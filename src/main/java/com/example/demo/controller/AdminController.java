package com.example.demo.controller;

import com.example.demo.Repository.DemandeRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.User;
import com.example.demo.service.EmailService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {

    private final UserService userService;
    private final EmailService emailService;

    @Autowired private UserRepository userRepo;
    @Autowired private DemandeRepository demandeRepo;

    // Un seul constructeur pour éviter les conflits d'injection
    public AdminController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    // Statistiques réelles pour le Dashboard
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Chiffres réels de la BDD
        stats.put("totalUsers", userRepo.count());
        stats.put("activeProviders", userRepo.countByRoleAndStatus("PROVIDER", "ACTIVE"));
        stats.put("completedJobs", demandeRepo.countByStatus("TERMINE"));

        // Optionnel : ajouter le revenu total (somme des prix des offres terminées)
        // stats.put("revenue", demandeRepo.sumRevenueFromCompletedJobs());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<User>> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingProviders());
    }

    @PostMapping("/validate/{id}")
    public ResponseEntity<?> validateUser(
            @PathVariable Long id,
            @RequestParam boolean accept) {
        try {
            // Cette méthode doit changer le statut en "ACTIVE" en BDD
            User user = userService.validateProvider(id, accept);

            if (accept) {
                emailService.sendApprovalEmail(user.getEmail(), user.getUsername());
            } else {
                emailService.sendRejectionEmail(user.getEmail(), user.getUsername());
            }

            return ResponseEntity.ok(Map.of(
                    "message", accept ? "Prestataire approuvé et activé." : "Demande refusée."
            ));
        } catch (Exception e) {
            // C'est ici que l'erreur "Erreur lors de la validation" est captée
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/suspend/{id}")
    public ResponseEntity<?> suspendUser(@PathVariable Long id) {
        try {
            userService.suspendUser(id);
            return ResponseEntity.ok(Map.of("message", "Utilisateur suspendu."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}