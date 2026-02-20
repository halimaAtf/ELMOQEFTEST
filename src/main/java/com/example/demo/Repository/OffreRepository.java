package com.example.demo.Repository;

import com.example.demo.entity.Offre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OffreRepository extends JpaRepository<Offre, Long> {
    // Pour afficher les offres reçues par le client pour une demande spécifique
    List<Offre> findByDemande_Id(Long demandeId);

    // Pour que le prestataire voie ses propres offres
    List<Offre> findByProvider_Id(Long providerId);
}