package com.edusmart.server.service;

import com.edusmart.server.common.BusinessException;
import com.edusmart.server.dto.note.NoteDto;
import com.edusmart.server.dto.note.SaveNoteRequest;
import com.edusmart.server.dto.note.UploadFileRequest;
import com.edusmart.server.dto.note.UploadFileResponse;
import com.edusmart.server.entity.Note;
import com.edusmart.server.repository.NoteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 笔记 CRUD + 附件上传。
 */
@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final NoteRepository noteRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public NoteService(NoteRepository noteRepository,
                       FileStorageService fileStorageService,
                       ObjectMapper objectMapper) {
        this.noteRepository = noteRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public List<NoteDto> list(String userId, String subject) {
        List<Note> notes;
        if (subject != null && !subject.isBlank()) {
            notes = noteRepository.findByUserIdAndSubjectOrderByUpdatedAtDesc(userId, subject);
        } else {
            notes = noteRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }
        return notes.stream().map(this::toDto).toList();
    }

    public NoteDto get(String userId, String noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> BusinessException.notFound("笔记不存在"));
        return toDto(note);
    }

    @Transactional
    public NoteDto create(String userId, SaveNoteRequest req) {
        long now = System.currentTimeMillis();
        String id = (req.getId() != null && !req.getId().isBlank()) ? req.getId() : UUID.randomUUID().toString();
        Note note = Note.builder()
                .id(id)
                .userId(userId)
                .title(req.getTitle())
                .subject(req.getSubject())
                .content(req.getContent())
                .imagesJson(writeJson(req.getImages()))
                .audioPath(req.getAudioPath())
                .transcript(req.getTranscript())
                .knowledgePointsJson(writeJson(req.getKnowledgePoints()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        noteRepository.save(note);
        log.info("📝 创建笔记: user={}, id={}", userId, id);
        return toDto(note);
    }

    @Transactional
    public NoteDto update(String userId, String noteId, SaveNoteRequest req) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> BusinessException.notFound("笔记不存在"));
        if (req.getTitle() != null) note.setTitle(req.getTitle());
        if (req.getSubject() != null) note.setSubject(req.getSubject());
        if (req.getContent() != null) note.setContent(req.getContent());
        if (req.getImages() != null) note.setImagesJson(writeJson(req.getImages()));
        if (req.getAudioPath() != null) note.setAudioPath(req.getAudioPath());
        if (req.getTranscript() != null) note.setTranscript(req.getTranscript());
        if (req.getKnowledgePoints() != null) note.setKnowledgePointsJson(writeJson(req.getKnowledgePoints()));
        note.setUpdatedAt(System.currentTimeMillis());
        noteRepository.save(note);
        return toDto(note);
    }

    @Transactional
    public void delete(String userId, String noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> BusinessException.notFound("笔记不存在"));
        noteRepository.delete(note);
    }

    public UploadFileResponse uploadFile(String userId, UploadFileRequest req) {
        String category = detectCategory(req.getFileType(), req.getFileName());
        FileStorageService.StoredFile stored = fileStorageService.saveBase64(
                category, userId, req.getFileBase64(), req.getFileName());
        return UploadFileResponse.builder()
                .url(stored.url())
                .cloudPath(stored.cloudPath())
                .fileId(stored.cloudPath())
                .build();
    }

    private String detectCategory(String fileType, String fileName) {
        if (fileType != null) {
            return switch (fileType.toLowerCase(Locale.ROOT)) {
                case "image" -> "notes/images";
                case "audio" -> "notes/audios";
                default -> "notes/files";
            };
        }
        if (fileName == null) return "notes/files";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".webp") || lower.endsWith(".gif")) {
            return "notes/images";
        }
        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a")
                || lower.endsWith(".aac") || lower.endsWith(".amr")) {
            return "notes/audios";
        }
        return "notes/files";
    }

    private NoteDto toDto(Note note) {
        return NoteDto.builder()
                .id(note.getId())
                .userId(note.getUserId())
                .title(note.getTitle())
                .subject(note.getSubject())
                .content(note.getContent())
                .images(readList(note.getImagesJson()))
                .audioPath(note.getAudioPath())
                .transcript(note.getTranscript())
                .knowledgePoints(readList(note.getKnowledgePointsJson()))
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    private String writeJson(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new BusinessException("序列化字段失败: " + e.getMessage());
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            log.warn("反序列化字段失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
