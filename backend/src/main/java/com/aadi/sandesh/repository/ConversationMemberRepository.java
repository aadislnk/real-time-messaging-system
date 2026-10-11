package com.aadi.sandesh.repository;

import com.aadi.sandesh.api.dto.ConversationMemberResponse;
import com.aadi.sandesh.api.dto.MemberResponse;
import com.aadi.sandesh.domain.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("""
                SELECT new com.aadi.sandesh.api.dto.MemberResponse(
                    cm.userId, u.displayName, cm.role, cm.lastReadSequence
                    )
                    FROM ConversationMember cm
                        JOIN UserAccount u
                            on u.id = cm.userId
                                WHERE cm.conversationId = :conversationId
            """)
    List<MemberResponse> findMemberResponses(
            @Param("conversationId") UUID conversationId
    );


    @Query("""
                SELECT new com.aadi.sandesh.api.dto.ConversationMemberResponse(
                    cm.conversationId, cm.userId, u.displayName, cm.role, cm.lastReadSequence,
                                cm.hiddenAfterSequence
                    )
                    FROM ConversationMember cm
                        JOIN UserAccount u
                            on u.id = cm.userId
                                WHERE cm.conversationId IN :conversationIds
            """)
    List<ConversationMemberResponse> findConversationMemberResponses(
            @Param("conversationIds") Collection<UUID> conversationIds
    );

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

}
