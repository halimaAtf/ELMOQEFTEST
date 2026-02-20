package com.example.demo.service;

import com.example.demo.entity.Conversation;
import com.example.demo.entity.DemandeService;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.Repository.ConversationRepository;
import com.example.demo.Repository.MessageRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Repository.DemandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final DemandeRepository demandeRepository;

    // Crée une conversation ou récupère celle déjà existante
    @Transactional
    public Conversation creerOuRecuperer(Long clientId, Long prestataireId, Long demandeId) {
        return conversationRepository
                .findByClientIdAndPrestataireIdAndDemandeId(clientId, prestataireId, demandeId)
                .orElseGet(() -> {
                    User client = userRepository.findById(clientId).orElseThrow();
                    User prestataire = userRepository.findById(prestataireId).orElseThrow();
                    DemandeService demande = demandeRepository.findById(demandeId).orElseThrow();

                    Conversation conv = new Conversation();
                    conv.setClient(client);
                    conv.setPrestataire(prestataire);
                    conv.setDemande(demande);
                    return conversationRepository.save(conv);
                });
    }

    // Envoie un message dans une conversation
    @Transactional
    public Message envoyerMessage(Long conversationId, Long expediteurId, String contenu) {
        Conversation conv = conversationRepository.findById(conversationId).orElseThrow();
        User expediteur = userRepository.findById(expediteurId).orElseThrow();

        Message message = new Message();
        message.setConversation(conv);
        message.setExpediteur(expediteur);
        message.setContenu(contenu);

        conv.setDateDernierMessage(LocalDateTime.now());
        conversationRepository.save(conv);

        return messageRepository.save(message);
    }

    // Récupère tous les messages d'une conversation (triés par date)
    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByDateEnvoiAsc(conversationId);
    }

    // Récupère toutes les conversations d'un utilisateur
    public List<Conversation> getConversationsParUtilisateur(Long userId) {
        return conversationRepository.findAllByUserId(userId);
    }

    // Marque tous les messages non lus comme lus pour un utilisateur
    @Transactional
    public void marquerCommeLus(Long conversationId, Long userId) {
        List<Message> nonLus = messageRepository.findNonLusByConversationAndUser(conversationId, userId);
        nonLus.forEach(m -> m.setLu(true));
        messageRepository.saveAll(nonLus);
    }

    // Archive une conversation
    @Transactional
    public void archiver(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId).orElseThrow();
        conv.setStatut("ARCHIVEE");
        conversationRepository.save(conv);
    }

    // Compte les messages non lus pour un utilisateur
    public Long countNonLus(Long conversationId, Long userId) {
        return messageRepository.countNonLus(conversationId, userId);
    }
}