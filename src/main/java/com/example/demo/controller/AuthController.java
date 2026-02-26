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

import java.util.Collections;
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

    // Temporary storage for verification codes before account creation
    private java.util.Map<String, String> verificationCodes = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/social-login")
    public ResponseEntity<?> socialLogin(@RequestBody SocialLoginRequest req, HttpServletRequest request) {
        try {
            String email = req.getEmail();
            if (email == null || email.trim().isEmpty()) {
                email = req.getProvider() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8)
                        + "@example.com";
            }

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // Register new user via social login
                user = new User();
                user.setEmail(email);
                String username = req.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    username = email.split("@")[0];
                }
                user.setUsername(username);
                user.setProfilePicture(req.getProfilePicture());
                user.setRole(req.getRole() != null ? req.getRole().toUpperCase() : "CLIENT");
                user.setStatus("ACTIVE");
                // Random strong password for social users
                user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString() + "!A1a"));

                if ("PROVIDER".equals(user.getRole())) {
                    user.setStatus("PENDING");
                    user.setProfession(req.getProfession() != null ? req.getProfession() : "Autre");
                }

                userRepository.save(user);
            }

            // Authenticate programmatically
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user.getUsername(),
                    null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authToken);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Social login successful");
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("status", user.getStatus());
            response.put("profilePicture", user.getProfilePicture());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred during social login."));
        }
    }

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
            System.err.println("=== ERREUR LOGIN : " + e.getClass().getName() + " : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendPreRegistrationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String username = request.get("username");
        if (email == null || email.isBlank() || username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email et Username sont obligatoires"));
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cet email est déjà utilisé"));
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ce nom d'utilisateur est déjà utilisé"));
        }
        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        verificationCodes.put(email, code);
        try {
            emailService.sendVerificationEmail(email, username, code);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de l'envoi de l'email"));
        }
        return ResponseEntity.ok(Map.of("message", "Code envoyé à " + email));
    }

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

            if (userRepository.findByUsername(req.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Ce nom d'utilisateur est déjà utilisé"));
            }

            if (userRepository.findByEmail(req.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cet email est déjà utilisé"));
            }

            String role = req.getRole().toUpperCase();
            if (!role.equals("CLIENT") && !role.equals("PROVIDER")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Rôle invalide : " + role));
            }

            if (req.getCode() == null || req.getCode().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code de vérification obligatoire"));
            }

            String expectedCode = verificationCodes.get(req.getEmail());
            if (expectedCode == null || !expectedCode.equals(req.getCode())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code de verification invalide ou expiré"));
            }

            // 5. Créer l'utilisateur
            User user = new User();
            user.setUsername(req.getUsername().trim());
            user.setEmail(req.getEmail().trim());
            user.setPhone(req.getPhone().trim());
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            user.setRole(role);
            user.setStatus(role.equals("PROVIDER") ? "PENDING" : "ACTIVE");
            if (role.equals("PROVIDER") && req.getProfession() != null) {
                user.setProfession(req.getProfession());
            }
            if (role.equals("PROVIDER") && req.getVerificationDocument() != null) {
                user.setVerificationDocument(req.getVerificationDocument());
            }

            userRepository.save(user);
            verificationCodes.remove(req.getEmail());

            System.out.println(
                    "=== INSCRIPTION OK : " + user.getUsername() + " / " + user.getRole() + " / " + user.getStatus());

            // 7. Logique de réponse différenciée
            Map<String, Object> response = new HashMap<>();

            if (role.equals("CLIENT")) {
                response.put("role", user.getRole());
                response.put("status", user.getStatus());
                response.put("userId", user.getId());
                response.put("message", "Inscription réussie ! Vous pouvez maintenant vous connecter.");
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

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId"));
            String code = request.get("code");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            if ("AWAITING_VERIFICATION".equals(user.getStatus()) && code.equals(user.getVerificationCode())) {
                user.setStatus("ACTIVE");
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

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            if ("AWAITING_VERIFICATION".equals(user.getStatus())) {
                String code = String.format("%06d", new java.util.Random().nextInt(999999));
                user.setVerificationCode(code);
                userRepository.save(user);

                // envoi email
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

    // mdp oublier
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String contact = request.get("contact"); // email or phone
            if (contact == null || contact.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email ou téléphone obligatoire"));
            }

            // trouver user avec email / num de tele
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

            // Send email

            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), code);

            return ResponseEntity
                    .ok(Map.of("message", "Le code de réinitialisation a été envoyé à votre adresse email."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur : " + e.getMessage()));
        }
    }

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
            user.setVerificationCode(null);
            userRepository.save(user);

            return ResponseEntity.ok(
                    Map.of("message", "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur : " + e.getMessage()));
        }
    }

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
        private String code;

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

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class SocialLoginRequest {
        private String email;
        private String username;
        private String profilePicture;
        private String provider;
        private String role;
        private String profession;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getProfilePicture() {
            return profilePicture;
        }

        public void setProfilePicture(String profilePicture) {
            this.profilePicture = profilePicture;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getProfession() {
            return profession;
        }

        public void setProfession(String profession) {
            this.profession = profession;
        }
    }
}
