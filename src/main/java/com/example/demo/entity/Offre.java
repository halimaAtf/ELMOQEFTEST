package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
public class Offre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double prixPropose;
    private String message;
    private String statutOffre = "PENDING"; // PENDING, ACCEPTED, REJECTED

    @ManyToOne
    private DemandeService demande;

    @ManyToOne
    private User prestataire;

    // Getters & Setters
}