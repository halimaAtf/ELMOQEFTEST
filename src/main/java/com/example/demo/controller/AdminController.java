package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<User>> getPendingUsers() {
        List<User> pending = userService.getPendingProviders();
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/validate/{id}")
    public ResponseEntity<?> validateUser(@PathVariable Long id, @RequestParam boolean accept) {
        try {
            userService.validateProvider(id, accept);
            return ResponseEntity.ok(Map.of(
                    "message", accept ? "Prestataire validé" : "Demande refusée"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}