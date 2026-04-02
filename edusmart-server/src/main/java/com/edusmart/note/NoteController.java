package com.edusmart.note;

import com.edusmart.note.dto.NoteRequest;
import com.edusmart.note.dto.NoteResponse;
import com.edusmart.security.SecurityUtils;
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
    public List<NoteResponse> list(@RequestParam(required = false) String subject) {
        return noteService.list(SecurityUtils.requireUser(), subject);
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable Long id) {
        return noteService.getById(SecurityUtils.requireUser(), id);
    }

    @PostMapping
    public NoteResponse create(@RequestBody NoteRequest body) {
        return noteService.create(SecurityUtils.requireUser(), body);
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable Long id, @RequestBody NoteRequest body) {
        return noteService.update(SecurityUtils.requireUser(), id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        noteService.delete(SecurityUtils.requireUser(), id);
    }
}
