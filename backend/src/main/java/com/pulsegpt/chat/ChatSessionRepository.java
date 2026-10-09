package com.pulsegpt.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Page<ChatSession> findByUserId(UUID userId, Pageable pageable);

    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);
}
