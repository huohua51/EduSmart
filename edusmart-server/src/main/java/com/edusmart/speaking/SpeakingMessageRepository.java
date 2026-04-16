package com.edusmart.speaking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpeakingMessageRepository extends JpaRepository<SpeakingMessage, Long> {

    List<SpeakingMessage> findBySession_IdOrderByCreatedAtAsc(Long sessionId);
}
