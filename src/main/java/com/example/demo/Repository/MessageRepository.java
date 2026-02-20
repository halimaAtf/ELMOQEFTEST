package com.example.demo.Repository;

import com.example.demo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByDateEnvoiAsc(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId AND m.lu = false AND m.expediteur.id != :userId")
    List<Message> findNonLusByConversationAndUser(@Param("convId") Long conversationId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId AND m.lu = false AND m.expediteur.id != :userId")
    Long countNonLus(@Param("convId") Long conversationId, @Param("userId") Long userId);
}