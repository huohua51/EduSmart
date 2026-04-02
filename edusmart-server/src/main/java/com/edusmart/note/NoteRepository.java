package com.edusmart.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Note> findByUserIdAndSubjectOrderByUpdatedAtDesc(Long userId, String subject);

    Optional<Note> findByIdAndUserId(Long id, Long userId);
}
