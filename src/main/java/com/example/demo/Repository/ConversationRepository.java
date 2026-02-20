package com.example.demo.Repository;

import com.example.demo.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByClientIdOrderByDateDernierMessageDesc(Long clientId);

    List<Conversation> findByPrestataireIdOrderByDateDernierMessageDesc(Long prestataireId);

    @Query("SELECT c FROM Conversation c WHERE c.client.id = :userId OR c.prestataire.id = :userId ORDER BY c.dateDernierMessage DESC")
    List<Conversation> findAllByUserId(@Param("userId") Long userId);

    Optional<Conversation> findByClientIdAndPrestataireIdAndDemandeId(Long clientId, Long prestataireId, Long demandeId);
}