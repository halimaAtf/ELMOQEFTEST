package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            String email = payload.get("email");
            String phone = payload.get("phone");
            String password = payload.get("password");
            String role = payload.get("role");

            User newUser = userService.registerUser(username, email, phone, password, role);
            return ResponseEntity.ok(Map.of(
                    "message", "Inscription réussie!",
                    "role", newUser.getRole(),
                    "username", newUser.getUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            User user = userService.login(username, password);

            return ResponseEntity.ok(Map.of(
                    "message", "Connexion réussie",
                    "role", user.getRole(),
                    "username", user.getUsername(),
                    "id", user.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Identifiants incorrects"));
        }
    }
}