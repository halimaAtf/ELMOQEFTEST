package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.Repository.UserRepository;
import com.example.demo.service.EmailService;
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

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private EmailService emailService;

    // LOGIN

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
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
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
                case "AWAITING_VERIFICATION" -> {
                    SecurityContextHolder.clearContext();
                    return ResponseEntity.ok().body(Map.of(
                            "status", "AWAITING_VERIFICATION",
                            "userId", user.getId(),
                            "message", "Veuillez entrer le code de vérification reçu par email."));
                }
                case "PENDING" -> {
                    SecurityContextHolder.clearContext();
                    return ResponseEntity.status(403).body(Map.of(
                            "error", "Votre compte est en attente de validation par un administrateur."));
                }
                case "REJECTED" -> {
                    SecurityContextHolder.clearContext();
                    return ResponseEntity.status(403).body(Map.of(
                            "error", "Votre demande a ete refusee. Contactez le support."));
                }
                case "SUSPENDED" -> {
                    SecurityContextHolder.clearContext();
                    return ResponseEntity.status(403).body(Map.of(
                            "error", "Votre compte a ete suspendu. Contactez le support."));
                }
            }

            // 6. Créer la session
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            // 7. Réponse
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("status", user.getStatus());
            if (user.getProfession() != null) {
                response.put("profession", user.getProfession());
            }
            if (user.getProfilePicture() != null) {
                response.put("profilePicture", user.getProfilePicture());
            }
            if (user.getLatitude() != null) {
                response.put("latitude", user.getLatitude());
            }
            if (user.getLongitude() != null) {
                response.put("longitude", user.getLongitude());
            }

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Nom d'utilisateur ou mot de passe incorrect"));
        } catch (Exception e) {
            // ✅ Log l'erreur exacte dans IntelliJ console
            System.err.println("=== ERREUR LOGIN : " + e.getClass().getName() + " : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    // ─────────────────────────────────────────────
    // REGISTER
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
                        "error", "Ce nom d'utilisateur est déjà utilisé"));
            }

            // 3. Vérifier unicité email
            if (userRepository.findByEmail(req.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cet email est déjà utilisé"));
            }

            // 4. Valider le rôle
            String role = req.getRole().toUpperCase();
            if (!role.equals("CLIENT") && !role.equals("PROVIDER")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Rôle invalide : " + role));
            }

            // 5. Créer l'utilisateur
            User user = new User();
            user.setUsername(req.getUsername().trim());
            user.setEmail(req.getEmail().trim());
            user.setPhone(req.getPhone().trim());
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            user.setRole(role);
            user.setStatus(role.equals("PROVIDER") ? "PENDING" : "AWAITING_VERIFICATION");
            if (role.equals("PROVIDER") && req.getProfession() != null) {
                user.setProfession(req.getProfession());
            }
            if (role.equals("PROVIDER") && req.getVerificationDocument() != null) {
                user.setVerificationDocument(req.getVerificationDocument());
            }

            // Generate a 6-digit verification code for both roles
            String code = String.format("%06d", new java.util.Random().nextInt(999999));
            user.setVerificationCode(code);

            userRepository.save(user);

            // Send verification email to CLIENT
            if (role.equals("CLIENT")) {
                try {
                    emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), code);
                } catch (Exception e) {
                    System.err.println("ERREUR ENVOI EMAIL: " + e.getMessage());
                }
            }

            // 6. Log succès
            System.out.println(
                    "=== INSCRIPTION OK : " + user.getUsername() + " / " + user.getRole() + " / " + user.getStatus());

            // 7. Logique de réponse différenciée
            Map<String, Object> response = new HashMap<>();

            if (role.equals("CLIENT")) {
                // Return AWAITING_VERIFICATION status to redirect to email verification screen
                response.put("role", user.getRole());
                response.put("status", user.getStatus());
                response.put("userId", user.getId());
                response.put("message", "Inscription réussie ! Un code de vérification a été envoyé à votre email.");
            } else { // PROVIDER (EN ATTENTE)
                response.put("role", user.getRole());
                response.put("status", user.getStatus());
                response.put("message", "Inscription réussie ! En attente de validation par un administrateur.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("=== ERREUR REGISTER : " + e.getClass().getName() + " : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue lors de l'inscription"));
        }
    }

    // ─────────────────────────────────────────────
    // VERIFY CODE
    // ─────────────────────────────────────────────
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId"));
            String code = request.get("code");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            if ("AWAITING_VERIFICATION".equals(user.getStatus()) && code.equals(user.getVerificationCode())) {
                user.setStatus("ACTIVE");
                // Clear code if you want, but fine to keep it
                userRepository.save(user);

                return ResponseEntity
                        .ok(Map.of("message", "Compte vérifié avec succès. Vous pouvez maintenant vous connecter."));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Code de vérification invalide."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erreur serveur"));
        }
    }

    // ─────────────────────────────────────────────
    // RESEND CODE
    // ─────────────────────────────────────────────
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            if ("AWAITING_VERIFICATION".equals(user.getStatus())) {
                // Generer un nouveau code
                String code = String.format("%06d", new java.util.Random().nextInt(999999));
                user.setVerificationCode(code);
                userRepository.save(user);

                // Send email
                emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), user.getVerificationCode());

                return ResponseEntity
                        .ok(Map.of("message", "Nouveau code de vérification envoyé sur votre adresse email."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "L'utilisateur n'est pas en attente de vérification."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erreur serveur"));
        }
    }

    // ─────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String contact = request.get("contact"); // email or phone
            if (contact == null || contact.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email ou téléphone obligatoire"));
            }

            // Find user by email or phone
            User user = userRepository.findByEmail(contact)
                    .orElseGet(() -> userRepository.findByPhone(contact).orElse(null));

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucun compte trouvé avec cet email ou téléphone."));
            }

            // Generate reset code
            String code = String.format("%06d", new java.util.Random().nextInt(999999));
            user.setVerificationCode(code);
            userRepository.save(user);

            // Send email (always to email even if phone is provided, since we only have
            // EmailService)
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), code);

            return ResponseEntity
                    .ok(Map.of("message", "Le code de réinitialisation a été envoyé à votre adresse email."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur : " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String contact = request.get("contact");
            String code = request.get("code");
            String newPassword = request.get("newPassword");

            if (contact == null || code == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tous les champs sont obligatoires."));
            }

            User user = userRepository.findByEmail(contact)
                    .orElseGet(() -> userRepository.findByPhone(contact).orElse(null));

            if (user == null || !code.equals(user.getVerificationCode())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code invalide ou utilisateur introuvable."));
            }

            // Met à jour le mot de passe
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setVerificationCode(null); // Clear code
            userRepository.save(user);

            return ResponseEntity.ok(
                    Map.of("message", "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur : " + e.getMessage()));
        }
    }

    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Deconnexion reussie"));
    }

    // INNER CLASSES
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String u) {
            this.username = u;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String p) {
            this.password = p;
        }
    }

    public static class RegisterRequest {
        private String username;
        private String email;
        private String phone;
        private String password;
        private String role;
        private String profession;
        private String verificationDocument;

        public String getUsername() {
            return username;
        }

        public void setUsername(String u) {
            this.username = u;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String e) {
            this.email = e;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String p) {
            this.phone = p;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String p) {
            this.password = p;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String r) {
            this.role = r;
        }

        public String getProfession() {
            return profession;
        }

        public void setProfession(String pr) {
            this.profession = pr;
        }

        public String getVerificationDocument() {
            return verificationDocument;
        }

        public void setVerificationDocument(String v) {
            this.verificationDocument = v;
        }
    }
}
