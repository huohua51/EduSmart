package com.edusmart.speaking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

    List<SpeakingSession> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<SpeakingSession> findByIdAndUser_Id(Long id, Long userId);

    long countByUser_Id(Long userId);
}
