package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Offre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceType;
    private Double prix;
    private Integer tempsArrivee;
    private String description;
    private String status = "ACTIVE";

    @Transient
    private Double distance;

    @ManyToOne
    @JoinColumn(name = "demande_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DemandeService demande;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private User provider;
}