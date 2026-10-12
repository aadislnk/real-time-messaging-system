package com.aadi.sandesh.domain;

import com.aadi.sandesh.domain.enums.ConversationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private ConversationType type;

    @Column(length = 120)
    private String title;

    @Column(name = "created_by",nullable = false)
    private UUID createdBy;

    @Column(name = "next_message_sequence",nullable = false)
    private long nextMessageSequence;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at",nullable = false)
    private Instant updatedAt;

    public Conversation(ConversationType type,String title, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.title = title == null || title.isBlank() ? null : title.strip();
        this.createdBy = createdBy;
        this.nextMessageSequence = 1L;
    }

    @PrePersist
    void onCreate(){
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate(){
        this.updatedAt = Instant.now();
    }

    public long allocateNextMessageSequence(){
        return nextMessageSequence++;
    }

    public void rename(String newTitle){
        if(newTitle == null || newTitle.isBlank()){
            throw new IllegalArgumentException("Conversation title cannot be null or blank");
        }
        this.title = newTitle.strip();
    }
}
