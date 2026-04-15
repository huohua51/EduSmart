package com.edusmart.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUser_IdOrderByUpdatedAtDesc(Long userId);

    List<Note> findByUser_IdAndSubjectOrderByUpdatedAtDesc(Long userId, String subject);

    Optional<Note> findByIdAndUser_Id(Long id, Long userId);
}
