package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
public class DemandeService {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titre;
    private String description;
    private Double budgetEstime;
    private String statut = "EN_ATTENTE"; // EN_ATTENTE, ACCEPTEE, TERMINEE

    @ManyToOne
    private User client;

    // Getters & Setters
}