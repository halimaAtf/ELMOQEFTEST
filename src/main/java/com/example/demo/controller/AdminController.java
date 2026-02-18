package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.EmailService;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {

    private final UserService userService;
    private final EmailService emailService;

    public AdminController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    // ── GET /api/admin/pending ────────────────────────────
    @GetMapping("/pending")
    public ResponseEntity<List<User>> getPendingUsers() {
        List<User> pending = userService.getPendingProviders();
        return ResponseEntity.ok(pending);
    }

    // ── POST /api/admin/validate/{id}?accept=true/false ───
    @PostMapping("/validate/{id}")
    public ResponseEntity<?> validateUser(
            @PathVariable Long id,
            @RequestParam boolean accept) {
        try {
            User user = userService.validateProvider(id, accept); // retourne User

            if (accept) {
                emailService.sendApprovalEmail(user.getEmail(), user.getUsername());
            } else {
                emailService.sendRejectionEmail(user.getEmail(), user.getUsername());
            }

            return ResponseEntity.ok(Map.of(
                    "message", accept ? "Prestataire valide, email envoye." : "Demande refusee, email envoye."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/admin/users ──────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // ── POST /api/admin/suspend/{id} ──────────────────────
    @PostMapping("/suspend/{id}")
    public ResponseEntity<?> suspendUser(@PathVariable Long id) {
        try {
            userService.suspendUser(id);
            return ResponseEntity.ok(Map.of("message", "Utilisateur suspendu."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/admin/users/{id} ──────────────────────
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "Utilisateur supprime."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}