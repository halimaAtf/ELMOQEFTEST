package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;

    // ─────────────────────────────────────────────
    //  LOGIN
    // ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            // 1. Vérifier que les champs ne sont pas vides
            if (req.getUsername() == null || req.getUsername().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username obligatoire"));
            }
            if (req.getPassword() == null || req.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password obligatoire"));
            }

            // 2. Authentifier
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 3. Récupérer l'utilisateur
            User user = userRepository.findByUsername(req.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            // 4. Corriger status null (anciens comptes)
            if (user.getStatus() == null || user.getStatus().isBlank()) {
                user.setStatus("ACTIVE");
                userRepository.save(user);
            }

            // 5. Vérifier le statut
            switch (user.getStatus()) {
                case "PENDING" -> {
                    SecurityContextHolder.clearContext();
                    return ResponseEntity.status(403).body(Map.of(
                            "error", "Votre compte est en attente de validation par un administrateur."
                    ));
                }
                case "REJECTED" -> {
                    SecurityContextHolder.clearContext();
                    return ResponseEntity.status(403).body(Map.of(
                            "error", "Votre demande a ete refusee. Contactez le support."
                    ));
                }
                case "SUSPENDED" -> {
                    SecurityContextHolder.clearContext();
                    return ResponseEntity.status(403).body(Map.of(
                            "error", "Votre compte a ete suspendu. Contactez le support."
                    ));
                }
            }

            // 6. Créer la session
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            // 7. Réponse
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("status", user.getStatus());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Nom d'utilisateur ou mot de passe incorrect"
            ));
        } catch (Exception e) {
            // ✅ Log l'erreur exacte dans IntelliJ console
            System.err.println("=== ERREUR LOGIN : " + e.getClass().getName() + " : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"
            ));
        }
    }

    // ─────────────────────────────────────────────
    //  REGISTER
    // ─────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            // 1. Vérifier champs obligatoires
            if (req.getUsername() == null || req.getUsername().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Username obligatoire"));
            if (req.getEmail() == null || req.getEmail().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Email obligatoire"));
            if (req.getPhone() == null || req.getPhone().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Téléphone obligatoire"));
            if (req.getPassword() == null || req.getPassword().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe obligatoire"));
            if (req.getRole() == null || req.getRole().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Rôle obligatoire"));

            // 2. Vérifier unicité username
            if (userRepository.findByUsername(req.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Ce nom d'utilisateur est déjà utilisé"
                ));
            }

            // 3. Vérifier unicité email
            if (userRepository.findByEmail(req.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cet email est déjà utilisé"
                ));
            }

            // 4. Valider le rôle
            String role = req.getRole().toUpperCase();
            if (!role.equals("CLIENT") && !role.equals("PROVIDER")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Rôle invalide : " + role
                ));
            }

            // 5. Créer l'utilisateur
            User user = new User();
            user.setUsername(req.getUsername().trim());
            user.setEmail(req.getEmail().trim());
            user.setPhone(req.getPhone().trim());
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            user.setRole(role);
            user.setStatus(role.equals("PROVIDER") ? "PENDING" : "ACTIVE");

            userRepository.save(user);

            // 6. Log succès dans IntelliJ
            System.out.println("=== INSCRIPTION OK : " + user.getUsername() + " / " + user.getRole() + " / " + user.getStatus());

            // 7. Réponse
            Map<String, Object> response = new HashMap<>();
            response.put("role", user.getRole());
            response.put("status", user.getStatus());
            response.put("message", role.equals("PROVIDER")
                    ? "Inscription réussie ! En attente de validation par un administrateur."
                    : "Inscription réussie ! Vous pouvez maintenant vous connecter."
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // ✅ Log l'erreur exacte dans IntelliJ console
            System.err.println("=== ERREUR REGISTER : " + e.getClass().getName() + " : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue lors de l'inscription"
            ));
        }
    }

    // ─────────────────────────────────────────────
    //  LOGOUT
    // ─────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Deconnexion reussie"));
    }

    // ─────────────────────────────────────────────
    //  INNER CLASSES
    // ─────────────────────────────────────────────
    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String u) { this.username = u; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
    }

    public static class RegisterRequest {
        private String username;
        private String email;
        private String phone;
        private String password;
        private String role;
        public String getUsername() { return username; }
        public void setUsername(String u) { this.username = u; }
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getPhone() { return phone; }
        public void setPhone(String p) { this.phone = p; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
        public String getRole() { return role; }
        public void setRole(String r) { this.role = r; }
    }
}
