package com.edusmart.server.controller;

import com.edusmart.server.common.ApiResponse;
import com.edusmart.server.dto.note.NoteDto;
import com.edusmart.server.dto.note.SaveNoteRequest;
import com.edusmart.server.dto.note.UploadFileRequest;
import com.edusmart.server.dto.note.UploadFileResponse;
import com.edusmart.server.security.TokenAuthInterceptor;
import com.edusmart.server.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ApiResponse<List<NoteDto>> list(@RequestParam(value = "subject", required = false) String subject) {
        return ApiResponse.success(noteService.list(TokenAuthInterceptor.requireUserId(), subject));
    }

    @GetMapping("/{noteId}")
    public ApiResponse<NoteDto> get(@PathVariable String noteId) {
        return ApiResponse.success(noteService.get(TokenAuthInterceptor.requireUserId(), noteId));
    }

    @PostMapping
    public ApiResponse<NoteDto> create(@Valid @RequestBody SaveNoteRequest request) {
        return ApiResponse.success("笔记创建成功",
                noteService.create(TokenAuthInterceptor.requireUserId(), request));
    }

    @PutMapping("/{noteId}")
    public ApiResponse<NoteDto> update(@PathVariable String noteId, @RequestBody SaveNoteRequest request) {
        return ApiResponse.success("笔记更新成功",
                noteService.update(TokenAuthInterceptor.requireUserId(), noteId, request));
    }

    @DeleteMapping("/{noteId}")
    public ApiResponse<Void> delete(@PathVariable String noteId) {
        noteService.delete(TokenAuthInterceptor.requireUserId(), noteId);
        return ApiResponse.success("笔记删除成功", null);
    }

    @PostMapping("/files")
    public ApiResponse<UploadFileResponse> upload(@Valid @RequestBody UploadFileRequest request) {
        return ApiResponse.success("上传成功",
                noteService.uploadFile(TokenAuthInterceptor.requireUserId(), request));
    }
}
