package com.edusmart.wrongquestion;

import com.edusmart.security.SecurityUtils;
import com.edusmart.wrongquestion.dto.WrongQuestionRequest;
import com.edusmart.wrongquestion.dto.WrongQuestionResponse;
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
@RequestMapping("/api/wrong-questions")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    @GetMapping
    public List<WrongQuestionResponse> list(@RequestParam(required = false) Boolean dueForReview) {
        var user = SecurityUtils.requireUser();
        if (Boolean.TRUE.equals(dueForReview)) {
            return wrongQuestionService.listDueForReview(user);
        }
        return wrongQuestionService.list(user);
    }

    @GetMapping("/{id}")
    public WrongQuestionResponse get(@PathVariable Long id) {
        return wrongQuestionService.getById(SecurityUtils.requireUser(), id);
    }

    @PostMapping
    public WrongQuestionResponse create(@RequestBody WrongQuestionRequest body) {
        return wrongQuestionService.create(SecurityUtils.requireUser(), body);
    }

    @PutMapping("/{id}")
    public WrongQuestionResponse update(@PathVariable Long id, @RequestBody WrongQuestionRequest body) {
        return wrongQuestionService.update(SecurityUtils.requireUser(), id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        wrongQuestionService.delete(SecurityUtils.requireUser(), id);
    }
}

