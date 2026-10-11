package com.aadi.sandesh.repository;

import com.aadi.sandesh.domain.Conversation;
import com.aadi.sandesh.domain.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {


    @Query("""
        SELECT c FROM Conversation c
        JOIN ConversationMember cm ON c.id = cm.conversationId
        WHERE c.type = :type
        AND cm.userId IN :memberIds
        GROUP BY c.id
        HAVING COUNT(DISTINCT cm.userId) = 2
        AND COUNT(cm.userId) = 2
        """)
    Optional<Conversation> findExistingDirectConversation(
            @Param("type") ConversationType type,
            @Param("memberIds") Set<UUID> memberIds
    );

    @Query("""
            SELECT c from Conversation c JOIN ConversationMember cm
                    on cm.conversationId = c.id
                                where cm.userId = :userId
                                            ORDER BY cm.joinedAt DESC
            """)
    List<Conversation> findAllForUser(@Param("userId") UUID userId);
}
