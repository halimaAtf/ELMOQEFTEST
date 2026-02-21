package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DemandeService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceType;
    private String description;
    private String adresse;
    private String status = "EN_ATTENTE";

    @ElementCollection
    @CollectionTable(name = "demande_photos", joinColumns = @JoinColumn(name = "demande_id"))
    @Column(name = "photo_base64_data", columnDefinition = "LONGTEXT")
    private List<String> photos;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private User provider; 

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Offre> offres;
}