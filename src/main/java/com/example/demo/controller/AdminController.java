package com.example.demo.controller;

import com.example.demo.Repository.DemandeRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Repository.SystemSettingRepository;
import com.example.demo.Repository.SupportTicketRepository;
import com.example.demo.entity.SystemSetting;
import com.example.demo.entity.SupportTicket;
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

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private DemandeRepository demandeRepo;
    @Autowired
    private SystemSettingRepository settingRepo;

    @Autowired
    private SupportTicketRepository supportRepo;

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

        Double totalRevenue = demandeRepo.sumRevenueFromCompletedJobs();
        stats.put("revenue", totalRevenue != null ? totalRevenue : 0.0);

        List<Object[]> revenueByMonthRaw = demandeRepo.getRevenueByMonth();
        List<Map<String, Object>> chartData = new java.util.ArrayList<>();

        // Add fake base data if DB is empty to prevent visual break
        if (revenueByMonthRaw.isEmpty()) {
            chartData.add(Map.of("name", "Jan", "value", 0));
            chartData.add(Map.of("name", "Feb", "value", 0));
            chartData.add(Map.of("name", "Mar", "value", 0));
            chartData.add(Map.of("name", "Apr", "value", 0));
            chartData.add(Map.of("name", "May", "value", 0));
        } else {
            for (Object[] row : revenueByMonthRaw) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", row[0]);
                map.put("value", row[1]);
                chartData.add(map);
            }
        }

        stats.put("chartData", chartData);

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
                emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), user.getVerificationCode());
            } else {
                emailService.sendRejectionEmail(user.getEmail(), user.getUsername());
            }

            return ResponseEntity.ok(Map.of(
                    "message", accept ? "Prestataire approuvé et activé." : "Demande refusée."));
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

    // ─────────────────────────────────────────────
    // SETTINGS
    // ─────────────────────────────────────────────
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        SystemSetting setting = settingRepo.findById(1L).orElseGet(() -> {
            SystemSetting defaultSetting = new SystemSetting();
            return settingRepo.save(defaultSetting);
        });
        return ResponseEntity.ok(setting);
    }

    @PostMapping("/settings")
    public ResponseEntity<?> saveSettings(@RequestBody SystemSetting newSettings) {
        SystemSetting setting = settingRepo.findById(1L).orElse(new SystemSetting());
        setting.setId(1L);
        setting.setPlatformName(newSettings.getPlatformName());
        setting.setSupportEmail(newSettings.getSupportEmail());
        if (newSettings.getPlatformFee() != null)
            setting.setPlatformFee(newSettings.getPlatformFee());
        if (newSettings.getEmailNotifications() != null)
            setting.setEmailNotifications(newSettings.getEmailNotifications());
        if (newSettings.getAutoApprove() != null)
            setting.setAutoApprove(newSettings.getAutoApprove());
        if (newSettings.getAvailableLanguages() != null)
            setting.setAvailableLanguages(newSettings.getAvailableLanguages());

        settingRepo.save(setting);
        return ResponseEntity.ok(Map.of("message", "Settings updated successfully"));
    }

    // ─────────────────────────────────────────────
    // SUPPORT TICKETS
    // ─────────────────────────────────────────────
    @GetMapping("/support")
    public ResponseEntity<?> getAllSupportTickets() {
        return ResponseEntity.ok(supportRepo.findAll());
    }

    @DeleteMapping("/support/{id}")
    public ResponseEntity<?> resolveSupportTicket(@PathVariable Long id) {
        SupportTicket ticket = supportRepo.findById(id).orElseThrow();
        ticket.setStatus("RESOLVED");
        supportRepo.save(ticket);
        return ResponseEntity.ok(Map.of("message", "Ticket resolved successfully"));
    }
}