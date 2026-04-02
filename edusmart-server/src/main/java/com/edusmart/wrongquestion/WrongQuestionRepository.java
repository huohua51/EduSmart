package com.edusmart.wrongquestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {

    List<WrongQuestion> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WrongQuestion> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT wq FROM WrongQuestion wq WHERE wq.user.id = :userId AND (wq.nextReviewTime IS NULL OR wq.nextReviewTime <= :currentTime) ORDER BY wq.createdAt DESC")
    List<WrongQuestion> findDueForReview(Long userId, Long currentTime);

    void deleteByIdAndUserId(Long id, Long userId);
}
