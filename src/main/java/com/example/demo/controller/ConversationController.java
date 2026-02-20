package com.example.demo.controller;

import com.example.demo.entity.Conversation;
import com.example.demo.entity.Message;
import com.example.demo.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    // ─────────────────────────────────────────────
    //  REST ENDPOINTS
    // ─────────────────────────────────────────────

    // POST /api/conversations
    // Body: { "clientId": 1, "prestataireId": 2, "demandeId": 3 }
    @PostMapping
    public ResponseEntity<?> creerOuRecuperer(@RequestBody Map<String, Long> body) {
        Conversation conv = conversationService.creerOuRecuperer(
                body.get("clientId"),
                body.get("prestataireId"),
                body.get("demandeId")
        );
        return ResponseEntity.ok(conv);
    }

    // GET /api/conversations/utilisateur/{userId}
    @GetMapping("/utilisateur/{userId}")
    public ResponseEntity<List<Conversation>> getConversations(@PathVariable Long userId) {
        return ResponseEntity.ok(conversationService.getConversationsParUtilisateur(userId));
    }

    // GET /api/conversations/{id}/messages
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.getMessages(id));
    }

    // PATCH /api/conversations/{id}/archiver
    @PatchMapping("/{id}/archiver")
    public ResponseEntity<?> archiver(@PathVariable Long id) {
        conversationService.archiver(id);
        return ResponseEntity.ok(Map.of("message", "Conversation archivée"));
    }

    // POST /api/conversations/{id}/lus/{userId}
    @PostMapping("/{id}/lus/{userId}")
    public ResponseEntity<?> marquerLus(@PathVariable Long id, @PathVariable Long userId) {
        conversationService.marquerCommeLus(id, userId);
        return ResponseEntity.ok(Map.of("message", "Messages marqués comme lus"));
    }

    // GET /api/conversations/{id}/non-lus/{userId}
    @GetMapping("/{id}/non-lus/{userId}")
    public ResponseEntity<Long> countNonLus(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(conversationService.countNonLus(id, userId));
    }

    // ─────────────────────────────────────────────
    //  WEBSOCKET
    //  Client envoie vers  : /app/chat/{conversationId}
    //  Serveur broadcast   : /topic/conversation/{conversationId}
    // ─────────────────────────────────────────────

    @MessageMapping("/chat/{conversationId}")
    public void envoyerMessageWs(
            @DestinationVariable Long conversationId,
            @Payload WsMessage payload) {

        Message saved = conversationService.envoyerMessage(
                conversationId,
                payload.getExpediteurId(),
                payload.getContenu()
        );

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                saved
        );
    }

    // DTO WebSocket entrant
    public static class WsMessage {
        private Long expediteurId;
        private String contenu;

        public Long getExpediteurId() { return expediteurId; }
        public void setExpediteurId(Long v) { this.expediteurId = v; }
        public String getContenu() { return contenu; }
        public void setContenu(String v) { this.contenu = v; }
    }
}