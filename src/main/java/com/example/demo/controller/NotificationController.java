package com.example.demo.controller;

import com.example.demo.Repository.NotificationRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.Notification;
import com.example.demo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private UserRepository userRepo;

    @GetMapping
    public ResponseEntity<?> getUserNotifications(Authentication auth) {
        try {
            User user = userRepo.findByUsername(auth.getName()).orElseThrow();
            List<Notification> notifs = notificationRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
            return ResponseEntity.ok(notifs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            Notification notif = notificationRepo.findById(id).orElseThrow();
            notif.setRead(true);
            notificationRepo.save(notif);
            return ResponseEntity.ok(Map.of("message", "Marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
