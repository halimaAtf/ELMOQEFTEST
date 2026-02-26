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
import com.example.demo.entity.Notification;
import com.example.demo.Repository.NotificationRepository;

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
    @Autowired
    private NotificationRepository notificationRepository;

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

            // Check if Demande is confirmed or active (e.g. "EN_COURS")
            if (demande.getStatus() == null || demande.getStatus().equals("EN_ATTENTE")) {
                return ResponseEntity.badRequest().body(Map.of("error", "You can only chat on accepted requests"));
            }
            if (demande.getStatus().equals("TERMINE") || demande.getStatus().equals("TERMINEE") || demande.getStatus().equals("ANNULE") || demande.getStatus().equals("ANNULEEE")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot send messages for closed requests"));
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

            // Add a Notification for the receiver
            Notification notif = new Notification();
            notif.setUser(receiver);
            notif.setMessage("Nouveau message de " + currentUser.getUsername() + " pour la demande " + demande.getServiceType());
            notificationRepository.save(notif);

            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server Error"));
        }
    }
    @GetMapping("/user/{partnerId}")
    public ResponseEntity<?> getMessagesByUser(@PathVariable Long partnerId, Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            List<ChatMessage> messages = chatMessageRepository.findChatHistoryBetweenUsers(currentUser.getId(), partnerId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server Error"));
        }
    }

    @PostMapping("/user/{partnerId}")
    public ResponseEntity<?> sendMessageToUser(
            @PathVariable Long partnerId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            User receiver = userRepository.findById(partnerId).orElseThrow(() -> new RuntimeException("Receiver not found"));

            boolean hasActive = false;
            DemandeService activeDemande = null;

            List<DemandeService> sharedClient = demandeRepository.findByClient_Id(currentUser.getId());
            if (sharedClient != null) {
                for (DemandeService d : sharedClient) {
                    if (d.getProvider() != null && d.getProvider().getId().equals(partnerId) && "EN_COURS".equals(d.getStatus())) {
                        hasActive = true;
                        activeDemande = d;
                        break;
                    }
                }
            }

            if (!hasActive) {
                List<DemandeService> sharedProvider = demandeRepository.findByProvider_Id(currentUser.getId());
                if (sharedProvider != null) {
                    for (DemandeService d : sharedProvider) {
                        if (d.getClient() != null && d.getClient().getId().equals(partnerId) && "EN_COURS".equals(d.getStatus())) {
                            hasActive = true;
                            activeDemande = d;
                            break;
                        }
                    }
                }
            }

            if (!hasActive) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot send messages. No active requests (EN_COURS) exist between you."));
            }

            String content = payload.get("content").toString();
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message content cannot be empty"));
            }

            ChatMessage msg = new ChatMessage();
            msg.setDemande(activeDemande);
            msg.setSender(currentUser);
            msg.setReceiver(receiver);
            msg.setContent(content);
            msg.setTimestamp(LocalDateTime.now());

            chatMessageRepository.save(msg);

            Notification notif = new Notification();
            notif.setUser(receiver);
            notif.setMessage("Nouveau message de " + currentUser.getUsername());
            notificationRepository.save(notif);

            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server Error"));
        }
    }
}
