package com.example.demo.Repository;

import com.example.demo.entity.DemandeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeRepository extends JpaRepository<DemandeService, Long> {

    // Demandes d'un client
    List<DemandeService> findByClientId(Long clientId);

    // Demandes reçues par un provider
    List<DemandeService> findByProviderId(Long providerId);

    // Demandes en attente par type de service
    List<DemandeService> findByServiceTypeAndStatus(String serviceType, String status);
}