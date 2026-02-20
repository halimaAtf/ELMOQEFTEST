package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ElementCollection
    @CollectionTable(name = "demande_photos", joinColumns = @JoinColumn(name = "demande_id"))
    @Column(name = "photo_base64_data", columnDefinition = "LONGTEXT")
    private List<String> photos;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonIgnore
    private User client;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    @JsonIgnore
    private User provider;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Offre> offres;
}