package com.edusmart.note;

import com.edusmart.note.dto.NoteRequest;
import com.edusmart.note.dto.NoteResponse;
import com.edusmart.security.AuthenticatedUser;
import com.edusmart.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public List<NoteResponse> list(AuthenticatedUser user, String subject) {
        List<Note> notes;
        if (subject != null && !subject.isBlank()) {
            notes = noteRepository.findByUserIdAndSubjectOrderByUpdatedAtDesc(user.getUserId(), subject.trim());
        } else {
            notes = noteRepository.findByUserIdOrderByUpdatedAtDesc(user.getUserId());
        }
        return notes.stream().map(this::toResponse).toList();
    }

    public NoteResponse getById(AuthenticatedUser user, Long id) {
        Note note = noteRepository.findByIdAndUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在"));
        return toResponse(note);
    }

    public NoteResponse create(AuthenticatedUser user, NoteRequest req) {
        var owner = userRepository.getReferenceById(user.getUserId());
        Note note = new Note();
        note.setUser(owner);
        applyRequest(note, req);
        note.setCreatedAt(Instant.now());
        note.setUpdatedAt(Instant.now());
        note = noteRepository.save(note);
        return toResponse(note);
    }

    public NoteResponse update(AuthenticatedUser user, Long id, NoteRequest req) {
        Note note = noteRepository.findByIdAndUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在"));
        applyRequest(note, req);
        note.setUpdatedAt(Instant.now());
        note = noteRepository.save(note);
        return toResponse(note);
    }

    public void delete(AuthenticatedUser user, Long id) {
        Note note = noteRepository.findByIdAndUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在"));
        noteRepository.delete(note);
    }

    private void applyRequest(Note note, NoteRequest req) {
        note.setTitle(req.getTitle());
        note.setSubject(req.getSubject());
        note.setContent(req.getContent());
        note.setTranscript(req.getTranscript());
        note.setAudioUrl(req.getAudioUrl());
        try {
            note.setKnowledgePointsJson(
                    req.getKnowledgePoints() == null || req.getKnowledgePoints().isEmpty()
                            ? null
                            : objectMapper.writeValueAsString(req.getKnowledgePoints())
            );
            note.setImageUrlsJson(
                    req.getImages() == null || req.getImages().isEmpty()
                            ? null
                            : objectMapper.writeValueAsString(req.getImages())
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "序列化 JSON 失败");
        }
    }

    private NoteResponse toResponse(Note n) {
        NoteResponse r = new NoteResponse();
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setSubject(n.getSubject());
        r.setContent(n.getContent());
        r.setTranscript(n.getTranscript());
        r.setAudioUrl(n.getAudioUrl());
        r.setCreatedAt(n.getCreatedAt());
        r.setUpdatedAt(n.getUpdatedAt());
        r.setKnowledgePoints(readList(n.getKnowledgePointsJson()));
        r.setImages(readList(n.getImageUrlsJson()));
        return r;
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
