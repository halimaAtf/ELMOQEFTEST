package com.example.demo.Repository;

import com.example.demo.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByDemandeIdOrderByTimestampAsc(Long demandeId);

    @Query("SELECT c FROM ChatMessage c WHERE (c.sender.id = :user1 AND c.receiver.id = :user2) OR (c.sender.id = :user2 AND c.receiver.id = :user1) ORDER BY c.timestamp ASC")
    List<ChatMessage> findChatHistoryBetweenUsers(@Param("user1") Long user1, @Param("user2") Long user2);
}
