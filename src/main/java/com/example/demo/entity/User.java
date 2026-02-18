package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;
    private String email;
    private String role; // ADMIN, PROVIDER, CLIENT
    private String phone;
    private String status;

    @OneToMany(mappedBy = "client")
    @JsonIgnore
    private List<DemandeService> demandes;

    @OneToMany(mappedBy = "provider")
    @JsonIgnore
    private List<Offre> offres;
}