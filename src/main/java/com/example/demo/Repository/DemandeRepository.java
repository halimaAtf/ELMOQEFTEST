package com.example.demo.Repository;

import com.example.demo.entity.DemandeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeRepository extends JpaRepository<DemandeService, Long> {
    long countByStatus(String status);

    // Demandes d'un client
    List<DemandeService> findByClient_Id(Long clientId);

    // Demandes reçues par un provider/assignées
    List<DemandeService> findByProvider_Id(Long providerId);

    // Demandes en attente par statut
    List<DemandeService> findByStatus(String status);
}