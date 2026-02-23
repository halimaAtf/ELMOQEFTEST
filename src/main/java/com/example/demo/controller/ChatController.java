package com.example.demo.controller;

import com.example.demo.Repository.ChatMessageRepository;
import com.example.demo.Repository.DemandeRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.DemandeService;
import com.example.demo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private DemandeRepository demandeRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{demandeId}")
    public ResponseEntity<?> getMessages(@PathVariable Long demandeId, Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DemandeService demande = demandeRepository.findById(demandeId).orElse(null);
            if (demande == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Demande not found"));
            }

            // Verify if user is part of the request
            boolean isClient = demande.getClient() != null && demande.getClient().getId().equals(currentUser.getId());
            boolean isProvider = demande.getProvider() != null
                    && demande.getProvider().getId().equals(currentUser.getId());

            if (!isClient && !isProvider) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Unauthorized to view messages for this request"));
            }

            // Return messages
            List<ChatMessage> messages = chatMessageRepository.findByDemandeIdOrderByTimestampAsc(demandeId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server Error"));
        }
    }

    @PostMapping("/{demandeId}")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long demandeId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DemandeService demande = demandeRepository.findById(demandeId).orElse(null);
            if (demande == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Demande not found"));
            }

            // Check if Demande is confirmed or active (e.g. "EN_COURS", "TERMINE")
            if (demande.getStatus() == null || demande.getStatus().equals("EN_ATTENTE")) {
                return ResponseEntity.badRequest().body(Map.of("error", "You can only chat on accepted requests"));
            }

            boolean isClient = demande.getClient() != null && demande.getClient().getId().equals(currentUser.getId());
            boolean isProvider = demande.getProvider() != null
                    && demande.getProvider().getId().equals(currentUser.getId());

            if (!isClient && !isProvider) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Unauthorized to send messages for this request"));
            }

            String content = payload.get("content").toString();
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message content cannot be empty"));
            }

            User receiver = isClient ? demande.getProvider() : demande.getClient();

            ChatMessage msg = new ChatMessage();
            msg.setDemande(demande);
            msg.setSender(currentUser);
            msg.setReceiver(receiver);
            msg.setContent(content);
            msg.setTimestamp(LocalDateTime.now());

            chatMessageRepository.save(msg);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server Error"));
        }
    }
}
