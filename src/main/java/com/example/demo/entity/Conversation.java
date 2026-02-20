package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateDernierMessage;
    private String statut = "ACTIVE"; // ACTIVE, ARCHIVEE, FERMEE

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonIgnore
    private User client;

    @ManyToOne
    @JoinColumn(name = "prestataire_id")
    @JsonIgnore
    private User prestataire;

    @ManyToOne
    @JoinColumn(name = "demande_id")
    @JsonIgnore
    private DemandeService demande;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Message> messages;
}