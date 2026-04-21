package com.edusmart.server.repository;

import com.edusmart.server.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, String> {

    List<Note> findByUserIdOrderByUpdatedAtDesc(String userId);

    List<Note> findByUserIdAndSubjectOrderByUpdatedAtDesc(String userId, String subject);

    Optional<Note> findByIdAndUserId(String id, String userId);
}
