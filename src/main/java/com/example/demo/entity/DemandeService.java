package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class DemandeService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceType;
    private String description;
    private String adresse;
    private Double latitude;
    private Double longitude;
    private String status = "EN_ATTENTE";

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client; // Corrigé

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private User provider; // Corrigé

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Offre> offres;
}