package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Offre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceType;
    private Double prix;
    private Integer tempsArrivee;
    private String description;
    private String status = "ACTIVE";

    @ManyToOne
    @JoinColumn(name = "demande_id")
    @JsonIgnore
    private DemandeService demande;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    @JsonIgnore
    private User provider;
}